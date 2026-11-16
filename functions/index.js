// Safar backend. Both functions need the Blaze plan; at ~1k users they stay inside the free tier.
const { onValueCreated } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const REGION = "asia-southeast1";
const DAY = 24 * 60 * 60 * 1000;
const EXPIRY_WINDOW = 3 * DAY; // matches Ride.EXPIRY_WINDOW_MS in the app
const KEEP_BOOKED_RIDES = 14 * DAY; // time riders get to mark a booked ride complete and review it

// Push notification for every in-app notification the app writes.
exports.onNotificationCreated = onValueCreated(
  { ref: "/notifications/{uid}/{nid}", region: REGION },
  async (event) => {
    const n = event.data.val() || {};
    const tokenSnap = await getDatabase().ref(`pushTokens/${event.params.uid}`).get();
    const token = tokenSnap.val();
    if (!token) return;
    try {
      await getMessaging().send({
        token,
        notification: { title: n.title || "Safar", body: n.message || "" },
        data: { type: String(n.type || ""), adId: String(n.relatedAdId || "") },
        android: { priority: "high", notification: { channelId: "safar_rides" } },
      });
    } catch (err) {
      if (err.code === "messaging/registration-token-not-registered") {
        await tokenSnap.ref.remove();
      }
    }
  }
);

/**
 * Daily cleanup so /ads only holds live rides (keeps every app open's download small).
 * - Expired rides nobody was accepted on are deleted.
 * - Expired rides with accepted passengers are marked COMPLETED and kept for two weeks,
 *   so both sides can still mark the ride done and review each other, then deleted.
 */
exports.purgeExpiredRides = onSchedule(
  { schedule: "every day 03:30", timeZone: "Asia/Karachi", region: REGION },
  async () => {
    const db = getDatabase();
    const now = Date.now();
    const snap = await db.ref("ads").orderByChild("expiresAt").endAt(now).get();
    const updates = {};
    let deleted = 0;
    let completed = 0;

    for (const [adId, ride] of Object.entries(snap.val() || {})) {
      const expiresAt = ride.expiresAt || (ride.createdAt || 0) + EXPIRY_WINDOW;
      if (!expiresAt || expiresAt > now) continue;

      const requests = await db.ref("joinRequests").orderByChild("adId").equalTo(adId).get();
      const booked = Object.values(requests.val() || {}).some((r) => r.status === "ACCEPTED");

      if (!booked || expiresAt + KEEP_BOOKED_RIDES <= now) {
        updates[`ads/${adId}`] = null;
        deleted++;
      } else if (ride.status !== "COMPLETED" && ride.status !== "CANCELLED") {
        updates[`ads/${adId}/status`] = "COMPLETED";
        completed++;
      }
    }

    if (Object.keys(updates).length) await db.ref().update(updates);
    logger.info(`purgeExpiredRides: deleted ${deleted}, completed ${completed}`);
  }
);
