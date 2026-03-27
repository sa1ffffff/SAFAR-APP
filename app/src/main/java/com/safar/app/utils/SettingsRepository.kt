package com.safar.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * SAFAR SETTINGS REPOSITORY
 * 
 * Manages user preferences and settings using SharedPreferences.
 * Provides a clean interface for theme settings, app preferences, and account settings.
 */
class SettingsRepository(private val context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // ─── Theme Settings ──────────────────────────────────────────────────────────
    
    var isDarkModeEnabled: Boolean
        get() = preferences.getBoolean(KEY_DARK_MODE, true) // Default to dark mode
        set(value) = preferences.edit().putBoolean(KEY_DARK_MODE, value).apply()
    
    var followSystemTheme: Boolean
        get() = preferences.getBoolean(KEY_FOLLOW_SYSTEM, false)
        set(value) = preferences.edit().putBoolean(KEY_FOLLOW_SYSTEM, value).apply()
    
    // ─── App Preferences ─────────────────────────────────────────────────────────
    
    var notificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = preferences.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
    
    var pushNotificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_PUSH_NOTIFICATIONS, true)
        set(value) = preferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, value).apply()
    
    var emailNotificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_EMAIL_NOTIFICATIONS, true)
        set(value) = preferences.edit().putBoolean(KEY_EMAIL_NOTIFICATIONS, value).apply()
    
    var autoRefreshEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_REFRESH, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_REFRESH, value).apply()
    
    var selectedLanguage: String
        get() = preferences.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = preferences.edit().putString(KEY_LANGUAGE, value).apply()
    
    // ─── Privacy Settings ────────────────────────────────────────────────────────
    
    var locationSharingEnabled: Boolean
        get() = preferences.getBoolean(KEY_LOCATION_SHARING, true)
        set(value) = preferences.edit().putBoolean(KEY_LOCATION_SHARING, value).apply()
    
    var profileVisibility: String
        get() = preferences.getString(KEY_PROFILE_VISIBILITY, "public") ?: "public"
        set(value) = preferences.edit().putString(KEY_PROFILE_VISIBILITY, value).apply()
    
    // ─── User Preferences ────────────────────────────────────────────────────────
    
    var preferredDistanceUnit: String
        get() = preferences.getString(KEY_DISTANCE_UNIT, "km") ?: "km"
        set(value) = preferences.edit().putString(KEY_DISTANCE_UNIT, value).apply()
    
    var defaultSortOrder: String
        get() = preferences.getString(KEY_DEFAULT_SORT, "nearest") ?: "nearest"
        set(value) = preferences.edit().putString(KEY_DEFAULT_SORT, value).apply()
    
    // ─── Account Settings ────────────────────────────────────────────────────────
    
    var autoLogoutMinutes: Int
        get() = preferences.getInt(KEY_AUTO_LOGOUT, 0) // 0 = disabled
        set(value) = preferences.edit().putInt(KEY_AUTO_LOGOUT, value).apply()
    
    var biometricAuthEnabled: Boolean
        get() = preferences.getBoolean(KEY_BIOMETRIC_AUTH, false)
        set(value) = preferences.edit().putBoolean(KEY_BIOMETRIC_AUTH, value).apply()
    
    // ─── Helper Methods ──────────────────────────────────────────────────────────
    
    fun clearAllSettings() {
        preferences.edit().clear().apply()
    }
    
    fun resetToDefaults() {
        preferences.edit().apply {
            putBoolean(KEY_DARK_MODE, true)
            putBoolean(KEY_FOLLOW_SYSTEM, false)
            putBoolean(KEY_NOTIFICATIONS, true)
            putBoolean(KEY_PUSH_NOTIFICATIONS, true)
            putBoolean(KEY_EMAIL_NOTIFICATIONS, true)
            putBoolean(KEY_AUTO_REFRESH, true)
            putString(KEY_LANGUAGE, "en")
            putBoolean(KEY_LOCATION_SHARING, true)
            putString(KEY_PROFILE_VISIBILITY, "public")
            putString(KEY_DISTANCE_UNIT, "km")
            putString(KEY_DEFAULT_SORT, "nearest")
            putInt(KEY_AUTO_LOGOUT, 0)
            putBoolean(KEY_BIOMETRIC_AUTH, false)
        }.apply()
    }
    
    companion object {
        private const val PREFS_NAME = "safar_settings"
        
        // Theme Settings
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FOLLOW_SYSTEM = "follow_system_theme"
        
        // App Preferences
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_PUSH_NOTIFICATIONS = "push_notifications"
        private const val KEY_EMAIL_NOTIFICATIONS = "email_notifications"
        private const val KEY_AUTO_REFRESH = "auto_refresh"
        private const val KEY_LANGUAGE = "selected_language"
        
        // Privacy Settings
        private const val KEY_LOCATION_SHARING = "location_sharing"
        private const val KEY_PROFILE_VISIBILITY = "profile_visibility"
        
        // User Preferences
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_DEFAULT_SORT = "default_sort_order"
        
        // Account Settings
        private const val KEY_AUTO_LOGOUT = "auto_logout_minutes"
        private const val KEY_BIOMETRIC_AUTH = "biometric_auth"
        
        @Volatile
        private var INSTANCE: SettingsRepository? = null
        
        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Settings UI state data class for Compose.
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val followSystemTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = true,
    val autoRefreshEnabled: Boolean = true,
    val selectedLanguage: String = "en",
    val locationSharingEnabled: Boolean = true,
    val profileVisibility: String = "public",
    val preferredDistanceUnit: String = "km",
    val defaultSortOrder: String = "nearest",
    val autoLogoutMinutes: Int = 0,
    val biometricAuthEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val showResetDialog: Boolean = false
)

/**
 * State holder for settings screen.
 * Manages settings data and UI interactions.
 */
class SettingsStateHolder(private val repository: SettingsRepository) {
    
    var uiState by mutableStateOf(SettingsUiState())
        private set
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        uiState = uiState.copy(
            isDarkMode = repository.isDarkModeEnabled,
            followSystemTheme = repository.followSystemTheme,
            notificationsEnabled = repository.notificationsEnabled,
            pushNotificationsEnabled = repository.pushNotificationsEnabled,
            emailNotificationsEnabled = repository.emailNotificationsEnabled,
            autoRefreshEnabled = repository.autoRefreshEnabled,
            selectedLanguage = repository.selectedLanguage,
            locationSharingEnabled = repository.locationSharingEnabled,
            profileVisibility = repository.profileVisibility,
            preferredDistanceUnit = repository.preferredDistanceUnit,
            defaultSortOrder = repository.defaultSortOrder,
            autoLogoutMinutes = repository.autoLogoutMinutes,
            biometricAuthEnabled = repository.biometricAuthEnabled,
            isLoading = false
        )
    }
    
    // ─── Theme Settings ──────────────────────────────────────────────────────────
    
    fun toggleDarkMode() {
        val newValue = !uiState.isDarkMode
        repository.isDarkModeEnabled = newValue
        uiState = uiState.copy(isDarkMode = newValue)
    }
    
    fun toggleFollowSystemTheme() {
        val newValue = !uiState.followSystemTheme
        repository.followSystemTheme = newValue
        uiState = uiState.copy(followSystemTheme = newValue)
    }
    
    // ─── Notification Settings ──────────────────────────────────────────────────
    
    fun toggleNotifications() {
        val newValue = !uiState.notificationsEnabled
        repository.notificationsEnabled = newValue
        uiState = uiState.copy(notificationsEnabled = newValue)
        
        // Disable sub-notifications if main notifications are disabled
        if (!newValue) {
            repository.pushNotificationsEnabled = false
            repository.emailNotificationsEnabled = false
            uiState = uiState.copy(
                pushNotificationsEnabled = false,
                emailNotificationsEnabled = false
            )
        }
    }
    
    fun togglePushNotifications() {
        val newValue = !uiState.pushNotificationsEnabled
        repository.pushNotificationsEnabled = newValue
        uiState = uiState.copy(pushNotificationsEnabled = newValue)
    }
    
    fun toggleEmailNotifications() {
        val newValue = !uiState.emailNotificationsEnabled
        repository.emailNotificationsEnabled = newValue
        uiState = uiState.copy(emailNotificationsEnabled = newValue)
    }
    
    // ─── Other Settings ──────────────────────────────────────────────────────────
    
    fun toggleAutoRefresh() {
        val newValue = !uiState.autoRefreshEnabled
        repository.autoRefreshEnabled = newValue
        uiState = uiState.copy(autoRefreshEnabled = newValue)
    }
    
    fun updateLanguage(language: String) {
        repository.selectedLanguage = language
        uiState = uiState.copy(selectedLanguage = language)
    }
    
    fun toggleLocationSharing() {
        val newValue = !uiState.locationSharingEnabled
        repository.locationSharingEnabled = newValue
        uiState = uiState.copy(locationSharingEnabled = newValue)
    }
    
    fun updateDistanceUnit(unit: String) {
        repository.preferredDistanceUnit = unit
        uiState = uiState.copy(preferredDistanceUnit = unit)
    }
    
    // ─── Reset Dialog ────────────────────────────────────────────────────────────
    
    fun showResetDialog() {
        uiState = uiState.copy(showResetDialog = true)
    }
    
    fun dismissResetDialog() {
        uiState = uiState.copy(showResetDialog = false)
    }
    
    fun resetToDefaults() {
        repository.resetToDefaults()
        loadSettings()
        dismissResetDialog()
    }
}