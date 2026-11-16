package com.safar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.ui.SafarRoot
import com.safar.app.ui.theme.DarkColors
import com.safar.app.ui.theme.LightColors
import com.safar.app.ui.theme.SafarTheme
import com.safar.app.data.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {

    /** Deep link requested by a notification tap, consumed once by the nav graph. */
    private val pendingOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpen.value = intent?.getStringExtra(EXTRA_OPEN)
        setContent {
            val mode by container.preferences.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(DarkColors.background.toArgb() and 0x00FFFFFF)
                else SystemBarStyle.light(0x00FFFFFF, LightColors.content.toArgb())
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose { }
            }
            SafarTheme(mode) {
                SafarRoot(
                    pendingOpen = pendingOpen.value,
                    onPendingOpenHandled = { pendingOpen.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN)?.let { pendingOpen.value = it }
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_NOTIFICATIONS = "notifications"
        const val OPEN_CHAT_PREFIX = "chat:"
    }
}
