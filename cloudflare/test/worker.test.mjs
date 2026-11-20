// Runs the Worker against in-memory fakes of Google auth, the Realtime Database and FCM.
// node --test test/
import { test } from "node:test";
import assert from "node:assert/strict";
import worker, { purgeExpiredRides, extendSeries, upcoming, dayKey, sweepWanted } from "../src/index.js";
import { score, encodePolyline, decodePolyline, nearestOnPath, MATCH_THRESHOLD } from "../src/matching.js";

const PROJECT = "safar-test";
const DB_URL = "https://db.test";
const enc = new TextEncoder();
const b64url = (bytes) => Buffer.from(bytes).toString("base64url");

// ─── Fake Google ────────────────────────────────────────────────────────────

const { publicKey, privateKey } = await crypto.subtle.generateKey(
  { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
  true, ["sign", "verify"],
);
const jwk = { ...(await crypto.subtle.exportKey("jwk", publicKey)), kid: "k1", alg: "RS256", use: "sig" };
const pkcs8 = Buffer.from(await crypto.subtle.exportKey("pkcs8", privateKey)).toString("base64");
const serviceAccount = JSON.stringify({
  project_id: PROJECT, client_email: "worker@test", token_uri: "https://oauth.test/token",
  private_key: `-----BEGIN PRIVATE KEY-----\n${pkcs8}\n-----END PRIVATE KEY-----\n`,
});

async function idToken(uid, overrides = {}) {
  const now = Math.floor(Date.now() / 1000);
  const head = b64url(enc.encode(JSON.stringify({ alg: "RS256", kid: "k1" })));
  const body = b64url(enc.encode(JSON.stringify({
    aud: PROJECT, iss: `https://securetoken.google.com/${PROJECT}`, sub: uid, iat: now, exp: now + 3600, ...overrides,
  })));
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", privateKey, enc.encode(`${head}.${body}`));
  return `${head}.${body}.${b64url(sig)}`;
}

// ─── Fake database and FCM ──────────────────────────────────────────────────

let data;
let pushes;
let osrmCalls = 0;
let osrmFixed = "_p~iF~ps|U_ulLnnqC"; // null: answer with a straight line between the two points
const routeCache = new Map();
const at = (path) => path.split("/").filter(Boolean).reduce((node, k) => node?.[k], data);
function write(path, value) {
  const keys = path.split("/").filter(Boolean);
  if (!keys.length) { data = value ?? {}; return; }
  let node = data;
  for (const k of keys.slice(0, -1)) node = node[k] ??= {};
  if (value === null) delete node[keys.at(-1)];
  else node[keys.at(-1)] = value;
}

globalThis.caches = { default: { match: async (r) => routeCache.get(r.url)?.clone(), put: async (r, res) => { if (r.url.startsWith("https://route.cache")) routeCache.set(r.url, res); } } };
globalThis.fetch = async (input, init = {}) => {
  const url = new URL(String(input));
  const method = init.method || "GET";
  const ok = (body) => new Response(JSON.stringify(body), { status: 200 });

  if (url.href.includes("securetoken@system")) return new Response(JSON.stringify({ keys: [jwk] }), { headers: { "cache-control": "max-age=600" } });
  if (url.origin === "https://oauth.test") return ok({ access_token: "access", expires_in: 3600 });
  if (url.hostname === "fcm.googleapis.com") {
    const { message } = JSON.parse(init.body);
    pushes.push(message);
    return message.token === "dead-token" ? new Response('{"error":{"status":"NOT_FOUND","details":["UNREGISTERED"]}}', { status: 404 }) : ok({ name: "m1" });
  }
  if (url.hostname === "router.project-osrm.org") {
    osrmCalls++;
    const [a, b] = url.pathname.split("/").pop().split(";").map((p) => p.split(",").map(Number).reverse());
    const geometry = osrmFixed ?? encodePolyline([a, [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2], b]);
    return ok({ code: "Ok", routes: [{ geometry, distance: 12345.6, duration: 1500.4 }] });
  }
  if (url.origin === DB_URL) {
    assert.equal(init.headers.authorization, "Bearer access");
    const path = decodeURIComponent(url.pathname.replace(/\.json$/, ""));
    if (method === "GET") {
      let value = at(path) ?? null;
      const orderBy = url.searchParams.get("orderBy");
      if (orderBy && value) {
        const field = JSON.parse(orderBy);
        const startAt = url.searchParams.get("startAt");
        const endAt = url.searchParams.get("endAt");
        const equalTo = url.searchParams.get("equalTo");
        value = Object.fromEntries(Object.entries(value).filter(([, v]) =>
          (startAt === null || (v[field] !== undefined && v[field] >= Number(startAt))) &&
          (endAt === null || (v[field] !== undefined && v[field] <= Number(endAt))) &&
          (equalTo === null || v[field] === JSON.parse(equalTo))));
      }
      const etag = init.headers["X-Firebase-ETag"] ? { ETag: `e${JSON.stringify(value)}` } : {};
      return new Response(JSON.stringify(value), { status: 200, headers: etag });
    }
    if (method === "PUT") {
      if (init.headers["if-match"] && init.headers["if-match"] !== `e${JSON.stringify(at(path) ?? null)}`) return new Response("{}", { status: 412 });
      write(path, JSON.parse(init.body));
      return ok(JSON.parse(init.body));
    }
    if (method === "PATCH") {
      for (const [k, v] of Object.entries(JSON.parse(init.body))) write(`${path}/${k}`, v);
      return ok({});
    }
    if (method === "DELETE") { write(path, null); return ok(null); }
  }
  throw new Error(`Unexpected fetch ${method} ${url}`);
};

// ─── Helpers ────────────────────────────────────────────────────────────────

const kv = new Map();
const env = {
  FIREBASE_PROJECT_ID: PROJECT, FIREBASE_DATABASE_URL: DB_URL, FIREBASE_SERVICE_ACCOUNT: serviceAccount,
  PHOTOS_KV: {
    put: async (k, v) => kv.set(k, v),
    get: async (k) => (kv.has(k) ? kv.get(k).buffer : null),
    delete: async (k) => kv.delete(k),
  },
};
const ctx = { waitUntil: () => {} };
const call = (method, path, { uid, token, body, headers = {} } = {}) => (async () => {
  const auth = token ?? (uid ? await idToken(uid) : null);
  return worker.fetch(new Request(`https://api.test${path}`, {
    method, body, headers: { ...(auth ? { authorization: `Bearer ${auth}` } : {}), ...headers },
  }), env, ctx);
})();
const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 1, 2, 3, 4]);

function reset() {
  data = { users: { alice: { name: "Alice" }, bob: { name: "Bob" } }, pushTokens: { bob: "bob-token" } };
  pushes = [];
  kv.clear();
}

// ─── Auth ───────────────────────────────────────────────────────────────────

test("rejects missing, forged, expired and wrong-project tokens", async () => {
  reset();
  assert.equal((await call("PUT", "/avatar", { body: JPEG })).status, 401);
  const good = await idToken("alice");
  const forged = good.slice(0, good.lastIndexOf(".") + 1) + b64url(new Uint8Array(256));
  assert.equal((await call("PUT", "/avatar", { token: forged, body: JPEG })).status, 401);
  assert.equal((await call("PUT", "/avatar", { token: await idToken("alice", { exp: 1000 }), body: JPEG })).status, 401);
  assert.equal((await call("PUT", "/avatar", { token: await idToken("alice", { aud: "other" }), body: JPEG })).status, 401);
  assert.equal(kv.size, 0);
});

// ─── Photos ─────────────────────────────────────────────────────────────────

test("uploads, serves and deletes the caller's own photo", async () => {
  reset();
  const res = await call("PUT", "/avatar", { uid: "alice", body: JPEG, headers: { "content-type": "image/jpeg" } });
  assert.equal(res.status, 200);
  const { photoUrl } = await res.json();
  assert.match(photoUrl, /^https:\/\/api\.test\/avatars\/alice\.jpg\?v=\d+$/);
  assert.equal(data.users.alice.photoUrl, photoUrl);
  assert.equal(data.users.alice.name, "Alice", "PATCH keeps the rest of the profile");

  const img = await call("GET", "/avatars/alice.jpg?v=1");
  assert.equal(img.status, 200);
  assert.equal(img.headers.get("content-type"), "image/jpeg");
  assert.deepEqual(new Uint8Array(await img.arrayBuffer()), JPEG);

  assert.equal((await call("DELETE", "/avatar", { uid: "alice" })).status, 200);
  assert.equal((await call("GET", "/avatars/alice.jpg")).status, 404);
});

test("refuses non-JPEG and oversized photos", async () => {
  reset();
  assert.equal((await call("PUT", "/avatar", { uid: "alice", body: enc.encode("<svg/>") })).status, 415);
  assert.equal((await call("PUT", "/avatar", { uid: "alice", body: new Uint8Array(1024 * 1024 + 1) })).status, 413);
  assert.equal(kv.size, 0);
});

// ─── Push ───────────────────────────────────────────────────────────────────

function addNotification(id, fields = {}) {
  data.notifications ??= { bob: {} };
  data.notifications.bob[id] = {
    fromUserId: "alice", title: "Seat request", message: "Alice wants 1 seat", type: "REQUEST_RECEIVED",
    relatedAdId: "ad1", timestamp: Date.now(), read: false, ...fields,
  };
}

test("pushes a fresh notification once, only for its author", async () => {
  reset();
  addNotification("n1");
  const body = JSON.stringify({ kind: "notification", to: "bob", id: "n1" });

  assert.equal((await call("POST", "/notify", { uid: "bob", body })).status, 403, "bob didn't write it");
  assert.equal(pushes.length, 0);

  const res = await call("POST", "/notify", { uid: "alice", body });
  assert.deepEqual(await res.json(), { ok: true, sent: true });
  assert.equal(pushes.length, 1);
  assert.equal(pushes[0].token, "bob-token");
  assert.equal(pushes[0].data.title, "Seat request");
  assert.equal(pushes[0].data.open, "notifications");
  assert.ok(data.notifications.bob.n1.pushedAt);

  const again = await call("POST", "/notify", { uid: "alice", body });
  assert.equal((await again.json()).skipped, "already pushed");
  assert.equal(pushes.length, 1);
});

test("won't push old notifications or unknown ones", async () => {
  reset();
  addNotification("old", { timestamp: Date.now() - 60 * 60 * 1000 });
  assert.equal((await call("POST", "/notify", { uid: "alice", body: JSON.stringify({ kind: "notification", to: "bob", id: "old" }) })).status, 409);
  assert.equal((await call("POST", "/notify", { uid: "alice", body: JSON.stringify({ kind: "notification", to: "bob", id: "nope" }) })).status, 403);
  assert.equal((await call("POST", "/notify", { uid: "alice", body: JSON.stringify({ kind: "notification", to: "../x", id: "n" }) })).status, 400);
  assert.equal(pushes.length, 0);
});

test("pushes a chat message to the other participant and opens that chat", async () => {
  reset();
  data.messages = { alice_bob: { m1: { senderId: "alice", senderName: "Alice", text: "At the gate", timestamp: Date.now() } } };
  const body = JSON.stringify({ kind: "message", cid: "alice_bob", id: "m1" });

  assert.equal((await call("POST", "/notify", { uid: "carol", body })).status, 403, "outsider");
  assert.equal((await call("POST", "/notify", { uid: "bob", body })).status, 403, "bob isn't the sender");

  assert.equal((await call("POST", "/notify", { uid: "alice", body })).status, 200);
  assert.equal(pushes.length, 1);
  assert.equal(pushes[0].token, "bob-token");
  assert.equal(pushes[0].data.body, "At the gate");
  assert.equal(pushes[0].data.open, "chat:alice");
});

test("drops a push token FCM says is dead", async () => {
  reset();
  data.pushTokens.bob = "dead-token";
  addNotification("n1");
  await call("POST", "/notify", { uid: "alice", body: JSON.stringify({ kind: "notification", to: "bob", id: "n1" }) });
  assert.equal(data.pushTokens.bob, undefined);
});

test("skips quietly when the recipient has no device", async () => {
  reset();
  delete data.pushTokens.bob;
  addNotification("n1");
  const res = await call("POST", "/notify", { uid: "alice", body: JSON.stringify({ kind: "notification", to: "bob", id: "n1" }) });
  assert.equal((await res.json()).skipped, "no device");
});

// ─── Cleanup ────────────────────────────────────────────────────────────────

test("purges expired rides and keeps booked ones for reviews", async () => {
  reset();
  const now = Date.UTC(2026, 10, 16);
  const DAY = 86400000;
  data.ads = {
    live: { expiresAt: now + DAY, status: "OPEN" },
    stale: { expiresAt: now - DAY, status: "OPEN" },
    booked: { expiresAt: now - DAY, status: "OPEN" },
    bookedOld: { expiresAt: now - 15 * DAY, status: "COMPLETED" },
    cancelled: { expiresAt: now - DAY, status: "CANCELLED" },
  };
  data.joinRequests = {
    r1: { adId: "booked", status: "ACCEPTED" },
    r2: { adId: "bookedOld", status: "ACCEPTED" },
    r3: { adId: "stale", status: "REJECTED" },
  };
  const result = await purgeExpiredRides(env, now);
  assert.deepEqual(Object.keys(data.ads).sort(), ["booked", "live"]);
  assert.equal(data.ads.booked.status, "COMPLETED");
  assert.equal(data.ads.live.status, "OPEN");
  assert.deepEqual(result, { deleted: 3, completed: 1 });
  assert.deepEqual(Object.keys(data.joinRequests).sort(), ["r1"], "requests on deleted rides are removed");
});

// ─── Routes ─────────────────────────────────────────────────────────────────

test("returns a cached driving route for signed-in users only", async () => {
  reset();
  osrmCalls = 0;
  const q = "/route?from=33.6844,73.0479&to=33.5651,73.0169";
  assert.equal((await call("GET", q)).status, 401);
  const res = await call("GET", q, { uid: "alice" });
  assert.deepEqual(await res.json(), { polyline: "_p~iF~ps|U_ulLnnqC", meters: 12346, seconds: 1500 });
  await call("GET", "/route?from=33.68441,73.04791&to=33.5651,73.0169", { uid: "alice" });
  assert.equal(osrmCalls, 1, "nearby points share the cached answer");
  assert.equal((await call("GET", "/route?from=abc&to=1,2", { uid: "alice" })).status, 400);
});

// ─── Weekly repeats ─────────────────────────────────────────────────────────

test("upcoming departures land on the chosen weekdays in local time", () => {
  const PKT = 300;
  const now = Date.UTC(2026, 10, 16, 5, 0); // Mon 16 Nov 2026, 10:00 in Pakistan
  const out = upcoming([1, 3, 5], 8, 30, PKT, now); // Mon/Wed/Fri 08:30 PKT
  // Today's 08:30 has already passed, so the next are Wed 18, Fri 20 and Mon 23.
  assert.deepEqual(out.map((t) => dayKey(t, PKT)), ["20261118", "20261120", "20261123"]);
  assert.equal(new Date(out[0]).getUTCHours(), 3, "08:30 PKT is 03:30 UTC");
});

test("extends weekly repeats a week ahead without recreating skipped days", async () => {
  reset();
  const now = Date.UTC(2026, 10, 16, 5, 0);
  data.rideSeries = {
    s1: {
      creatorUid: "alice", days: "1,2,3,4,5", hour: 8, minute: 0, utcOffsetMinutes: 300, active: true,
      template: { creatorUid: "alice", origin: "NUST", destination: "Saddar", availableSeats: 3, seriesId: "s1", repeatDays: "1,2,3,4,5" },
      made: { "20261117": true },
    },
    off: { creatorUid: "bob", days: "1,2,3,4,5,6,7", hour: 9, minute: 0, utcOffsetMinutes: 300, active: false, template: { creatorUid: "bob" } },
  };
  const r = await extendSeries(env, now);
  assert.equal(r.created, 4, "Wed, Thu, Fri, next Mon; Tue was already made");
  assert.deepEqual(Object.keys(data.ads).sort(), ["s1-20261118", "s1-20261119", "s1-20261120", "s1-20261123"]);
  const wed = data.ads["s1-20261118"];
  assert.equal(wed.status, "OPEN");
  assert.equal(wed.origin, "NUST");
  assert.equal(wed.expiresAt - wed.departureTime, 60 * 60 * 1000);
  assert.equal(new Date(wed.departureTime).toISOString(), "2026-11-18T03:00:00.000Z");
  const again = await extendSeries(env, now);
  assert.equal(again.created, 0, "idempotent");
});

// ─── Wanted rides ───────────────────────────────────────────────────────────

const H = 60 * 60 * 1000;
const T0 = Date.now() + 24 * H; // tomorrow, so nothing has left yet
// Islamabad: the rider wants G-10 Markaz → Blue Area around T0.
const want = (over = {}) => ({
  name: "Alice", origin: "G-10 Markaz, Islamabad", originLat: 33.6758, originLng: 73.0153,
  destination: "Blue Area, Islamabad", destinationLat: 33.7100, destinationLng: 73.0600,
  kind: "LOCAL", time: T0, flex: 30, windowStart: T0 - 0.5 * H, windowEnd: T0 + 0.5 * H,
  seats: 1, visible: true, status: "ACTIVE", utcOffsetMinutes: 300, createdAt: Date.now(), ...over,
});
// Bob drives F-11 side → Jinnah Avenue, passing ~250 m from the rider's pickup.
const ride = (over = {}) => ({
  creatorUid: "bob", creatorName: "Bob Khan", origin: "F-11, Islamabad", originLat: 33.66, originLng: 73.00,
  destination: "Jinnah Avenue, Islamabad", destinationLat: 33.715, destinationLng: 73.065,
  departureTime: T0 + 10 * 60 * 1000, availableSeats: 2, totalSeats: 3, pricePerSeat: 250, status: "OPEN", ...over,
});

function resetWanted() {
  reset();
  osrmFixed = null;
  data.pushTokens.alice = "alice-token";
  data.users.alice.phone = "+923001112222";
}

test("scores a ride that passes the pickup, and rejects wrong direction, place and time", () => {
  const w = { ...want(), uid: "alice" };
  const m = score(w, ride());
  assert.ok(m && m.score >= MATCH_THRESHOLD, `matches (${m?.score})`);
  assert.ok(m.pickupM < 400, `pickup ${m.pickupM} m`);
  assert.equal(m.passes, true, "closer on the way than at the start");
  // The same road the other way: drop-off would come before pickup.
  const back = ride({ originLat: 33.715, originLng: 73.065, destinationLat: 33.66, destinationLng: 73.00 });
  assert.equal(score(w, back), null);
  assert.equal(score(w, ride({ originLat: 33.60, originLng: 73.05, destinationLat: 33.58, destinationLng: 73.10 })), null, "Rawalpindi");
  assert.equal(score(w, ride({ departureTime: T0 + 2 * H })), null, "outside the window");
  assert.equal(score(w, ride({ availableSeats: 0 })), null, "full");
  assert.equal(score(w, ride({ creatorUid: "alice" })), null, "own ride");
  // Pickup weighs most: 1.5 km off costs more than the same distance at the drop-off.
  const farPick = score(w, ride({ originLat: 33.6758 - 0.0135, originLng: 73.0153, departureTime: T0 }), [[33.6758 - 0.0135, 73.0153], [33.7100, 73.0600]]);
  const farDrop = score(w, ride({ departureTime: T0 }), [[33.6758, 73.0153], [33.7100 + 0.0135, 73.0600]]);
  assert.ok(farPick.score < farDrop.score, `${farPick.score} < ${farDrop.score}`);
});

test("polyline helpers round-trip and find the closest point along the way", () => {
  const pts = [[33.66, 73.0], [33.70, 73.04], [33.715, 73.065]];
  assert.deepEqual(decodePolyline(encodePolyline(pts)), pts);
  const near = nearestOnPath(33.70, 73.04, pts);
  assert.ok(near.meters < 1 && near.along > 0.5 && near.along < 0.9);
});

test("a new wanted ride finds open rides without pushing, and shows the rider to the driver", async () => {
  resetWanted();
  data.ads = { r1: ride(), far: ride({ originLat: 33.60, originLng: 73.05, destinationLat: 33.58, destinationLng: 73.10 }) };
  data.wanted = { alice: { w1: want() } };
  const res = await call("POST", "/wanted/match", { uid: "alice", body: JSON.stringify({ id: "w1" }) });
  assert.equal(res.status, 200);
  assert.deepEqual(Object.keys(data.wantedMatches.alice.w1), ["r1"]);
  assert.equal(data.wantedMatches.alice.w1.r1.driverName, "Bob Khan");
  assert.equal(data.rideMatches.r1.alice_w1.name, "Alice");
  assert.ok(data.wanted.alice.w1.notified.r1, "already on screen, so never pushed later");
  assert.equal(pushes.length, 0);
  assert.equal((await call("POST", "/wanted/match", { uid: "bob", body: JSON.stringify({ id: "w1" }) })).status, 404, "only your own");
});

test("a hidden wanted ride still matches but isn't shown to drivers", async () => {
  resetWanted();
  data.ads = { r1: ride() };
  data.wanted = { alice: { w1: want({ visible: false }) } };
  await call("POST", "/wanted/match", { uid: "alice", body: JSON.stringify({ id: "w1" }) });
  assert.ok(data.wantedMatches.alice.w1.r1);
  assert.equal(data.rideMatches?.r1?.alice_w1, undefined);
});

test("a newly posted ride pushes each matching rider once, with the route for the map", async () => {
  resetWanted();
  data.wanted = { alice: { w1: want() } };
  data.ads = { r1: ride() };
  assert.equal((await call("POST", "/rides/match", { uid: "alice", body: JSON.stringify({ id: "r1" }) })).status, 403);
  const res = await call("POST", "/rides/match", { uid: "bob", body: JSON.stringify({ id: "r1" }) });
  assert.deepEqual(await res.json(), { matches: 1, pushed: 1 });
  assert.equal(pushes.length, 1);
  const p = pushes[0];
  assert.equal(p.token, "alice-token");
  assert.equal(p.data.type, "RIDE_FOUND");
  assert.equal(p.data.open, "ride:r1");
  assert.match(p.data.title, /^Ride found for \d{1,2}:\d{2} (AM|PM)$/);
  assert.equal(p.data.body, "Bob drives right past your pickup.");
  assert.ok(decodePolyline(p.data.route).length >= 2);
  const feed = Object.values(data.notifications.alice);
  assert.equal(feed[0].type, "RIDE_FOUND");
  await call("POST", "/rides/match", { uid: "bob", body: JSON.stringify({ id: "r1" }) });
  assert.equal(pushes.length, 1, "not again");
  // The ride fills up: it stops matching on both sides.
  data.ads.r1.availableSeats = 0;
  await call("POST", "/rides/match", { uid: "bob", body: JSON.stringify({ id: "r1" }) });
  assert.equal(data.wantedMatches?.alice?.w1?.r1, undefined);
  assert.equal(data.rideMatches?.r1?.alice_w1, undefined);
});

test("a driver offers a seat; the rider takes it and the seat is booked", async () => {
  resetWanted();
  data.wanted = { alice: { w1: want() } };
  data.ads = { r1: ride({ availableSeats: 1 }) };
  assert.equal((await call("POST", "/offers", { uid: "alice", body: JSON.stringify({ rideId: "r1", riderUid: "alice", wantId: "w1" }) })).status, 403);
  const res = await call("POST", "/offers", { uid: "bob", body: JSON.stringify({ rideId: "r1", riderUid: "alice", wantId: "w1" }) });
  assert.equal(res.status, 200);
  const { id } = await res.json();
  assert.equal(data.offers.alice[id].status, "PENDING");
  assert.equal(data.driverOffers.bob[id].status, "PENDING");
  assert.equal(pushes.at(-1).data.type, "SEAT_OFFERED");
  assert.equal(pushes.at(-1).data.open, `offer:${id}`);
  const again = await call("POST", "/offers", { uid: "bob", body: JSON.stringify({ rideId: "r1", riderUid: "alice", wantId: "w1" }) });
  assert.equal((await again.json()).id, id, "one pending offer per rider and ride");

  assert.equal((await call("POST", "/offers/respond", { uid: "bob", body: JSON.stringify({ id, accept: true }) })).status, 404, "only the rider answers");
  const take = await call("POST", "/offers/respond", { uid: "alice", body: JSON.stringify({ id, accept: true }) });
  assert.equal(take.status, 200);
  const { requestId } = await take.json();
  assert.equal(data.ads.r1.availableSeats, 0);
  assert.equal(data.ads.r1.status, "FILLED");
  const booking = data.joinRequests[requestId];
  assert.equal(booking.status, "ACCEPTED");
  assert.equal(booking.requestorUid, "alice");
  assert.equal(booking.requestorPhone, "+923001112222");
  assert.equal(booking.adCreatorUid, "bob");
  assert.equal(data.wanted.alice.w1.status, "BOOKED");
  assert.equal(data.offers.alice[id].status, "ACCEPTED");
  assert.equal(data.driverOffers.bob[id].status, "ACCEPTED");
  assert.equal(pushes.at(-1).token, "bob-token");
  assert.equal(pushes.at(-1).data.title, "Alice took your seat");
  assert.equal((await call("POST", "/offers/respond", { uid: "alice", body: JSON.stringify({ id, accept: true }) })).status, 409);
});

test("an offer can't overbook a ride that filled in the meantime", async () => {
  resetWanted();
  data.wanted = { alice: { w1: want() } };
  data.ads = { r1: ride({ availableSeats: 1 }) };
  const { id } = await (await call("POST", "/offers", { uid: "bob", body: JSON.stringify({ rideId: "r1", riderUid: "alice", wantId: "w1" }) })).json();
  data.ads.r1.availableSeats = 0;
  assert.equal((await call("POST", "/offers/respond", { uid: "alice", body: JSON.stringify({ id, accept: true }) })).status, 409);
  assert.equal(data.joinRequests, undefined);
  assert.equal(data.offers.alice[id].status, "PENDING");
});

test("declining an offer only changes its status", async () => {
  resetWanted();
  data.wanted = { alice: { w1: want() } };
  data.ads = { r1: ride() };
  const { id } = await (await call("POST", "/offers", { uid: "bob", body: JSON.stringify({ rideId: "r1", riderUid: "alice", wantId: "w1" }) })).json();
  assert.equal((await call("POST", "/offers/respond", { uid: "alice", body: JSON.stringify({ id, accept: false }) })).status, 200);
  assert.equal(data.offers.alice[id].status, "DECLINED");
  assert.equal(data.driverOffers.bob[id].status, "DECLINED");
  assert.equal(data.ads.r1.availableSeats, 2);
  assert.equal(data.wanted.alice.w1.status, "ACTIVE");
});

test("the sweep pushes new matches once, closes past and booked wants, and expires old offers", async () => {
  resetWanted();
  const now = Date.now();
  data.ads = { r1: ride(), r2: ride({ departureTime: now + 2 * H, creatorUid: "carol" }) };
  data.wanted = {
    alice: { w1: want(), old: want({ windowStart: now - 3 * H, windowEnd: now - H }) },
    bob: { w2: want({ name: "Bob", time: now + 2 * H, windowStart: now + H, windowEnd: now + 3 * H }) },
  };
  data.joinRequests = { j1: { adId: "r2", requestorUid: "bob", status: "ACCEPTED" } };
  data.offers = { alice: { o1: { driverUid: "carol", departureTime: now - H, status: "PENDING" } } };
  const out = await sweepWanted(env, ctx, now);
  assert.equal(out.expired, 1);
  assert.equal(out.booked, 1);
  assert.equal(out.pushed, 1);
  assert.equal(out.offersExpired, 1);
  assert.equal(data.wanted.alice.old.status, "EXPIRED");
  assert.equal(data.wanted.bob.w2.status, "BOOKED");
  assert.equal(data.offers.alice.o1.status, "EXPIRED");
  assert.equal(pushes.filter((p) => p.data.type === "RIDE_FOUND").length, 1);
  await sweepWanted(env, ctx, now);
  assert.equal(pushes.filter((p) => p.data.type === "RIDE_FOUND").length, 1, "each match is pushed once");
});
