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
 * Monochrome system in the spirit of Uber's Base: black and white carry the
 * interface, greys build hierarchy, and color appears only when it means something
 * (positive, negative, info). No brand-colored chrome.
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
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3F3F3),
    surfaceRaised = Color(0xFFFFFFFF),
    border = Color(0xFFE8E8E8),
    content = Color(0xFF000000),
    contentSecondary = Color(0xFF5E5E5E),
    contentTertiary = Color(0xFF8A8A8A),
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    positive = Color(0xFF05A357),
    positiveMuted = Color(0xFFE6F6EE),
    negative = Color(0xFFE11900),
    negativeMuted = Color(0xFFFDE9E7),
    warning = Color(0xFF9F6402),
    warningMuted = Color(0xFFFFF4DE),
    info = Color(0xFF276EF1),
    glassTint = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    glassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.65f),
    scrim = Color(0xFF000000).copy(alpha = 0.32f),
    isDark = false,
)

val DarkColors = SafarColors(
    background = Color(0xFF000000),
    surface = Color(0xFF141414),
    surfaceMuted = Color(0xFF1F1F1F),
    surfaceRaised = Color(0xFF1C1C1E),
    border = Color(0xFF2B2B2B),
    content = Color(0xFFFFFFFF),
    contentSecondary = Color(0xFFA6A6A6),
    contentTertiary = Color(0xFF737373),
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    positive = Color(0xFF3CCB7F),
    positiveMuted = Color(0xFF0F2A1C),
    negative = Color(0xFFFF5A4E),
    negativeMuted = Color(0xFF2E1210),
    warning = Color(0xFFFFC043),
    warningMuted = Color(0xFF2C2210),
    info = Color(0xFF5B91F5),
    glassTint = Color(0xFF1C1C1E).copy(alpha = 0.62f),
    glassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.10f),
    scrim = Color(0xFF000000).copy(alpha = 0.55f),
    isDark = true,
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
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
    fontFamily = Inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp,
)

val SafarTypography = SafarType(
    display = style(30, 36, FontWeight.Bold, -0.6f),
    title = style(22, 28, FontWeight.Bold, -0.3f),
    headline = style(17, 22, FontWeight.SemiBold, -0.1f),
    body = style(15, 21, FontWeight.Normal),
    bodyStrong = style(15, 21, FontWeight.SemiBold),
    label = style(13, 18, FontWeight.Medium),
    caption = style(12, 16, FontWeight.Medium),
    overline = style(11, 14, FontWeight.SemiBold, 0.6f),
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
