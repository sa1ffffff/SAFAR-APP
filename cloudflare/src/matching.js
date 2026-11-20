/**
 * Matching a rider's wanted ride against a driver's ride. Pure functions, no I/O.
 *
 * Score (0..1) = 45% pickup + 30% drop-off + 25% time. Distances are measured to the
 * driver's road route when we have it, so a ride that passes the rider counts, not only
 * one that starts nearby. A pair is a match at MATCH_THRESHOLD or above.
 */

export const MATCH_THRESHOLD = 0.65;
export const WEIGHTS = { pickup: 0.45, drop: 0.3, time: 0.25 };
const FULL_SCORE_M = 300; // this close counts as door to door
const LIMIT_M = { LOCAL: 2500, CITY: 15000 }; // beyond this a stop doesn't match at all
const MIN_HALF_WINDOW = 15 * 60 * 1000;

const R = 6371000;
const rad = (d) => (d * Math.PI) / 180;

export function haversine(aLat, aLng, bLat, bLng) {
  const dLat = rad(bLat - aLat);
  const dLng = rad(bLng - aLng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(aLat)) * Math.cos(rad(bLat)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Google's encoded polyline (precision 5) to [[lat, lng], ...]. */
export function decodePolyline(encoded) {
  const out = [];
  let i = 0, lat = 0, lng = 0;
  const next = () => {
    let result = 0, shift = 0, b;
    do {
      b = encoded.charCodeAt(i++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    return result & 1 ? ~(result >> 1) : result >> 1;
  };
  while (i < encoded.length) {
    lat += next();
    lng += next();
    out.push([lat / 1e5, lng / 1e5]);
  }
  return out;
}

export function encodePolyline(points) {
  let out = "", pLat = 0, pLng = 0;
  const put = (v) => {
    v = v < 0 ? ~(v << 1) : v << 1;
    while (v >= 0x20) { out += String.fromCharCode((0x20 | (v & 0x1f)) + 63); v >>= 5; }
    out += String.fromCharCode(v + 63);
  };
  for (const [lat, lng] of points) {
    const a = Math.round(lat * 1e5), b = Math.round(lng * 1e5);
    put(a - pLat); put(b - pLng);
    pLat = a; pLng = b;
  }
  return out;
}

/** Keeps every nth point (and the last) so a route fits in a push payload. */
export function thin(points, max = 60) {
  if (points.length <= max) return points;
  const step = (points.length - 1) / (max - 1);
  return Array.from({ length: max }, (_, k) => points[Math.round(k * step)]);
}

/**
 * Closest approach of a point to a path: distance in metres and how far along the path
 * (0..1) it happens. Uses a flat projection around the point, accurate to well under 1%
 * at city scale.
 */
export function nearestOnPath(lat, lng, path) {
  const kx = Math.cos(rad(lat)) * (Math.PI / 180) * R;
  const ky = (Math.PI / 180) * R;
  const xy = ([pLat, pLng]) => [(pLng - lng) * kx, (pLat - lat) * ky];
  let best = { meters: Infinity, along: 0 };
  let total = 0;
  const lengths = [0];
  for (let i = 1; i < path.length; i++) {
    const [ax, ay] = xy(path[i - 1]);
    const [bx, by] = xy(path[i]);
    total += Math.hypot(bx - ax, by - ay);
    lengths.push(total);
  }
  for (let i = 1; i < path.length; i++) {
    const [ax, ay] = xy(path[i - 1]);
    const [bx, by] = xy(path[i]);
    const dx = bx - ax, dy = by - ay;
    const len2 = dx * dx + dy * dy;
    const t = len2 === 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / len2));
    const d = Math.hypot(ax + t * dx, ay + t * dy);
    if (d < best.meters) best = { meters: d, along: total ? (lengths[i - 1] + t * Math.sqrt(len2)) / total : 0 };
  }
  if (path.length === 1) best = { meters: haversine(lat, lng, path[0][0], path[0][1]), along: 0 };
  return best;
}

/** 1 within door-to-door range, falling linearly to 0 at the limit. */
export function distanceScore(meters, limit) {
  if (meters <= FULL_SCORE_M) return 1;
  if (meters >= limit) return 0;
  return 1 - (meters - FULL_SCORE_M) / (limit - FULL_SCORE_M);
}

/** 1 at the rider's preferred time, 0.5 at the edge of their window. */
export function timeScore(departure, want) {
  const half = Math.max(MIN_HALF_WINDOW, (want.windowEnd - want.windowStart) / 2);
  return Math.max(0, 1 - 0.5 * (Math.abs(departure - want.time) / half));
}

/**
 * Scores [ride] for [want]. Returns null when a hard rule fails (wrong time, not enough
 * seats, not open, their own ride, or a stop out of range), otherwise the score and why.
 * [path] is the ride's road route as [[lat, lng], ...], or null to use its two ends.
 */
export function score(want, ride, path = null) {
  if (!ride || ride.status !== "OPEN") return null;
  if (ride.creatorUid === want.uid) return null;
  if ((ride.availableSeats || 0) < (want.seats || 1)) return null;
  const dep = Number(ride.departureTime) || 0;
  if (dep < want.windowStart || dep > want.windowEnd) return null;

  const ends = [[ride.originLat, ride.originLng], [ride.destinationLat, ride.destinationLng]];
  const route = path && path.length > 1 ? path : ends;
  const pick = nearestOnPath(want.originLat, want.originLng, route);
  const drop = nearestOnPath(want.destinationLat, want.destinationLng, route);
  // Pickup has to come before drop-off along the way.
  if (pick.along > drop.along + 0.02) return null;

  const limit = LIMIT_M[want.kind] || LIMIT_M.LOCAL;
  const p = distanceScore(pick.meters, limit);
  const d = distanceScore(drop.meters, limit);
  if (p === 0 || d === 0) return null;
  const t = timeScore(dep, want);
  const total = WEIGHTS.pickup * p + WEIGHTS.drop * d + WEIGHTS.time * t;

  // "Passes" when the closest point is on the way rather than at the start of the route.
  const startGap = haversine(want.originLat, want.originLng, ride.originLat, ride.originLng);
  return {
    score: Math.round(total * 1000) / 1000,
    pickupM: Math.round(pick.meters),
    dropM: Math.round(drop.meters),
    passes: pick.meters + 50 < startGap,
  };
}
