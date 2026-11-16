package com.safar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.R
import com.safar.app.data.ThemeMode

/*
 * Slate & Bone. One tonal family: bone (#E3DCCB), slate (#4A5868), deep slate
 * (#263038) and ink (#1B1E26). Light mode sets ink on warm bone paper; dark mode
 * sets bone on ink. The accent is contrast, never saturation, and appears only on
 * each screen's main action, the user's selection and the route. Status colours
 * are muted so they sit inside the palette instead of shouting over it.
 */
@Immutable
data class SafarColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceRaised: Color,
    val border: Color,
    val content: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val positive: Color,
    val positiveMuted: Color,
    val negative: Color,
    val negativeMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val info: Color,
    val glassTint: Color,
    val glassHighlight: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightColors = SafarColors(
    background = Color(0xFFF3F0E8),
    surface = Color(0xFFFBFAF6),
    surfaceMuted = Color(0xFFECE7DB),
    surfaceRaised = Color(0xFFFBFAF6),
    border = Color(0xFFE0DACB),
    content = Color(0xFF1B1E26),
    contentSecondary = Color(0xFF5A6170),
    contentTertiary = Color(0xFF949AA4),
    primary = Color(0xFF263038),
    onPrimary = Color(0xFFF3F0E8),
    positive = Color(0xFF3F7A62),
    positiveMuted = Color(0xFFE2EAE2),
    negative = Color(0xFFA9483D),
    negativeMuted = Color(0xFFF3E1DC),
    warning = Color(0xFF8E6420),
    warningMuted = Color(0xFFF2E8D3),
    info = Color(0xFF4A5868),
    glassTint = Color(0xFFFBFAF6).copy(alpha = 0.78f),
    glassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.60f),
    scrim = Color(0xFF1B1E26).copy(alpha = 0.32f),
    isDark = false,
)

val DarkColors = SafarColors(
    background = Color(0xFF15181F),
    surface = Color(0xFF1B1E26),
    surfaceMuted = Color(0xFF232831),
    surfaceRaised = Color(0xFF232831),
    border = Color(0xFF2C333D),
    content = Color(0xFFECE6D8),
    contentSecondary = Color(0xFF9AA3AE),
    contentTertiary = Color(0xFF6A7380),
    primary = Color(0xFFE3DCCB),
    onPrimary = Color(0xFF1B1E26),
    positive = Color(0xFF8DBFA5),
    positiveMuted = Color(0xFF1E2E29),
    negative = Color(0xFFE08D80),
    negativeMuted = Color(0xFF34211F),
    warning = Color(0xFFD8B46E),
    warningMuted = Color(0xFF2F2919),
    info = Color(0xFF9FB0C2),
    glassTint = Color(0xFF232831).copy(alpha = 0.70f),
    glassHighlight = Color(0xFFE3DCCB).copy(alpha = 0.08f),
    scrim = Color(0xFF0B0D12).copy(alpha = 0.60f),
    isDark = true,
)

/**
 * Instrument Sans (SIL OFL 1.1; licence in /third_party/fonts). Refined and slightly
 * narrow, with an editorial calm that suits Slate & Bone. Safar tops out at SemiBold:
 * a Bold request also gets SemiBold, so nothing on screen shouts.
 */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Light),
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_sans_semibold, FontWeight.Bold),
)

/** Type ramp. Screens use at most four of these. */
@Immutable
data class SafarType(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Float = 0f) = TextStyle(
    fontFamily = InstrumentSans, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp,
)

val SafarTypography = SafarType(
    display = style(30, 36, FontWeight.SemiBold, -0.5f),
    title = style(22, 28, FontWeight.SemiBold, -0.3f),
    headline = style(17, 22, FontWeight.Medium, -0.1f),
    body = style(15, 21, FontWeight.Normal),
    bodyStrong = style(15, 21, FontWeight.Medium),
    label = style(13, 18, FontWeight.Medium),
    caption = style(12, 16, FontWeight.Medium),
    overline = style(11, 14, FontWeight.Medium, 0.9f),
)

object Radii {
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 28.dp
}

private val LocalColors = staticCompositionLocalOf { LightColors }
private val LocalType = staticCompositionLocalOf { SafarTypography }

object Safar {
    val colors: SafarColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: SafarType
        @Composable @ReadOnlyComposable get() = LocalType.current
}

@Composable
fun SafarTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    // Material components (dialogs, pickers, text selection) read from this scheme.
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.content, onSecondary = colors.background,
            background = colors.background, onBackground = colors.content,
            surface = colors.surface, onSurface = colors.content,
            surfaceVariant = colors.surfaceMuted, onSurfaceVariant = colors.contentSecondary,
            surfaceContainerHigh = colors.surfaceRaised, surfaceContainerHighest = colors.surfaceMuted,
            outline = colors.border, outlineVariant = colors.border,
            error = colors.negative,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.content, onSecondary = colors.background,
            background = colors.background, onBackground = colors.content,
            surface = colors.surface, onSurface = colors.content,
            surfaceVariant = colors.surfaceMuted, onSurfaceVariant = colors.contentSecondary,
            surfaceContainerHigh = colors.surfaceRaised, surfaceContainerHighest = colors.surfaceMuted,
            outline = colors.border, outlineVariant = colors.border,
            error = colors.negative,
        )
    }
    val type = SafarTypography
    CompositionLocalProvider(LocalColors provides colors, LocalType provides type) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                displaySmall = type.display, titleLarge = type.title, titleMedium = type.headline,
                bodyLarge = type.body, bodyMedium = type.body, labelLarge = type.bodyStrong,
                labelMedium = type.label, labelSmall = type.caption,
            ),
            shapes = Shapes(
                small = RoundedCornerShape(Radii.sm), medium = RoundedCornerShape(Radii.md),
                large = RoundedCornerShape(Radii.lg), extraLarge = RoundedCornerShape(Radii.xl),
            ),
            content = content,
        )
    }
}
