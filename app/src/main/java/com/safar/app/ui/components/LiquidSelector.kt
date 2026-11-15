package com.safar.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Apple-style "liquid" selection: one translucent lens glides between options.
 *
 * - Critically-damped-ish spring (no overshoot on taps), animated from the lens's
 *   live position, so a second tap mid-flight re-targets smoothly instead of jumping.
 * - The lens stretches along its direction of travel in proportion to its speed,
 *   and settles back to its resting shape as it arrives.
 * - The lens can be dragged 1:1 across the control; on release the momentum is
 *   projected forward to pick the option, and the release velocity carries into the spring.
 * - [content] gets each option's closeness to the lens (0..1) so labels blend
 *   continuously as it passes, rather than flipping when the tap lands.
 * - With system animations turned off, the lens moves instantly.
 */
@Composable
fun LiquidSelector(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    lens: LensStyle = LensStyle.tint(),
    roles: Role = Role.Tab,
    content: @Composable (index: Int, focus: Float, pressed: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(selected.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)

    // response ≈ 0.35s, damping ≈ 0.85: settles quickly with no visible bounce on a tap.
    val glide = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow * 1.6f)

    LaunchedEffect(selected) {
        if (dragging) return@LaunchedEffect
        if (reduceMotion) position.snapTo(selected.toFloat()) else position.animateTo(selected.toFloat(), glide)
    }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val spacingPx = with(density) { spacing.toPx() }
        val slotPx = (constraints.maxWidth - spacingPx * (count - 1)) / count
        val stridePx = slotPx + spacingPx
        val slotDp = with(density) { slotPx.toDp() }
        val slotHeight = maxHeight

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(count, stridePx) {
                    val tracker = VelocityTracker()
                    var lastIndex = selected
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            tracker.resetTracking()
                            lastIndex = position.value.roundToInt()
                            scope.launch { position.stop() }
                        },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            val next = (position.value + dx / stridePx).coerceIn(0f, (count - 1).toFloat())
                            scope.launch { position.snapTo(next) }
                            val nearest = next.roundToInt()
                            if (nearest != lastIndex) {
                                lastIndex = nearest
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x / stridePx // slots per second
                            // Project momentum forward (Apple's decay form, snappy rate) before choosing.
                            val projected = position.value + velocity * 0.12f
                            val target = projected.roundToInt().coerceIn(0, count - 1)
                            dragging = false
                            currentOnSelect(target)
                            scope.launch {
                                if (reduceMotion) position.snapTo(target.toFloat())
                                else position.animateTo(target.toFloat(), glide, initialVelocity = velocity)
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { position.animateTo(currentSelected.toFloat(), glide) }
                        },
                    )
                }
        ) {
            // The lens.
            val speed = abs(position.velocity)
            val stretch = 1f + (speed * 0.045f).coerceAtMost(0.28f)
            val squash = 1f - (speed * 0.012f).coerceAtMost(0.07f)
            Box(
                Modifier
                    .offset { IntOffset((position.value * stridePx).roundToInt(), 0) }
                    .size(width = slotDp, height = slotHeight)
                    .graphicsLayer { scaleX = stretch; scaleY = squash }
                    .then(lens.modifier())
            )
            // The options.
            for (i in 0 until count) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val focus = (1f - abs(position.value - i)).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .offset { IntOffset((i * stridePx).roundToInt(), 0) }
                        .size(width = slotDp, height = slotHeight)
                        .clickable(interaction, indication = null, role = roles) { currentOnSelect(i) },
                ) { content(i, focus, pressed) }
            }
        }
    }
}

/** How the lens is drawn. Tint for glass bars; raised for segmented tracks. */
class LensStyle private constructor(private val build: @Composable () -> Modifier) {
    @Composable fun modifier(): Modifier = build()

    companion object {
        /** Translucent tint with a light rim, for glass tab bars. */
        fun tint(radius: Dp = 26.dp) = LensStyle {
            val c = Safar.colors
            val shape = RoundedCornerShape(radius)
            val rim = Brush.verticalGradient(listOf(c.glassHighlight, Color.Transparent))
            Modifier.clip(shape).background(c.content.copy(alpha = if (c.isDark) 0.12f else 0.07f), shape).border(1.dp, rim, shape)
        }

        /** Raised surface with a soft shadow, for segmented controls on a muted track. */
        fun raised(radius: Dp) = LensStyle {
            val c = Safar.colors
            val shape = RoundedCornerShape(radius)
            Modifier.shadow(if (c.isDark) 0.dp else 2.dp, shape, ambientColor = Color.Black.copy(0.08f), spotColor = Color.Black.copy(0.10f))
                .clip(shape).background(c.surfaceRaised, shape)
        }
    }
}

/** Blends between two colors by an option's focus, so labels change continuously with the lens. */
fun lerpColor(from: Color, to: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(from, to, t)
