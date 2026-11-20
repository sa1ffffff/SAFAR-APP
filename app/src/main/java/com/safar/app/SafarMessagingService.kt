package com.safar.app

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Shows pushes sent by Safar's Cloudflare Worker (see /cloudflare). They arrive as data-only
 * messages: `title`, `body`, and `open` (where a tap goes: "notifications", "chat:<uid>", "ride:<id>" or "offer:<id>").
 * "Ride found" pushes also carry the driver's photo and a thinned route, drawn into the
 * expanded notification with [RouteArt].
 */
class SafarMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = container.auth.currentUid ?: return
        container.users.savePushToken(uid, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return

        val title = message.notification?.title ?: message.data["title"] ?: "Safar"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val target = message.data["open"]?.takeIf { it.isNotBlank() } ?: MainActivity.OPEN_NOTIFICATIONS
        val open = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, target)
        // One request code per target, so a chat push and a ride push don't overwrite each other's extras.
        val pending = PendingIntent.getActivity(
            this, target.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, SafarApp.CHANNEL_RIDES)
            .setSmallIcon(R.drawable.ic_stat_safar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
        message.data["photo"]?.takeIf { it.startsWith("https://") }?.let { RouteArt.roundPhoto(it) }?.let(builder::setLargeIcon)

        // A found ride shows where it goes, and can be requested straight from the shade.
        val rideId = message.data["adId"].orEmpty()
        if (message.data["type"] == "RIDE_FOUND" && rideId.isNotBlank()) {
            val route = message.data["route"]?.let { runCatching { com.safar.app.data.decodePolyline(it) }.getOrNull() }.orEmpty()
            val pickup = message.data["pLat"]?.toDoubleOrNull()?.let { lat -> message.data["pLng"]?.toDoubleOrNull()?.let { lat to it } }
            builder.setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(RouteArt.route(route, pickup)).setSummaryText(body)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
            )
            builder.addAction(0, "Request seat", openIntent("${MainActivity.OPEN_REQUEST_PREFIX}$rideId"))
            builder.addAction(0, "View ride", openIntent("${MainActivity.OPEN_RIDE_PREFIX}$rideId"))
        }
        val notification = builder.build()
        // Chat pushes from the same person replace each other instead of stacking up.
        val id = if (target.startsWith(MainActivity.OPEN_CHAT_PREFIX)) target.hashCode() else message.messageId.hashCode()
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    private fun openIntent(target: String): PendingIntent = PendingIntent.getActivity(
        this, target.hashCode(),
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, target),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
