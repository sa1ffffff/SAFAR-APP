package com.safar.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the debug provider. On first run it logs a debug token
 * (search logcat for "DebugAppCheckProvider"); add it in Firebase console →
 * App Check → Apps → Manage debug tokens so debug builds keep working once
 * enforcement is on.
 */
fun installAppCheck(app: Application) {
    FirebaseApp.initializeApp(app)
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
