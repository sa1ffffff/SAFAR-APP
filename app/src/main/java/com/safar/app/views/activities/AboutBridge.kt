package com.safar.app.views.activities

import androidx.activity.compose.setContent
import com.safar.app.views.ui.screens.AboutScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ABOUT BRIDGE
// Connects Java AboutActivity to Jetpack Compose AboutScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Sets up the Compose UI for AboutActivity.
 *
 * @receiver The AboutActivity instance
 */
fun AboutActivity.setupComposeUI() {
    setContent {
        SafarTheme(darkTheme = true) {
            AboutScreen(
                appVersion = getAppVersion(),
                onBack = { onBackPressedDispatcher.onBackPressed() },
                onGitHubClick = { openGitHub() },
                onEmailClick = { openEmail() },
                onPrivacyPolicyClick = { openPrivacyPolicy() },
                onTermsClick = { openTermsOfService() }
            )
        }
    }
}