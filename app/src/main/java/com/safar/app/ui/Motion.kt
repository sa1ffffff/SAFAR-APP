@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.safar.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Motion vocabulary. Enters decelerate (things arrive and settle), exits are short
 * and accelerate away, and shared elements move on a critically-damped spring so a
 * morph never overshoots or wobbles. System "animator duration scale" (including
 * "remove animations") applies to all of these automatically.
 */
object Motion {
    /** Strong ease-out for things arriving on screen. */
    val Enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Quick ease-in for things leaving. */
    val Exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** Symmetric curve for drawing paths and moving along them. */
    val Glide = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    const val SHORT = 160
    const val MEDIUM = 280
    const val LONG = 420
}

val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalStageScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val MorphBounds = BoundsTransform { _, _ -> spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 260f) }

/**
 * Marks an element that morphs into its counterpart (same [key]) on the next stage,
 * e.g. the sign-in screen's pickup ring growing into the user's avatar.
 * A no-op outside a shared transition, so screens still work in isolation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.morph(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val stage = LocalStageScope.current ?: return this
    return with(shared) {
        this@morph.sharedBounds(
            rememberSharedContentState(key),
            animatedVisibilityScope = stage,
            boundsTransform = MorphBounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
