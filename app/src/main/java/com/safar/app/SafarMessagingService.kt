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
 * messages: `title`, `body`, and `open` (where a tap goes: "notifications" or "chat:<uid>").
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
        val notification = NotificationCompat.Builder(this, SafarApp.CHANNEL_RIDES)
            .setSmallIcon(R.drawable.ic_stat_safar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()
        // Chat pushes from the same person replace each other instead of stacking up.
        val id = if (target.startsWith(MainActivity.OPEN_CHAT_PREFIX)) target.hashCode() else message.messageId.hashCode()
        NotificationManagerCompat.from(this).notify(id, notification)
    }
}
