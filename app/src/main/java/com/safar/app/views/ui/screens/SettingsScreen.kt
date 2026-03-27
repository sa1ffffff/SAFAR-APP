package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safar.app.utils.SettingsUiState
import com.safar.app.views.ui.theme.*

/**
 * SAFAR SETTINGS SCREEN
 * 
 * Minimalist settings UI with Uber-inspired design principles:
 * - Clean category organization
 * - Professional dark theme
 * - Precise typography and spacing
 * - Intuitive toggle switches and options
 * 
 * Categories:
 * - Theme & Display
 * - Notifications
 * - Privacy & Security  
 * - App Preferences
 * - Account
 * - About & Support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState = SettingsUiState(),
    onBack: () -> Unit = {},
    onDarkModeToggle: () -> Unit = {},
    onFollowSystemToggle: () -> Unit = {},
    onNotificationsToggle: () -> Unit = {},
    onPushNotificationsToggle: () -> Unit = {},
    onEmailNotificationsToggle: () -> Unit = {},
    onAutoRefreshToggle: () -> Unit = {},
    onLocationSharingToggle: () -> Unit = {},
    onLanguageSelect: (String) -> Unit = {},
    onDistanceUnitSelect: (String) -> Unit = {},
    onChangePassword: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsOfService: () -> Unit = {},
    onContactSupport: () -> Unit = {},
    onResetSettings: () -> Unit = {},
    onDismissResetDialog: () -> Unit = {},
    onConfirmReset: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UberTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = UberTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UberBlack
                )
            )
        },
        containerColor = UberBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            
            // ─── Theme & Display ─────────────────────────────────────────────────
            SettingsCategoryHeader("Theme & Display")
            
            SettingsToggleItem(
                icon = Icons.Rounded.DarkMode,
                title = "Dark Mode", 
                subtitle = "Enable dark theme for better viewing",
                checked = state.isDarkMode,
                onToggle = onDarkModeToggle,
                enabled = !state.followSystemTheme
            )
            
            SettingsToggleItem(
                icon = Icons.Rounded.PhoneAndroid,
                title = "Follow System Theme",
                subtitle = "Automatically match device theme",
                checked = state.followSystemTheme,
                onToggle = onFollowSystemToggle
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ─── Notifications ───────────────────────────────────────────────────
            SettingsCategoryHeader("Notifications")
            
            SettingsToggleItem(
                icon = Icons.Rounded.Notifications,
                title = "All Notifications",
                subtitle = "Enable notifications for rides and messages", 
                checked = state.notificationsEnabled,
                onToggle = onNotificationsToggle
            )
            
            AnimatedVisibility(
                visible = state.notificationsEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    SettingsToggleItem(
                        icon = Icons.Rounded.PhoneAndroid,
                        title = "Push Notifications",
                        subtitle = "Real-time notifications on this device",
                        checked = state.pushNotificationsEnabled,
                        onToggle = onPushNotificationsToggle,
                        isSubItem = true
                    )
                    
                    SettingsToggleItem(
                        icon = Icons.Rounded.Email,
                        title = "Email Notifications", 
                        subtitle = "Ride updates via email",
                        checked = state.emailNotificationsEnabled,
                        onToggle = onEmailNotificationsToggle,
                        isSubItem = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ─── Privacy & Security ──────────────────────────────────────────────
            SettingsCategoryHeader("Privacy & Security")
            
            SettingsToggleItem(
                icon = Icons.Rounded.LocationOn,
                title = "Location Sharing",
                subtitle = "Share location for better ride matching",
                checked = state.locationSharingEnabled,
                onToggle = onLocationSharingToggle
            )
            
            SettingsActionItem(
                icon = Icons.Rounded.Lock,
                title = "Change Password",
                subtitle = "Update your account password",
                onClick = onChangePassword
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ─── App Preferences ─────────────────────────────────────────────────
            SettingsCategoryHeader("App Preferences")
            
            SettingsToggleItem(
                icon = Icons.Rounded.Refresh,
                title = "Auto Refresh",
                subtitle = "Automatically refresh ride listings",
                checked = state.autoRefreshEnabled,
                onToggle = onAutoRefreshToggle
            )
            
            SettingsSelectionItem(
                icon = Icons.Rounded.Language,
                title = "Language",
                subtitle = "App display language",
                currentValue = getLanguageDisplayName(state.selectedLanguage),
                options = mapOf(
                    "en" to "English",
                    "ur" to "اردو",
                    "ar" to "العربية"
                ),
                onSelect = onLanguageSelect
            )
            
            SettingsSelectionItem(
                icon = Icons.Rounded.Straighten,
                title = "Distance Unit",
                subtitle = "Preferred distance measurement",
                currentValue = state.preferredDistanceUnit.uppercase(),
                options = mapOf(
                    "km" to "KM",
                    "mi" to "Miles"
                ),
                onSelect = onDistanceUnitSelect
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ─── Account ─────────────────────────────────────────────────────────
            SettingsCategoryHeader("Account")
            
            SettingsActionItem(
                icon = Icons.Rounded.DeleteOutline,
                title = "Delete Account",
                subtitle = "Permanently delete your SAFAR account",
                onClick = onDeleteAccount,
                showWarning = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ─── About & Support ─────────────────────────────────────────────────
            SettingsCategoryHeader("About & Support")
            
            SettingsActionItem(
                icon = Icons.Rounded.Policy,
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = onPrivacyPolicy
            )
            
            SettingsActionItem(
                icon = Icons.Rounded.Description,
                title = "Terms of Service", 
                subtitle = "View terms and conditions",
                onClick = onTermsOfService
            )
            
            SettingsActionItem(
                icon = Icons.Rounded.Support,
                title = "Contact Support",
                subtitle = "Get help with your account",
                onClick = onContactSupport
            )
            
            SettingsActionItem(
                icon = Icons.Rounded.Restore,
                title = "Reset Settings",
                subtitle = "Restore default settings",
                onClick = onResetSettings
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ─── App Version ─────────────────────────────────────────────────────
            Text(
                text = "SAFAR v1.0.0",
                fontSize = 12.sp,
                color = UberTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
    
    // Reset Settings Dialog
    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = onDismissResetDialog,
            title = {
                Text(
                    text = "Reset Settings?",
                    fontWeight = FontWeight.SemiBold,
                    color = UberTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will restore all settings to their default values. This action cannot be undone.",
                    color = UberTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmReset,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SafarError
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissResetDialog,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = UberTextSecondary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = UberMidGray
        )
    }
}

// ─── Settings Components ────────────────────────────────────────────────────

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = UberTextMuted,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    isSubItem: Boolean = false
) {
    val startPadding = if (isSubItem) 40.dp else 16.dp
    
    Surface(
        onClick = { if (enabled) onToggle() },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = startPadding,
                    end = 16.dp, 
                    top = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) UberGreen else UberTextMuted,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) UberTextPrimary else UberTextMuted
                )
                
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onToggle() },
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = UberGreen,
                    checkedTrackColor = UberGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = UberTextMuted,
                    uncheckedTrackColor = UberTextMuted.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable 
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showWarning: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (showWarning) SafarError else UberGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (showWarning) SafarError else UberTextPrimary
                )
                
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = UberTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSelectionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    currentValue: String,
    options: Map<String, String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = true },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically  
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = UberGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = UberTextPrimary
                )
                
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Text(
                text = currentValue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = UberGreen
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = UberTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    
    // Selection dropdown
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(UberMidGray)
    ) {
        options.forEach { (key, displayName) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = displayName,
                        color = UberTextPrimary
                    )
                },
                onClick = {
                    onSelect(key)
                    expanded = false
                },
                trailingIcon = if (key == currentValue.lowercase()) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = UberGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

private fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode) {
        "en" -> "English"
        "ur" -> "اردو"
        "ar" -> "العربية"
        else -> "English"
    }
}