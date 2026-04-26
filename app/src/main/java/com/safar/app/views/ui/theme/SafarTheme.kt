package com.safar.app.views.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Colors ──────────────────────────────────────────────────────────────────
val UberWhite = Color(0xFFFFFFFF)
val SafarBlack = Color(0xFF000000)
val SafarDarkSurface = Color(0xFF141414)
val SafarDarkCard = Color(0xFF1F1F1F)
val SafarDarkElevated = Color(0xFF282828)
val SafarTextPrimary = Color(0xFFFFFFFF)
val SafarTextSecondary = Color(0xFFB3B3B3)
val SafarTextTertiary = Color(0xFF757575)
val SafarBg = Color(0xFFF6F6F6)
val SafarWhite = Color(0xFFFFFFFF)
val SafarLightElevated = Color(0xFFFAFAFA)
val SafarTextDark = Color(0xFF000000)
val SafarTextMuted = Color(0xFF6B6B6B)
val SafarTextLight = Color(0xFF8F8F8F)
val SafarDividerLight = Color(0xFFE5E5E5)
val SafarDivider = Color(0xFF333333)
val SafarGreen = Color(0xFF06C167)
val SafarGreenDark = Color(0xFF048A46)
val SafarGreenLight = Color(0xFF34D399)
val SafarYellow = Color(0xFFFFD600)       // Primary yellow accent
val SafarYellowDark = Color(0xFFCCAA00)   // Darker yellow for pressed states
val SafarRed = Color(0xFFEF4444)          // Error/danger red
val SafarRedDark = Color(0xFFDC2626)      // Darker red
val SafarBlue = Color(0xFF276EF1)
val SafarBlueDark = Color(0xFF1A56C4)
val SafarBlueLight = Color(0xFF5B91F5)
val StatusOpen = Color(0xFF06C167)
val StatusFilled = Color(0xFFE74C3C)
val StatusPending = Color(0xFFF59E0B)
val SafarSuccess = Color(0xFF06C167)
val SafarWarning = Color(0xFFF59E0B)
val SafarError = Color(0xFFE74C3C)
val SafarInfo = Color(0xFF276EF1)

// ─── PROFESSIONAL UBER-INSPIRED COLOR SYSTEM ────────────────────────────────
// Dark theme optimized for professional appearance

// Core backgrounds
val UberBlack = Color(0xFF000000)        // Pure black - primary background
val UberDarkGray = Color(0xFF121212)     // Dark gray - elevated surfaces
val UberMidGray = Color(0xFF1E1E1E)      // Medium gray - cards and inputs
val UberLightGray = Color(0xFF2C2C2C)    // Light gray - subtle borders

// Professional green palette
val UberGreen = Color(0xFF00D084)        // Uber-style vibrant green - primary actions
val UberGreenDark = Color(0xFF00B36B)    // Darker green - pressed states
val UberGreenLight = Color(0xFF1AE5A1)   // Light green - highlights and success

// Text colors for dark theme
val UberTextPrimary = Color(0xFFFFFFFF)   // Pure white - primary text
val UberTextSecondary = Color(0xFFB3B3B3) // Light gray - secondary text  
val UberTextMuted = Color(0xFF808080)     // Medium gray - muted/disabled text

// Convenience aliases for common usage - UBER PROFESSIONAL THEME
val BgDark = UberBlack                    // Pure black background
val BgElevated = UberDarkGray            // Elevated surfaces
val Surface = UberMidGray                // Cards and input backgrounds
val Surface2 = UberLightGray             // Borders and subtle elements
val TextPrimary = UberTextPrimary        // Primary white text
val TextSecondary = UberTextSecondary    // Secondary gray text
val TextMuted = UberTextMuted            // Muted gray text
val Green = UberGreen                    // Primary green for actions
val GreenDark = UberGreenDark           // Dark green for pressed states
val GreenLight = UberGreenLight         // Light green for highlights

// ─── Typography ──────────────────────────────────────────────────────────────
object SafarTypography {
    val displayLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 48.sp)
    val headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp)
    val titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp)
    val labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp)
}

data class SafarThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val border: Color,
    val error: Color
)

private val DarkThemeColors = SafarThemeColors(
    background = SafarBlack,
    surface = SafarDarkSurface,
    surfaceElevated = SafarDarkCard,
    primary = SafarGreen,
    onBackground = SafarTextPrimary,
    onSurface = SafarTextPrimary,
    onSurfaceVariant = SafarTextSecondary,
    textPrimary = SafarTextPrimary,
    textSecondary = SafarTextSecondary,
    textTertiary = SafarTextTertiary,
    divider = SafarDivider,
    border = Color(0xFF404040),
    error = SafarError
)

private val LightThemeColors = SafarThemeColors(
    background = SafarBg,
    surface = SafarWhite,
    surfaceElevated = SafarLightElevated,
    primary = SafarGreen,
    onBackground = SafarTextDark,
    onSurface = SafarTextDark,
    onSurfaceVariant = SafarTextMuted,
    textPrimary = SafarTextDark,
    textSecondary = SafarTextMuted,
    textTertiary = SafarTextLight,
    divider = SafarDividerLight,
    border = SafarDividerLight,
    error = SafarError
)

private val LocalSafarColors = staticCompositionLocalOf { DarkThemeColors }
private val LocalSafarTypography = staticCompositionLocalOf { SafarTypography }

object SafarTheme {
    val colors: SafarThemeColors
        @Composable
        get() = LocalSafarColors.current
    
    val typography: SafarTypography
        @Composable
        get() = LocalSafarTypography.current
}

@Composable
fun SafarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = SafarGreen, 
            background = SafarBlack, 
            surface = SafarDarkSurface,
            onSurfaceVariant = SafarTextSecondary
        )
    } else {
        lightColorScheme(
            primary = SafarGreen, 
            background = SafarBg, 
            surface = SafarWhite,
            onSurfaceVariant = SafarTextMuted
        )
    }

    val themeColors = if (darkTheme) DarkThemeColors else LightThemeColors
    
    CompositionLocalProvider(
        LocalSafarColors provides themeColors,
        LocalSafarTypography provides SafarTypography
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * Settings-aware SafarTheme that respects user preferences.
 * Use this in activities that should follow user's theme settings.
 */
@Composable
fun SafarThemeWithSettings(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember { 
        com.safar.app.utils.SettingsRepository.getInstance(context) 
    }
    
    val darkTheme = if (settingsRepository.followSystemTheme) {
        isSystemInDarkTheme()
    } else {
        settingsRepository.isDarkModeEnabled
    }
    
    SafarTheme(darkTheme = darkTheme, content = content)
}
