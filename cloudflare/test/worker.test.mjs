// Runs the Worker against in-memory fakes of Google auth, the Realtime Database and FCM.
// node --test test/
import { test } from "node:test";
import assert from "node:assert/strict";
import worker, { purgeExpiredRides } from "../src/index.js";

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
const at = (path) => path.split("/").filter(Boolean).reduce((node, k) => node?.[k], data);
function write(path, value) {
  const keys = path.split("/").filter(Boolean);
  if (!keys.length) { data = value ?? {}; return; }
  let node = data;
  for (const k of keys.slice(0, -1)) node = node[k] ??= {};
  if (value === null) delete node[keys.at(-1)];
  else node[keys.at(-1)] = value;
}

globalThis.caches = { default: { match: async () => undefined, put: async () => {} } };
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
  if (url.origin === DB_URL) {
    assert.equal(init.headers.authorization, "Bearer access");
    const path = decodeURIComponent(url.pathname.replace(/\.json$/, ""));
    if (method === "GET") {
      let value = at(path) ?? null;
      const orderBy = url.searchParams.get("orderBy");
      if (orderBy && value) {
        const field = JSON.parse(orderBy);
        const endAt = url.searchParams.get("endAt");
        const equalTo = url.searchParams.get("equalTo");
        value = Object.fromEntries(Object.entries(value).filter(([, v]) =>
          (endAt === null || (v[field] !== undefined && v[field] <= Number(endAt))) &&
          (equalTo === null || v[field] === JSON.parse(equalTo))));
      }
      return ok(value);
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
});
