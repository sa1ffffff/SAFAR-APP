// Google auth for the Worker, using only Web Crypto: verifying Firebase ID tokens and
// minting OAuth access tokens from a service account.

const ID_TOKEN_KEYS = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const SCOPES = [
  "https://www.googleapis.com/auth/firebase.database",
  "https://www.googleapis.com/auth/firebase.messaging",
  "https://www.googleapis.com/auth/userinfo.email",
].join(" ");

const enc = new TextEncoder();
const dec = new TextDecoder();

function b64urlToBytes(s) {
  const b64 = s.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((s.length + 3) % 4);
  return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
}

function bytesToB64url(bytes) {
  let s = "";
  for (const b of new Uint8Array(bytes)) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

const b64urlJson = (obj) => bytesToB64url(enc.encode(JSON.stringify(obj)));

// ─── Firebase ID tokens ─────────────────────────────────────────────────────

let keyCache = { keys: null, until: 0 };

async function signingKeys() {
  if (keyCache.keys && Date.now() < keyCache.until) return keyCache.keys;
  const res = await fetch(ID_TOKEN_KEYS);
  if (!res.ok) throw new Error(`Couldn't load Google signing keys: ${res.status}`);
  const maxAge = Number((res.headers.get("cache-control") || "").match(/max-age=(\d+)/)?.[1] || 3600);
  const { keys } = await res.json();
  keyCache = { keys, until: Date.now() + maxAge * 1000 };
  return keys;
}

/** Returns the signed-in user's uid, or throws if the token isn't a valid one for this project. */
export async function verifyIdToken(token, projectId, now = Date.now()) {
  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("Malformed token");
  const header = JSON.parse(dec.decode(b64urlToBytes(parts[0])));
  const claims = JSON.parse(dec.decode(b64urlToBytes(parts[1])));
  if (header.alg !== "RS256") throw new Error("Unexpected algorithm");

  const jwk = (await signingKeys()).find((k) => k.kid === header.kid);
  if (!jwk) throw new Error("Unknown signing key");
  const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, b64urlToBytes(parts[2]), enc.encode(`${parts[0]}.${parts[1]}`));
  if (!ok) throw new Error("Bad signature");

  const seconds = Math.floor(now / 1000);
  if (claims.aud !== projectId) throw new Error("Wrong audience");
  if (claims.iss !== `https://securetoken.google.com/${projectId}`) throw new Error("Wrong issuer");
  if (!claims.sub || typeof claims.sub !== "string") throw new Error("No subject");
  if (claims.exp <= seconds - 60) throw new Error("Expired");
  if (claims.iat > seconds + 60) throw new Error("Issued in the future");
  return claims.sub;
}

// ─── Service account access tokens ──────────────────────────────────────────

let accessCache = { token: null, until: 0 };

export async function googleAccessToken(sa) {
  if (accessCache.token && Date.now() < accessCache.until) return accessCache.token;

  const iat = Math.floor(Date.now() / 1000);
  const unsigned = `${b64urlJson({ alg: "RS256", typ: "JWT" })}.${b64urlJson({
    iss: sa.client_email,
    scope: SCOPES,
    aud: sa.token_uri || "https://oauth2.googleapis.com/token",
    iat,
    exp: iat + 3600,
  })}`;
  const pem = sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const key = await crypto.subtle.importKey(
    "pkcs8", Uint8Array.from(atob(pem), (c) => c.charCodeAt(0)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc.encode(unsigned));

  const res = await fetch(sa.token_uri || "https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsigned}.${bytesToB64url(signature)}`,
    }),
  });
  if (!res.ok) throw new Error(`Google token exchange failed: ${res.status} ${await res.text()}`);
  const { access_token, expires_in } = await res.json();
  accessCache = { token: access_token, until: Date.now() + (expires_in - 300) * 1000 };
  return access_token;
}
