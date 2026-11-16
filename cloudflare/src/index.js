/**
 * Safar's Cloudflare Worker: the parts of the backend Firebase's free plan can't run.
 *
 *   PUT    /avatar               upload the caller's profile photo (JPEG body)
 *   DELETE /avatar               remove it (account deletion)
 *   GET    /avatars/{uid}.jpg    serve a profile photo
 *   POST   /notify               push an in-app notification or chat message the caller just wrote
 *   GET    /route?from=&to=      driving route between two points (OpenStreetMap data via OSRM)
 *   cron (hourly)                close expired rides, and post weekly repeats a week ahead
 *
 * Callers prove who they are with their Firebase ID token. The Worker reaches Firebase with a
 * service account (secret FIREBASE_SERVICE_ACCOUNT), which bypasses database rules, so every
 * route checks ownership itself.
 */
import { verifyIdToken, googleAccessToken } from "./google.js";

const DAY = 24 * 60 * 60 * 1000;
const EXPIRY_WINDOW = 3 * DAY; // matches Ride.EXPIRY_WINDOW_MS in the app
const KEEP_BOOKED_RIDES = 14 * DAY; // time riders get to mark a booked ride complete and review it
const RIDE_LIFETIME = 60 * 60 * 1000; // matches Ride.RIDE_LIFETIME_MS: a ride closes an hour after it leaves
const SERIES_HORIZON_DAYS = 7; // matches RideSeries.HORIZON_DAYS
const SERIES_MIN_LEAD = 30 * 60 * 1000; // matches RideSeries.MIN_LEAD_MS
const ROUTE_CACHE_SECONDS = 30 * 24 * 60 * 60;
const PUSH_WINDOW = 10 * 60 * 1000; // only push things written in the last 10 minutes
const MAX_AVATAR_BYTES = 1024 * 1024;
const PUSH_ID = /^[A-Za-z0-9_-]{1,128}$/;

export default {
  async fetch(request, env, ctx) {
    try {
      return await route(request, env, ctx);
    } catch (err) {
      if (err instanceof HttpError) {
        if (err.status !== 404) console.warn(`${request.method} ${new URL(request.url).pathname} -> ${err.status} ${err.message}`);
        return json({ error: err.message }, err.status);
      }
      console.error(err.stack || String(err));
      return json({ error: "Something went wrong" }, 500);
    }
  },

  async scheduled(_event, env, ctx) {
    ctx.waitUntil((async () => {
      console.log("extendSeries", JSON.stringify(await extendSeries(env)));
      console.log("purgeExpiredRides", JSON.stringify(await purgeExpiredRides(env)));
    })());
  },
};

async function route(request, env, ctx) {
  const url = new URL(request.url);
  const path = url.pathname;

  const avatar = path.match(/^\/avatars\/([A-Za-z0-9]{1,128})\.jpg$/);
  if (avatar && (request.method === "GET" || request.method === "HEAD")) return serveAvatar(request, env, ctx, avatar[1]);

  if (path === "/avatar" && request.method === "PUT") return uploadAvatar(request, env, url, await caller(request, env));
  if (path === "/avatar" && request.method === "DELETE") return deleteAvatar(env, await caller(request, env));
  if (path === "/notify" && request.method === "POST") return notify(request, env, await caller(request, env));
  if (path === "/route" && request.method === "GET") {
    await caller(request, env);
    return route_(request, url, ctx);
  }
  if (path === "/" && request.method === "GET") return json({ ok: true, service: "safar-api" });

  throw new HttpError(404, "Not found");
}

// ─── Photos ─────────────────────────────────────────────────────────────────

/** R2 when a bucket is bound, otherwise KV; both store the same key. */
function photoStore(env) {
  if (env.PHOTOS) {
    return {
      put: (key, bytes) => env.PHOTOS.put(key, bytes, { httpMetadata: { contentType: "image/jpeg" } }),
      get: async (key) => (await env.PHOTOS.get(key))?.arrayBuffer() ?? null,
      delete: (key) => env.PHOTOS.delete(key),
    };
  }
  if (env.PHOTOS_KV) {
    return {
      put: (key, bytes) => env.PHOTOS_KV.put(key, bytes),
      get: (key) => env.PHOTOS_KV.get(key, "arrayBuffer"),
      delete: (key) => env.PHOTOS_KV.delete(key),
    };
  }
  throw new HttpError(503, "Photo storage isn't configured");
}

async function uploadAvatar(request, env, url, uid) {
  const declared = Number(request.headers.get("content-length") || 0);
  if (declared > MAX_AVATAR_BYTES) throw new HttpError(413, "That photo is too large");
  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_AVATAR_BYTES) throw new HttpError(413, "That photo is too large");
  // JPEG files start with FF D8 FF; the app always re-encodes to JPEG before uploading.
  if (bytes[0] !== 0xff || bytes[1] !== 0xd8 || bytes[2] !== 0xff) throw new HttpError(415, "Photos must be JPEG");

  await photoStore(env).put(`avatars/${uid}.jpg`, bytes);
  // The version makes every upload a new URL, so caches never show the old photo.
  const photoUrl = `${url.origin}/avatars/${uid}.jpg?v=${Date.now()}`;
  await db(env, "PATCH", `users/${uid}`, { photoUrl });
  return json({ photoUrl });
}

async function deleteAvatar(env, uid) {
  await photoStore(env).delete(`avatars/${uid}.jpg`);
  return json({ ok: true });
}

async function serveAvatar(request, env, ctx, uid) {
  const cache = caches.default;
  const cached = await cache.match(request);
  if (cached) return cached;
  const bytes = await photoStore(env).get(`avatars/${uid}.jpg`);
  if (!bytes) return new Response("Not found", { status: 404, headers: { "cache-control": "public, max-age=60" } });
  const response = new Response(bytes, {
    headers: { "content-type": "image/jpeg", "cache-control": "public, max-age=31536000, immutable" },
  });
  ctx.waitUntil(cache.put(request, response.clone()));
  return response;
}

// ─── Push notifications ─────────────────────────────────────────────────────

/**
 * Body: {"kind":"notification","to":uid,"id":nid} or {"kind":"message","cid":cid,"id":mid}.
 * The caller must be the author of the item, it must be fresh, and each item is pushed once.
 */
async function notify(request, env, uid) {
  const body = await request.json().catch(() => ({}));
  if (!PUSH_ID.test(body.id || "")) throw new HttpError(400, "Bad id");

  let to, path, push, device;
  if (body.kind === "notification") {
    if (!PUSH_ID.test(body.to || "")) throw new HttpError(400, "Bad recipient");
    to = body.to;
    path = `notifications/${to}/${body.id}`;
    const [n, token] = await Promise.all([db(env, "GET", path), db(env, "GET", `pushTokens/${to}`)]);
    device = token;
    if (!n || n.fromUserId !== uid) throw new HttpError(403, "Not your notification");
    if (n.pushedAt) return json({ ok: true, skipped: "already pushed" });
    if (Math.abs(Date.now() - (n.timestamp || 0)) > PUSH_WINDOW) throw new HttpError(409, "Too old to push");
    push = {
      title: n.title || "Safar",
      body: n.message || "",
      data: { type: String(n.type || ""), adId: String(n.relatedAdId || ""), open: "notifications" },
    };
  } else if (body.kind === "message") {
    const cid = String(body.cid || "");
    const parts = cid.split("_");
    if (parts.length !== 2 || !parts.every((p) => PUSH_ID.test(p)) || !parts.includes(uid)) {
      throw new HttpError(403, "Not your conversation");
    }
    to = parts[0] === uid ? parts[1] : parts[0];
    path = `messages/${cid}/${body.id}`;
    const [m, token] = await Promise.all([db(env, "GET", path), db(env, "GET", `pushTokens/${to}`)]);
    device = token;
    if (!m || m.senderId !== uid) throw new HttpError(403, "Not your message");
    if (m.pushedAt) return json({ ok: true, skipped: "already pushed" });
    if (Math.abs(Date.now() - (m.timestamp || 0)) > PUSH_WINDOW) throw new HttpError(409, "Too old to push");
    const text = String(m.text || "");
    push = {
      title: m.senderName || "New message",
      body: text.length > 200 ? `${text.slice(0, 199)}…` : text,
      data: { type: "MESSAGE", open: `chat:${uid}` },
    };
  } else {
    throw new HttpError(400, "Unknown kind");
  }

  // Mark the item pushed (so a retry can't push it twice) while the push goes out.
  const claim = db(env, "PATCH", path, { pushedAt: Date.now() });
  if (typeof device !== "string" || !device) {
    await claim;
    return json({ ok: true, skipped: "no device" });
  }
  const [, sent] = await Promise.all([claim, sendPush(env, device, push)]);
  if (sent === "unregistered") await db(env, "DELETE", `pushTokens/${to}`);
  return json({ ok: true, sent: sent === "sent" });
}

async function sendPush(env, token, { title, body, data }) {
  const sa = serviceAccount(env);
  const access = await googleAccessToken(sa, env);
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${access}`, "content-type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        // Data-only, so the app's messaging service builds the notification and its tap target.
        data: { ...data, title, body },
        android: { priority: "HIGH" },
      },
    }),
  });
  if (res.ok) return "sent";
  const text = await res.text();
  if (res.status === 404 || text.includes("UNREGISTERED")) return "unregistered";
  console.error("FCM send failed", res.status, text);
  return "failed";
}

// ─── Driving routes ─────────────────────────────────────────────────────────

const POINT = /^-?\d{1,3}\.\d+,-?\d{1,3}\.\d+$/;

/**
 * Road route between two points, from the public OSRM server (OpenStreetMap data). Points are
 * rounded to ~10 m so nearby requests share a cache entry; answers are cached at the edge for a
 * month, which keeps traffic to OSRM tiny. Returns {polyline, meters, seconds}; polyline is
 * Google's encoded format (precision 5).
 */
async function route_(request, url, ctx) {
  const round = (p) => p.split(",").map((n) => Number(n).toFixed(4)).join(",");
  const from = url.searchParams.get("from") || "";
  const to = url.searchParams.get("to") || "";
  if (!POINT.test(from) || !POINT.test(to)) throw new HttpError(400, "Bad points");
  const [a, b] = [round(from), round(to)];

  const cacheKey = new Request(`https://route.cache/${a}/${b}`);
  const cached = await caches.default.match(cacheKey);
  if (cached) return cached;

  const lngLat = (p) => p.split(",").reverse().join(",");
  const res = await fetch(
    `https://router.project-osrm.org/route/v1/driving/${lngLat(a)};${lngLat(b)}?overview=full&geometries=polyline&alternatives=false&steps=false`,
    { headers: { "user-agent": "safar-api (Cloudflare Worker)" } },
  );
  if (!res.ok) throw new HttpError(502, "Route service unavailable");
  const data = await res.json();
  const r = data.routes?.[0];
  if (data.code !== "Ok" || !r) throw new HttpError(404, "No driving route");

  const response = json({ polyline: r.geometry, meters: Math.round(r.distance), seconds: Math.round(r.duration) });
  response.headers.set("cache-control", `public, max-age=${ROUTE_CACHE_SECONDS}`);
  ctx.waitUntil(caches.default.put(cacheKey, response.clone()));
  return response;
}

// ─── Weekly repeats ─────────────────────────────────────────────────────────

/** "20261117" for a departure in the series' time zone, as RideSeries.dayKey does in the app. */
export function dayKey(departure, utcOffsetMinutes) {
  const d = new Date(departure + utcOffsetMinutes * 60000);
  return `${d.getUTCFullYear()}${String(d.getUTCMonth() + 1).padStart(2, "0")}${String(d.getUTCDate()).padStart(2, "0")}`;
}

/** Departures in the next week on the series' weekdays (ISO, Monday = 1), as RideSeries.upcoming. */
export function upcoming(days, hour, minute, utcOffsetMinutes, now) {
  const out = [];
  const local = new Date(now + utcOffsetMinutes * 60000);
  for (let i = 0; i <= SERIES_HORIZON_DAYS; i++) {
    const day = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate() + i, hour, minute);
    const iso = ((new Date(day).getUTCDay() + 6) % 7) + 1;
    const departure = day - utcOffsetMinutes * 60000;
    if (days.includes(iso) && departure >= now + SERIES_MIN_LEAD && departure <= now + SERIES_HORIZON_DAYS * DAY) out.push(departure);
  }
  return out;
}

/**
 * Keeps every active weekly repeat posted a week ahead. A day is created once (recorded under
 * `made`), so a ride the driver deletes or cancels for one day doesn't come back.
 */
export async function extendSeries(env, now = Date.now()) {
  const all = (await db(env, "GET", "rideSeries")) || {};
  const updates = {};
  let created = 0;
  const stale = now - 30 * DAY;

  for (const [id, s] of Object.entries(all)) {
    if (!s || s.active === false || !s.template) continue;
    const days = String(s.days || "").split(",").map(Number).filter((d) => d >= 1 && d <= 7);
    const offset = Number(s.utcOffsetMinutes) || 0;
    for (const departure of upcoming(days, Number(s.hour) || 0, Number(s.minute) || 0, offset, now)) {
      const key = dayKey(departure, offset);
      if (s.made?.[key]) continue;
      updates[`ads/${id}-${key}`] = {
        ...s.template, adId: `${id}-${key}`, departureTime: departure, status: "OPEN",
        createdAt: now, expiresAt: departure + RIDE_LIFETIME,
      };
      updates[`rideSeries/${id}/made/${key}`] = true;
      created++;
    }
    // Forget days older than a month so `made` stays small.
    for (const key of Object.keys(s.made || {})) {
      const t = Date.UTC(+key.slice(0, 4), +key.slice(4, 6) - 1, +key.slice(6, 8));
      if (t < stale) updates[`rideSeries/${id}/made/${key}`] = null;
    }
  }

  if (Object.keys(updates).length) await db(env, "PATCH", "", updates);
  return { series: Object.keys(all).length, created };
}

// ─── Expired rides ──────────────────────────────────────────────────────────

/**
 * Keeps /ads to live rides only. Runs hourly; a ride expires an hour after it leaves.
 * - Expired rides nobody was accepted on are deleted.
 * - Expired rides with accepted passengers are marked COMPLETED and kept for two weeks,
 *   so both sides can still mark the ride done and review each other, then deleted.
 */
export async function purgeExpiredRides(env, now = Date.now()) {
  const expired = (await db(env, "GET", "ads", { orderBy: '"expiresAt"', endAt: String(now) })) || {};
  const updates = {};
  let deleted = 0;
  let completed = 0;

  for (const [adId, ride] of Object.entries(expired)) {
    const expiresAt = ride.expiresAt || (ride.createdAt || 0) + EXPIRY_WINDOW;
    if (!expiresAt || expiresAt > now) continue;

    const requests = (await db(env, "GET", "joinRequests", { orderBy: '"adId"', equalTo: JSON.stringify(adId) })) || {};
    const booked = Object.values(requests).some((r) => r.status === "ACCEPTED");

    if (!booked || expiresAt + KEEP_BOOKED_RIDES <= now) {
      updates[`ads/${adId}`] = null;
      deleted++;
    } else if (ride.status !== "COMPLETED" && ride.status !== "CANCELLED") {
      updates[`ads/${adId}/status`] = "COMPLETED";
      completed++;
    }
  }

  if (Object.keys(updates).length) await db(env, "PATCH", "", updates);
  return { deleted, completed };
}

// ─── Firebase plumbing ──────────────────────────────────────────────────────

function serviceAccount(env) {
  if (!env.FIREBASE_SERVICE_ACCOUNT) throw new Error("FIREBASE_SERVICE_ACCOUNT secret is missing");
  return JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
}

async function caller(request, env) {
  const header = request.headers.get("authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!token) throw new HttpError(401, "Sign in required");
  try {
    return await verifyIdToken(token, env.FIREBASE_PROJECT_ID);
  } catch (err) {
    throw new HttpError(401, "Sign in again to continue");
  }
}

/** Realtime Database REST call as the service account. */
async function db(env, method, path, query = {}, body) {
  if (method !== "GET" && method !== "DELETE" && body === undefined) [body, query] = [query, {}];
  const access = await googleAccessToken(serviceAccount(env), env);
  const params = new URLSearchParams(query).toString();
  const res = await fetch(`${env.FIREBASE_DATABASE_URL}/${path}.json${params ? `?${params}` : ""}`, {
    method,
    headers: { authorization: `Bearer ${access}`, "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`Database ${method} /${path} failed: ${res.status} ${await res.text()}`);
  return res.json();
}

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json" } });
}
