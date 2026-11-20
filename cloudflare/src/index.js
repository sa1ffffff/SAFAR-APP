/**
 * Safar's Cloudflare Worker: the parts of the backend Firebase's free plan can't run.
 *
 *   PUT    /avatar               upload the caller's profile photo (JPEG body)
 *   DELETE /avatar               remove it (account deletion)
 *   GET    /avatars/{uid}.jpg    serve a profile photo
 *   POST   /notify               push an in-app notification or chat message the caller just wrote
 *   GET    /route?from=&to=      driving route between two points (OpenStreetMap data via OSRM)
 *   POST   /wanted/match         match a rider's new wanted ride against open rides
 *   POST   /rides/match          match a driver's new or edited ride against wanted rides
 *   POST   /offers               a driver offers a rider a seat on one of their rides
 *   POST   /offers/respond       the rider takes or declines it
 *   cron (hourly)                close expired rides, and post weekly repeats a week ahead
 *   cron (every 10 min)          re-match wanted rides, push new matches, expire old ones
 *
 * Callers prove who they are with their Firebase ID token. The Worker reaches Firebase with a
 * service account (secret FIREBASE_SERVICE_ACCOUNT), which bypasses database rules, so every
 * route checks ownership itself.
 */
import { verifyIdToken, googleAccessToken } from "./google.js";
import { MATCH_THRESHOLD, score, haversine, decodePolyline, encodePolyline, thin } from "./matching.js";

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

  async scheduled(event, env, ctx) {
    ctx.waitUntil((async () => {
      if (event.cron === "*/10 * * * *") {
        console.log("sweepWanted", JSON.stringify(await sweepWanted(env, ctx)));
        return;
      }
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
  if (path === "/wanted/match" && request.method === "POST") return matchWantedRoute(request, env, ctx, await caller(request, env));
  if (path === "/rides/match" && request.method === "POST") return matchRideRoute(request, env, ctx, await caller(request, env));
  if (path === "/offers" && request.method === "POST") return createOffer(request, env, ctx, await caller(request, env));
  if (path === "/offers/respond" && request.method === "POST") return respondOffer(request, env, await caller(request, env));
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
  const from = url.searchParams.get("from") || "";
  const to = url.searchParams.get("to") || "";
  if (!POINT.test(from) || !POINT.test(to)) throw new HttpError(400, "Bad points");
  return fetchRoute(from, to, ctx);
}

async function fetchRoute(from, to, ctx) {
  const round = (p) => p.split(",").map((n) => Number(n).toFixed(4)).join(",");
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
      updates[`rideMatches/${adId}`] = null;
      // Its seat requests go too, so no rider is left with a booking for a ride that's gone.
      for (const requestId of Object.keys(requests)) updates[`joinRequests/${requestId}`] = null;
      deleted++;
    } else if (ride.status !== "COMPLETED" && ride.status !== "CANCELLED") {
      updates[`ads/${adId}/status`] = "COMPLETED";
      completed++;
    }
  }

  if (Object.keys(updates).length) await db(env, "PATCH", "", updates);
  return { deleted, completed };
}

// ─── Wanted rides: matching ─────────────────────────────────────────────────
//
// wanted/{uid}/{id}               a rider's wanted ride (written by the app)
// wantedMatches/{uid}/{id}/{ride} rides that match it, for the rider (written here only)
// rideMatches/{ride}/{uid}_{id}   riders a ride could take, for its driver (written here only)

const PUSHES_PER_SWEEP = 2; // per wanted ride, so a burst of new rides can't flood a phone
const shortName = (name) => String(name || "").split(",")[0].trim();
const firstName = (name) => String(name || "").trim().split(/\s+/)[0] || "A driver";

function clock(ms, offsetMinutes = 300) {
  const d = new Date(ms + (Number(offsetMinutes) || 300) * 60000);
  const h = d.getUTCHours();
  return `${h % 12 || 12}:${String(d.getUTCMinutes()).padStart(2, "0")} ${h < 12 ? "AM" : "PM"}`;
}

function distanceLabel(m) {
  return m < 1000 ? `${Math.max(10, Math.round(m / 10) * 10)} m` : `${(m / 1000).toFixed(1)} km`;
}

async function rideRoute(ctx, ride) {
  if ([ride.originLat, ride.originLng, ride.destinationLat, ride.destinationLng].some((v) => typeof v !== "number")) return null;
  try {
    const res = await fetchRoute(`${ride.originLat},${ride.originLng}`, `${ride.destinationLat},${ride.destinationLng}`, ctx);
    return decodePolyline((await res.json()).polyline);
  } catch {
    return null; // score against the ride's two ends instead
  }
}

function rideEntry(rideId, ride, m) {
  return {
    rideId, score: m.score, pickupM: m.pickupM, dropM: m.dropM, passes: m.passes,
    departureTime: ride.departureTime, price: ride.pricePerSeat || 0,
    driverUid: ride.creatorUid, driverName: ride.creatorName || "", driverPhoto: ride.creatorPhotoUrl || null,
    origin: ride.origin || "", destination: ride.destination || "",
    originLat: ride.originLat ?? null, originLng: ride.originLng ?? null,
    destinationLat: ride.destinationLat ?? null, destinationLng: ride.destinationLng ?? null,
  };
}

function riderEntry(want, m) {
  return {
    uid: want.uid, wantId: want.id, name: want.name || "", photoUrl: want.photoUrl || null, seats: want.seats || 1,
    pickup: want.origin || "", pickupLat: want.originLat, pickupLng: want.originLng,
    dropoff: want.destination || "", pickupM: m.pickupM, passes: m.passes, score: m.score,
    windowStart: want.windowStart, windowEnd: want.windowEnd, anyTime: !want.flex,
  };
}

const wantKey = (want) => `${want.uid}_${want.id}`;

/**
 * Re-matches one wanted ride against every open ride in its time window. With [notify],
 * pushes the best new matches; without (the rider is looking at the screen), records
 * them as already seen.
 */
export async function matchWant(env, ctx, want, { notify, now = Date.now() }) {
  if (want.status !== "ACTIVE") return { matches: 0, pushed: 0 };
  const [rides, prev] = await Promise.all([
    db(env, "GET", "ads", { orderBy: '"departureTime"', startAt: String(want.windowStart), endAt: String(want.windowEnd) }),
    db(env, "GET", `wantedMatches/${want.uid}/${want.id}`),
  ]);
  const found = {};
  const fresh = [];
  for (const [rideId, ride] of Object.entries(rides || {})) {
    if (!ride || typeof ride.originLat !== "number") continue;
    if (haversine(want.originLat, want.originLng, ride.originLat, ride.originLng) > 400000) continue;
    const path = await rideRoute(ctx, ride);
    const m = score(want, ride, path);
    if (!m || m.score < MATCH_THRESHOLD) continue;
    found[rideId] = rideEntry(rideId, ride, m);
    if (!want.notified?.[rideId]) fresh.push({ rideId, ride, m, path });
  }

  const updates = { [`wantedMatches/${want.uid}/${want.id}`]: Object.keys(found).length ? found : null };
  for (const rideId of Object.keys(prev || {})) if (!found[rideId]) updates[`rideMatches/${rideId}/${wantKey(want)}`] = null;
  for (const [rideId, entry] of Object.entries(found)) {
    updates[`rideMatches/${rideId}/${wantKey(want)}`] = want.visible === false ? null : riderEntry(want, entry);
  }
  const toPush = notify ? fresh.sort((a, b) => b.m.score - a.m.score).slice(0, PUSHES_PER_SWEEP) : [];
  for (const f of fresh) updates[`wanted/${want.uid}/${want.id}/notified/${f.rideId}`] = now;
  await db(env, "PATCH", "", updates);
  for (const f of toPush) await pushRideFound(env, want, f, now);
  return { matches: Object.keys(found).length, pushed: toPush.length };
}

async function pushRideFound(env, want, { rideId, ride, m, path }, now) {
  const who = firstName(ride.creatorName);
  const title = `Ride found for ${clock(ride.departureTime, want.utcOffsetMinutes)}`;
  const body = m.pickupM <= 300
    ? `${who} ${m.passes ? "drives right past" : "leaves right by"} your pickup.`
    : `${who} ${m.passes ? "passes" : "leaves"} ${distanceLabel(m.pickupM)} from your pickup.`;
  const route = path && path.length > 1 ? path : [[ride.originLat, ride.originLng], [ride.destinationLat, ride.destinationLng]];
  await deliver(env, want.uid, {
    type: "RIDE_FOUND", title, message: body, relatedAdId: rideId, relatedRequestId: want.id,
    fromUserId: ride.creatorUid || "", fromUserName: ride.creatorName || "", fromUserPhoto: ride.creatorPhotoUrl || null,
  }, now, {
    open: `ride:${rideId}`, photo: ride.creatorPhotoUrl || "",
    route: encodePolyline(thin(route, 60)),
    pLat: String(want.originLat), pLng: String(want.originLng),
  });
}

/** Writes an in-app notification for [uid] and pushes it to their phone. */
async function deliver(env, uid, n, now, extra = {}) {
  const id = newId();
  const [token] = await Promise.all([
    db(env, "GET", `pushTokens/${uid}`),
    db(env, "PATCH", `notifications/${uid}/${id}`, { notificationId: id, ...n, timestamp: now, read: false, pushedAt: now }),
  ]);
  if (typeof token !== "string" || !token) return "no device";
  const sent = await sendPush(env, token, {
    title: n.title, body: n.message,
    data: { type: n.type, adId: String(n.relatedAdId || ""), ...extra },
  });
  if (sent === "unregistered") await db(env, "DELETE", `pushTokens/${uid}`);
  return sent;
}

async function matchWantedRoute(request, env, ctx, uid) {
  const body = await request.json().catch(() => ({}));
  if (!PUSH_ID.test(body.id || "")) throw new HttpError(400, "Bad id");
  const want = await db(env, "GET", `wanted/${uid}/${body.id}`);
  if (!want) throw new HttpError(404, "Not found");
  return json(await matchWant(env, ctx, { ...want, uid, id: body.id }, { notify: false }));
}

/** A driver posted or edited a ride: match it against every active wanted ride and push new matches. */
export async function matchRide(env, ctx, rideId, now = Date.now()) {
  const [ride, all, prev] = await Promise.all([
    db(env, "GET", `ads/${rideId}`), db(env, "GET", "wanted"), db(env, "GET", `rideMatches/${rideId}`),
  ]);
  const updates = {};
  const pushes = [];
  const path = ride ? await rideRoute(ctx, ride) : null;
  const kept = new Set();
  for (const [uid, wants] of Object.entries(all || {})) {
    for (const [id, w] of Object.entries(wants || {})) {
      const want = { ...w, uid, id };
      if (!ride || want.status !== "ACTIVE" || want.windowEnd < now) continue;
      const m = score(want, ride, path);
      if (!m || m.score < MATCH_THRESHOLD) continue;
      kept.add(wantKey(want));
      const entry = rideEntry(rideId, ride, m);
      updates[`wantedMatches/${uid}/${id}/${rideId}`] = entry;
      updates[`rideMatches/${rideId}/${wantKey(want)}`] = want.visible === false ? null : riderEntry(want, entry);
      if (!want.notified?.[rideId]) {
        updates[`wanted/${uid}/${id}/notified/${rideId}`] = now;
        pushes.push({ want, f: { rideId, ride, m, path } });
      }
    }
  }
  for (const [key, entry] of Object.entries(prev || {})) {
    if (kept.has(key)) continue;
    updates[`rideMatches/${rideId}/${key}`] = null;
    if (entry?.uid && entry?.wantId) updates[`wantedMatches/${entry.uid}/${entry.wantId}/${rideId}`] = null;
  }
  if (Object.keys(updates).length) await db(env, "PATCH", "", updates);
  for (const p of pushes) await pushRideFound(env, p.want, p.f, now);
  return { matches: kept.size, pushed: pushes.length };
}

async function matchRideRoute(request, env, ctx, uid) {
  const body = await request.json().catch(() => ({}));
  if (!PUSH_ID.test(body.id || "")) throw new HttpError(400, "Bad id");
  const ride = await db(env, "GET", `ads/${body.id}`);
  if (ride && ride.creatorUid !== uid) throw new HttpError(403, "Not your ride");
  return json(await matchRide(env, ctx, body.id));
}

async function clearMatches(env, uid, id, updates) {
  const prev = (await db(env, "GET", `wantedMatches/${uid}/${id}`)) || {};
  updates[`wantedMatches/${uid}/${id}`] = null;
  for (const rideId of Object.keys(prev)) updates[`rideMatches/${rideId}/${uid}_${id}`] = null;
}

/**
 * Every 10 minutes: closes wanted rides whose window has passed or that got booked,
 * re-matches the rest (catching rides the app couldn't report, and weekly repeats),
 * and expires offers on rides that have left.
 */
export async function sweepWanted(env, ctx, now = Date.now()) {
  const all = (await db(env, "GET", "wanted")) || {};
  const out = { active: 0, expired: 0, booked: 0, pushed: 0, offersExpired: 0 };
  for (const [uid, wants] of Object.entries(all)) {
    for (const [id, w] of Object.entries(wants || {})) {
      if (!w) continue;
      const updates = {};
      if (w.status !== "ACTIVE") {
        if ((w.windowEnd || 0) < now - 7 * DAY) updates[`wanted/${uid}/${id}`] = null;
      } else if ((w.windowEnd || 0) < now) {
        updates[`wanted/${uid}/${id}/status`] = "EXPIRED";
        await clearMatches(env, uid, id, updates);
        out.expired++;
      } else if (await hasBooking(env, uid, w)) {
        updates[`wanted/${uid}/${id}/status`] = "BOOKED";
        await clearMatches(env, uid, id, updates);
        out.booked++;
      } else {
        out.active++;
        out.pushed += (await matchWant(env, ctx, { ...w, uid, id }, { notify: true, now })).pushed;
      }
      if (Object.keys(updates).length) await db(env, "PATCH", "", updates);
    }
  }

  const offers = (await db(env, "GET", "offers")) || {};
  const updates = {};
  for (const [riderUid, list] of Object.entries(offers)) {
    for (const [oid, o] of Object.entries(list || {})) {
      if (!o) continue;
      if (o.status === "PENDING" && (o.departureTime || 0) < now) {
        updates[`offers/${riderUid}/${oid}/status`] = "EXPIRED";
        updates[`driverOffers/${o.driverUid}/${oid}/status`] = "EXPIRED";
        out.offersExpired++;
      } else if ((o.departureTime || 0) < now - 7 * DAY) {
        updates[`offers/${riderUid}/${oid}`] = null;
        updates[`driverOffers/${o.driverUid}/${oid}`] = null;
      }
    }
  }
  if (Object.keys(updates).length) await db(env, "PATCH", "", updates);
  return out;
}

/** True once the rider has an accepted seat on a ride leaving inside this window. */
async function hasBooking(env, uid, want) {
  const mine = (await db(env, "GET", "joinRequests", { orderBy: '"requestorUid"', equalTo: JSON.stringify(uid) })) || {};
  for (const r of Object.values(mine)) {
    if (r?.status !== "ACCEPTED") continue;
    const ride = await db(env, "GET", `ads/${r.adId}`);
    if (ride && ride.departureTime >= want.windowStart && ride.departureTime <= want.windowEnd) return true;
  }
  return false;
}

// ─── Wanted rides: offers ───────────────────────────────────────────────────
//
// offers/{riderUid}/{id}        an offer, for the rider
// driverOffers/{driverUid}/{id} the same offer's status, for the driver
// Both are written here only, so neither side can change the other's.

async function createOffer(request, env, ctx, uid) {
  const body = await request.json().catch(() => ({}));
  const { rideId, riderUid, wantId } = body;
  if (![rideId, riderUid, wantId].every((v) => PUSH_ID.test(v || ""))) throw new HttpError(400, "Bad request");
  const now = Date.now();
  const [ride, want, mine, match] = await Promise.all([
    db(env, "GET", `ads/${rideId}`), db(env, "GET", `wanted/${riderUid}/${wantId}`),
    db(env, "GET", `driverOffers/${uid}`), db(env, "GET", `rideMatches/${rideId}/${riderUid}_${wantId}`),
  ]);
  if (!ride || ride.creatorUid !== uid) throw new HttpError(403, "Not your ride");
  if (ride.status !== "OPEN" || (ride.departureTime || 0) < now) throw new HttpError(409, "This ride is closed");
  if (!want || want.status !== "ACTIVE" || want.visible === false) throw new HttpError(409, "They're no longer looking");
  if ((ride.availableSeats || 0) < (want.seats || 1)) throw new HttpError(409, "Not enough seats left");
  const existing = Object.entries(mine || {}).find(([, o]) => o.rideId === rideId && o.riderUid === riderUid && o.wantId === wantId && o.status === "PENDING");
  if (existing) return json({ ok: true, id: existing[0] });

  const id = newId();
  const offer = {
    id, wantId, rideId, riderUid, driverUid: uid,
    driverName: ride.creatorName || "", driverPhoto: ride.creatorPhotoUrl || null, vehicle: ride.vehicleDetails || "",
    departureTime: ride.departureTime, price: ride.pricePerSeat || 0, seats: want.seats || 1,
    origin: ride.origin || "", destination: ride.destination || "",
    originLat: ride.originLat ?? null, originLng: ride.originLng ?? null,
    destinationLat: ride.destinationLat ?? null, destinationLng: ride.destinationLng ?? null,
    pickupLat: want.originLat ?? null, pickupLng: want.originLng ?? null,
    pickupM: match?.pickupM ?? null, passes: !!match?.passes, status: "PENDING", createdAt: now,
  };
  await db(env, "PATCH", "", {
    [`offers/${riderUid}/${id}`]: offer,
    [`driverOffers/${uid}/${id}`]: { riderUid, wantId, rideId, status: "PENDING", createdAt: now },
  });
  await deliver(env, riderUid, {
    type: "SEAT_OFFERED",
    title: `${firstName(ride.creatorName)} offered you a seat`,
    message: `${clock(ride.departureTime, want.utcOffsetMinutes)} to ${shortName(ride.destination)} · Rs ${offer.price}`,
    relatedAdId: rideId, relatedRequestId: id,
    fromUserId: uid, fromUserName: ride.creatorName || "", fromUserPhoto: ride.creatorPhotoUrl || null,
  }, now, { open: `offer:${id}`, photo: ride.creatorPhotoUrl || "" });
  return json({ ok: true, id });
}

async function respondOffer(request, env, uid) {
  const body = await request.json().catch(() => ({}));
  if (!PUSH_ID.test(body.id || "")) throw new HttpError(400, "Bad id");
  const offer = await db(env, "GET", `offers/${uid}/${body.id}`);
  if (!offer) throw new HttpError(404, "Not found");
  if (offer.status !== "PENDING") throw new HttpError(409, offer.status === "EXPIRED" ? "This offer has expired" : "You've already answered this offer");
  const now = Date.now();

  if (!body.accept) {
    await db(env, "PATCH", "", {
      [`offers/${uid}/${body.id}/status`]: "DECLINED",
      [`driverOffers/${offer.driverUid}/${body.id}/status`]: "DECLINED",
    });
    return json({ ok: true });
  }

  const [ride, user, want] = await Promise.all([
    db(env, "GET", `ads/${offer.rideId}`), db(env, "GET", `users/${uid}`), db(env, "GET", `wanted/${uid}/${offer.wantId}`),
  ]);
  if (!ride || ride.status !== "OPEN" || (ride.departureTime || 0) < now) throw new HttpError(409, "This ride is no longer available");
  const seats = offer.seats || 1;
  await takeSeats(env, offer.rideId, seats);

  const requestId = newId();
  const routeLabel = `${shortName(ride.origin)} → ${shortName(ride.destination)}`;
  const updates = {
    [`joinRequests/${requestId}`]: {
      requestId, adId: offer.rideId, requestorUid: uid, requestorName: user?.name || "",
      requestorPhone: user?.phone || "", requestorPhotoUrl: user?.photoUrl || null, adCreatorUid: offer.driverUid,
      seats, routeLabel, status: "ACCEPTED", timestamp: now,
      requesterMarkedComplete: false, creatorMarkedComplete: false, requesterReviewedCreator: false, creatorReviewedRequester: false,
    },
    [`offers/${uid}/${body.id}/status`]: "ACCEPTED",
    [`offers/${uid}/${body.id}/requestId`]: requestId,
    [`driverOffers/${offer.driverUid}/${body.id}/status`]: "ACCEPTED",
  };
  if (want) {
    updates[`wanted/${uid}/${offer.wantId}/status`] = "BOOKED";
    await clearMatches(env, uid, offer.wantId, updates);
  }
  await db(env, "PATCH", "", updates);
  await deliver(env, offer.driverUid, {
    type: "OFFER_TAKEN",
    title: `${firstName(user?.name)} took your seat`,
    message: `${routeLabel}, ${clock(ride.departureTime, want?.utcOffsetMinutes)}`,
    relatedAdId: offer.rideId, relatedRequestId: requestId,
    fromUserId: uid, fromUserName: user?.name || "", fromUserPhoto: user?.photoUrl || null,
  }, now, { open: `ride:${offer.rideId}`, photo: user?.photoUrl || "" });
  return json({ ok: true, requestId });
}

/** Takes [seats] off a ride atomically (conditional write on the seat count), as the app's transaction does. */
async function takeSeats(env, rideId, seats) {
  const path = `ads/${rideId}/availableSeats.json`;
  for (let attempt = 0; attempt < 5; attempt++) {
    const res = await rawDb(env, "GET", path, undefined, { "X-Firebase-ETag": "true" });
    if (!res.ok) throw new Error(`Seat read failed: ${res.status}`);
    const etag = res.headers.get("ETag");
    const left = await res.json();
    if (typeof left !== "number" || left < seats) throw new HttpError(409, "No seats left on this ride");
    const put = await rawDb(env, "PUT", path, left - seats, { "if-match": etag });
    if (put.ok) {
      if (left - seats === 0) await db(env, "PATCH", `ads/${rideId}`, { status: "FILLED" });
      return;
    }
    if (put.status !== 412) throw new Error(`Seat write failed: ${put.status}`);
  }
  throw new HttpError(409, "That ride is busy. Try again.");
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
  const params = new URLSearchParams(query).toString();
  const res = await rawDb(env, method, `${path}.json${params ? `?${params}` : ""}`, body);
  if (!res.ok) throw new Error(`Database ${method} /${path} failed: ${res.status} ${await res.text()}`);
  return res.json();
}

async function rawDb(env, method, pathAndQuery, body, headers = {}) {
  const access = await googleAccessToken(serviceAccount(env), env);
  return fetch(`${env.FIREBASE_DATABASE_URL}/${pathAndQuery}`, {
    method,
    headers: { authorization: `Bearer ${access}`, "content-type": "application/json", ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/** A time-ordered random key, like the app's push ids. */
function newId() {
  const abc = "0123456789abcdefghijklmnopqrstuvwxyz";
  const rand = crypto.getRandomValues(new Uint8Array(10));
  return Date.now().toString(36).padStart(9, "0") + Array.from(rand, (b) => abc[b % 36]).join("");
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
