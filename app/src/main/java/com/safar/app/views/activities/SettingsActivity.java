package com.safar.app.views.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SAFAR SETTINGS ACTIVITY
 * 
 * Manages user settings and preferences including:
 * - Theme and display options
 * - Notification preferences  
 * - Privacy and security settings
 * - Account management
 * - App preferences
 * 
 * Uses Compose UI through SettingsBridge for modern, clean interface.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1002;
    
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize session manager
        session = new SessionManager(this);
        
        // Setup Compose UI through bridge
        SettingsBridgeKt.setupComposeUI(this);
    }

    // ─── Navigation Methods ─────────────────────────────────────────────────────

    /**
     * Navigate to password change screen.
     * Opens a dialog or new activity for password change.
     */
    public void navigateToPasswordChange() {
        // For now, show a toast. In production, open password change dialog/activity
        Toast.makeText(this, "Password change coming soon", Toast.LENGTH_SHORT).show();
        
        // TODO: Implement password change flow
        // Intent intent = new Intent(this, PasswordChangeActivity.class);
        // startActivity(intent);
    }

    /**
     * Navigate to account deletion screen with confirmation.
     */
    public void navigateToAccountDeletion() {
        // For now, show a toast. In production, open account deletion confirmation
        Toast.makeText(this, "Account deletion requires contacting support", Toast.LENGTH_LONG).show();
        
        // TODO: Implement account deletion flow with proper confirmation
        // Intent intent = new Intent(this, AccountDeletionActivity.class);
        // startActivity(intent);
    }

    /**
     * Open privacy policy in browser or dedicated activity.
     */
    public void navigateToPrivacyPolicy() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://safar.pk/privacy-policy"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open privacy policy", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open terms of service in browser or dedicated activity.
     */
    public void navigateToTermsOfService() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://safar.pk/terms-of-service"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open terms of service", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open contact support flow (email, help activity, etc.).
     */
    public void navigateToContactSupport() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    // ─── Notification Management ────────────────────────────────────────────────

    /**
     * Enable notifications by requesting necessary permissions.
     */
    public void enableNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    /**
     * Disable notifications by guiding user to system settings.
     */
    public void disableNotifications() {
        // Since we can't programmatically disable notifications, guide user to settings
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return; // Already disabled
        }
        
        Toast.makeText(this, "Notifications will be disabled for new requests", Toast.LENGTH_SHORT).show();
    }

    /**
     * Register for push notifications via FCM.
     */
    public void registerForPushNotifications() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Failed to register for push notifications", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                
                // Send token to server
                updateFCMTokenOnServer(token, true);
                
                Toast.makeText(this, "Push notifications enabled", Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * Unregister from push notifications.
     */
    public void unregisterFromPushNotifications() {
        // Remove FCM token from server
        updateFCMTokenOnServer(null, false);
        Toast.makeText(this, "Push notifications disabled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Update email notification preference on server.
     */
    public void updateEmailNotificationPreference(boolean enabled) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("emailNotifications", enabled);
        
        FirebaseManager.getInstance().getUserRef(userId)
            .updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                String message = enabled ? "Email notifications enabled" : "Email notifications disabled";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update email preferences", Toast.LENGTH_SHORT).show()
            );
    }

    // ─── Location Management ────────────────────────────────────────────────────

    /**
     * Request location permissions when location sharing is enabled.
     */
    public void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
        }
    }

    /**
     * Handle location sharing being disabled.
     */
    public void handleLocationSharingDisabled() {
        Toast.makeText(this, "Location sharing disabled. Some features may be limited.", Toast.LENGTH_LONG).show();
    }

    // ─── Locale Management ──────────────────────────────────────────────────────

    /**
     * Update app locale based on user selection.
     */
    public void updateAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        
        // Save to preferences for persistence across app restarts
        session.setUserLanguage(languageCode);
        
        Toast.makeText(this, "Language updated. Some changes require app restart.", Toast.LENGTH_LONG).show();
        
        // Optionally restart the app or recreate activities
        // recreateTaskStack();
    }

    // ─── Permission Result Handling ─────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_NOTIFICATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Location permission denied. Location sharing disabled.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // ─── Helper Methods ─────────────────────────────────────────────────────────

    /**
     * Update FCM token on server for push notification management.
     */
    private void updateFCMTokenOnServer(String token, boolean enabled) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", enabled ? token : null);
        updates.put("pushNotifications", enabled);
        
        FirebaseManager.getInstance().getUserRef(userId).updateChildren(updates);
    }

    /**
     * Recreate the entire task stack to apply locale changes.
     * This ensures all activities use the new language.
     */
    private void recreateTaskStack() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
