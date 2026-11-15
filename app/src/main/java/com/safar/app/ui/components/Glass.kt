package com.safar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safar.app.ui.theme.Safar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * The haze source for the current screen, if its content is blurrable.
 * Screens whose backdrop is a Google Map leave this null: the map renders on its
 * own surface, so glass over it is tinted translucency without blur.
 */
val LocalHaze = compositionLocalOf<HazeState?> { null }

@Composable
fun glassStyle(): HazeStyle {
    val c = Safar.colors
    return HazeStyle(
        backgroundColor = c.background,
        tint = HazeTint(c.glassTint),
        blurRadius = 28.dp,
        noiseFactor = 0.08f,
        fallbackTint = HazeTint(c.glassTint.copy(alpha = if (c.isDark) 0.94f else 0.96f)),
    )
}

/**
 * Translucent "liquid glass" surface: backdrop blur + tint, a hairline light edge
 * that's brighter at the top (light catching the rim), and a soft drop shadow.
 */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    elevation: Dp = 12.dp,
    haze: HazeState? = LocalHaze.current,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Safar.colors
    val rim = Brush.verticalGradient(listOf(c.glassHighlight, c.glassHighlight.copy(alpha = c.glassHighlight.alpha * 0.25f)))
    val surface = if (haze != null) {
        Modifier.hazeChild(haze, shape, glassStyle())
    } else {
        Modifier.background(c.glassTint.copy(alpha = if (c.isDark) 0.80f else 0.86f), shape)
    }
    Box(
        modifier
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(shape)
            .then(surface)
            .border(1.dp, rim, shape),
        content = content,
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
) {
    val c = Safar.colors
    Box(modifier) {
        Glass {
            IconCircle(icon, contentDescription, onClick, background = Color.Transparent)
        }
        if (badge) {
            Box(
                Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(top = 9.dp, end = 10.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(c.info)
                    .border(1.5.dp, c.surface, CircleShape)
            )
        }
    }
}
