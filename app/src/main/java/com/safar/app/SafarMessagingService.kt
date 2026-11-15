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
 * Shows pushes sent by the `onNotificationCreated` Cloud Function (see /functions).
 * Without that function deployed, the app still shows notifications in-app.
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
        val open = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_NOTIFICATIONS)
        val pending = PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), notification)
    }
}
