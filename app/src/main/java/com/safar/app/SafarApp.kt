package com.safar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.safar.app.data.SafarApi
import com.safar.app.data.AuthRepository
import com.safar.app.data.ChatRepository
import com.safar.app.data.NotificationRepository
import com.safar.app.data.Preferences
import com.safar.app.data.ReportRepository
import com.safar.app.data.RequestRepository
import com.safar.app.data.ReviewRepository
import com.safar.app.data.RideRepository
import com.safar.app.data.WantedRepository
import com.safar.app.data.Session
import com.safar.app.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-rolled dependency container: one instance of each repository for the app's lifetime. */
class AppContainer(context: Context) {
    private val database = FirebaseDatabase.getInstance().apply {
        runCatching { setPersistenceEnabled(true) }
    }

    val preferences = Preferences(context)
    val auth = AuthRepository(FirebaseAuth.getInstance())
    val api = SafarApi(FirebaseAuth.getInstance())
    val users = UserRepository(database, api, context)
    val rides = RideRepository(database, api)
    val wanted = WantedRepository(database, api)
    val notifications = NotificationRepository(database, api)
    val requests = RequestRepository(database, notifications)
    val chat = ChatRepository(database, api)
    val reviews = ReviewRepository(database, notifications)
    val reports = ReportRepository(database)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val session = Session(auth, users, chat, notifications, preferences, appScope)

    /** Messages that must outlive the screen that raised them (shown by the root snackbar). */
    val messages = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
}

class SafarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installAppCheck(this) // before any Firebase call, so every request carries a token
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_RIDES, "Rides and messages", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Seat requests, ride updates and new messages" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_RIDES = "safar_rides"
    }
}

val Context.container: AppContainer get() = (applicationContext as SafarApp).container
