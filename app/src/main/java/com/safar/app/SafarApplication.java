package com.safar.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

/**
 * SAFAR Application class
 * Initializes Firebase SDK on app startup
 * 
 * IMPORTANT: For Phone Auth to work, you MUST:
 * 1. Add your SHA-1 and SHA-256 fingerprints to Firebase Console
 *    - Run: ./gradlew signingReport
 *    - Copy SHA1 and SHA256 fingerprints
 *    - Go to Firebase Console > Project Settings > Your Apps > Add fingerprint
 * 2. Download fresh google-services.json
 * 3. Enable Phone Auth in Firebase Console > Authentication > Sign-in method
 */
public class SafarApplication extends Application {

    private static final String TAG = "SafarApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase - this MUST happen before any Firebase calls
        FirebaseApp.initializeApp(this);
        
        // Log Firebase initialization status
        FirebaseApp app = FirebaseApp.getInstance();
        if (app != null) {
            Log.d(TAG, "Firebase initialized successfully");
            Log.d(TAG, "Firebase App Name: " + app.getName());
            Log.d(TAG, "Project ID: " + app.getOptions().getProjectId());
            
            // Check Auth
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null) {
                Log.d(TAG, "FirebaseAuth initialized");
                if (auth.getCurrentUser() != null) {
                    Log.d(TAG, "User already signed in: " + auth.getCurrentUser().getUid());
                }
            }
        } else {
            Log.e(TAG, "Firebase initialization FAILED - check google-services.json");
        }
    }
}
