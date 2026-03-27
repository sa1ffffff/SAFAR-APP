package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.safar.app.utils.SettingsRepository
import com.safar.app.utils.SettingsStateHolder
import com.safar.app.views.ui.screens.SettingsScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SETTINGS BRIDGE
// Connects Java SettingsActivity to Jetpack Compose SettingsScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In SettingsActivity.java:
//
//   1. Create settings state holder:
//      private SettingsStateHolder stateHolder;
//
//   2. In onCreate():
//      REMOVE:  setContentView(R.layout.activity_settings)
//      ADD:     SettingsBridgeKt.setupComposeUI(this)
//
//   3. Handle settings navigation:
//      - Add methods for each settings action
//      - Handle password change, account deletion, etc.
//      - Navigate to policy/terms screens
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Settings action types for navigation.
 */
enum class SettingsAction {
    CHANGE_PASSWORD,
    DELETE_ACCOUNT,
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    CONTACT_SUPPORT,
    BACK
}

/**
 * Sets up the Compose UI for SettingsActivity.
 * Uses dark theme to match app-wide Uber-inspired design.
 *
 * @receiver The SettingsActivity instance
 */
fun SettingsActivity.setupComposeUI() {
    
    // Initialize settings repository
    val settingsRepository = SettingsRepository.getInstance(this)
    val stateHolder = SettingsStateHolder(settingsRepository)
    
    setContent {
        // Use the current dark mode setting or default to dark
        SafarTheme(darkTheme = stateHolder.uiState.isDarkMode) {
            SettingsScreen(
                state = stateHolder.uiState,
                onBack = { 
                    onBackPressedDispatcher.onBackPressed()
                },
                
                // ─── Theme Settings ─────────────────────────────────────────────
                onDarkModeToggle = {
                    stateHolder.toggleDarkMode()
                    // Recreate activity to apply theme change
                    recreate()
                },
                onFollowSystemToggle = {
                    stateHolder.toggleFollowSystemTheme()
                    // Recreate activity to apply theme change  
                    recreate()
                },
                
                // ─── Notification Settings ──────────────────────────────────────
                onNotificationsToggle = {
                    stateHolder.toggleNotifications()
                    handleNotificationPermissionChange(stateHolder.uiState.notificationsEnabled)
                },
                onPushNotificationsToggle = {
                    stateHolder.togglePushNotifications()
                    handlePushNotificationToggle(stateHolder.uiState.pushNotificationsEnabled)
                },
                onEmailNotificationsToggle = {
                    stateHolder.toggleEmailNotifications()
                    handleEmailNotificationToggle(stateHolder.uiState.emailNotificationsEnabled)
                },
                
                // ─── Privacy & Security ─────────────────────────────────────────
                onLocationSharingToggle = {
                    stateHolder.toggleLocationSharing()
                    handleLocationPermissionChange(stateHolder.uiState.locationSharingEnabled)
                },
                onChangePassword = {
                    navigateToPasswordChange()
                },
                
                // ─── App Preferences ────────────────────────────────────────────
                onAutoRefreshToggle = {
                    stateHolder.toggleAutoRefresh()
                },
                onLanguageSelect = { language ->
                    stateHolder.updateLanguage(language)
                    handleLanguageChange(language)
                },
                onDistanceUnitSelect = { unit ->
                    stateHolder.updateDistanceUnit(unit)
                },
                
                // ─── Account Management ─────────────────────────────────────────
                onDeleteAccount = {
                    navigateToAccountDeletion()
                },
                
                // ─── About & Support ────────────────────────────────────────────
                onPrivacyPolicy = {
                    navigateToPrivacyPolicy()
                },
                onTermsOfService = {
                    navigateToTermsOfService()
                },
                onContactSupport = {
                    navigateToContactSupport()
                },
                
                // ─── Reset Settings ─────────────────────────────────────────────
                onResetSettings = {
                    stateHolder.showResetDialog()
                },
                onDismissResetDialog = {
                    stateHolder.dismissResetDialog()
                },
                onConfirmReset = {
                    stateHolder.resetToDefaults()
                    recreate() // Apply default theme
                }
            )
        }
    }
}

// ─── Navigation Helper Extensions ───────────────────────────────────────────

/**
 * Handle notification permission changes.
 * This should request or revoke notification permissions based on user preference.
 */
private fun SettingsActivity.handleNotificationPermissionChange(enabled: Boolean) {
    if (enabled) {
        // Request notification permissions if needed
        // Implementation depends on your notification setup
        enableNotifications()
    } else {
        // Disable notifications
        disableNotifications()
    }
}

/**
 * Handle push notification toggle.
 * This should register/unregister for FCM tokens.
 */
private fun SettingsActivity.handlePushNotificationToggle(enabled: Boolean) {
    if (enabled) {
        registerForPushNotifications()
    } else {
        unregisterFromPushNotifications()
    }
}

/**
 * Handle email notification toggle.
 * This should update the user's email notification preferences on the server.
 */
private fun SettingsActivity.handleEmailNotificationToggle(enabled: Boolean) {
    updateEmailNotificationPreference(enabled)
}

/**
 * Handle location permission changes.
 * This should request or handle location permissions.
 */
private fun SettingsActivity.handleLocationPermissionChange(enabled: Boolean) {
    if (enabled) {
        // Request location permissions if needed
        requestLocationPermission()
    } else {
        // Handle location sharing disabled
        handleLocationSharingDisabled()
    }
}

/**
 * Handle language change.
 * This should update app locale and recreate activities.
 */
private fun SettingsActivity.handleLanguageChange(languageCode: String) {
    updateAppLocale(languageCode)
    // You may need to restart the app or recreate activities
}

// ─── Activity Interface Methods (to be implemented in Java SettingsActivity) ──

/**
 * These methods should be implemented in the SettingsActivity.java class
 * to handle the specific actions triggered from the settings UI.
 * 
 * Note: These are regular method calls - the implementations exist in SettingsActivity.java
 */

// ─── State Management for Theme Changes ─────────────────────────────────────

/**
 * Helper class to manage theme state across the app.
 * This can be used by other activities to check current theme settings.
 */
object ThemeManager {
    
    fun isDarkMode(context: android.content.Context): Boolean {
        val repository = SettingsRepository.getInstance(context)
        return if (repository.followSystemTheme) {
            // Check system theme
            val uiMode = context.resources.configuration.uiMode and 
                         android.content.res.Configuration.UI_MODE_NIGHT_MASK
            uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            repository.isDarkModeEnabled
        }
    }
    
    fun shouldFollowSystem(context: android.content.Context): Boolean {
        return SettingsRepository.getInstance(context).followSystemTheme
    }
}

/**
 * Extension to apply current theme to any activity
 */
fun androidx.appcompat.app.AppCompatActivity.applyCurrentTheme() {
    val isDark = ThemeManager.isDarkMode(this)
    // Apply theme based on settings
    // This can be used in other activities to maintain consistent theming
}