package com.safar.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Safar's haptic vocabulary. Each moment has one feel, used everywhere it happens:
 *
 * - [tick]: choosing among options (tabs, segments, chips, days, stepper). Barely there.
 * - [tap]: committing to something (main buttons, switches, the Home panel snapping).
 * - [success]: an action went through (request sent, accepted, saved). A short rise into a click.
 * - [celebrate]: a milestone (ride posted, ride started, review sent). A longer rise and two beats.
 * - [error]: something went wrong. Two soft, low bumps, never harsh.
 * - [rating]: stars, each one a little stronger than the last.
 *
 * Built from the vibrator's composition primitives where the phone has them (Pixel and most
 * recent flagships), so they feel crisp rather than buzzy; older phones get the closest
 * predefined effect. Touch haptics respect the user's system setting.
 */
@android.annotation.SuppressLint("NewApi", "MissingPermission")
class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val rich: Boolean = Build.VERSION.SDK_INT >= 31 && vibrator?.hasVibrator() == true &&
        vibrator.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
        )

    fun tick() = play(
        rich = { compose { addPrimitive(PRIMITIVE_TICK, 0.5f) } },
        fallback = { predefined(VibrationEffect.EFFECT_TICK) },
    )

    fun tap() = play(
        rich = { compose { addPrimitive(PRIMITIVE_CLICK, 0.7f) } },
        fallback = { predefined(VibrationEffect.EFFECT_CLICK) },
    )

    fun success() = play(
        rich = { compose { addPrimitive(PRIMITIVE_QUICK_RISE, 0.45f); addPrimitive(PRIMITIVE_CLICK, 0.9f, 20) } },
        fallback = { predefined(VibrationEffect.EFFECT_DOUBLE_CLICK) },
    )

    fun celebrate() = play(
        rich = {
            compose {
                addPrimitive(PRIMITIVE_QUICK_RISE, 0.6f)
                addPrimitive(PRIMITIVE_CLICK, 1f, 30)
                addPrimitive(PRIMITIVE_CLICK, 0.6f, 90)
            }
        },
        fallback = { predefined(VibrationEffect.EFFECT_HEAVY_CLICK) },
    )

    fun error() = play(
        rich = { compose { addPrimitive(PRIMITIVE_LOW_TICK, 1f); addPrimitive(PRIMITIVE_LOW_TICK, 1f, 90) } },
        fallback = { predefined(VibrationEffect.EFFECT_DOUBLE_CLICK) },
    )

    /** [stars] of 5: the fifth star lands noticeably harder than the first. */
    fun rating(stars: Int) = play(
        rich = { compose { addPrimitive(PRIMITIVE_CLICK, 0.3f + 0.14f * stars.coerceIn(1, 5)) } },
        fallback = { predefined(if (stars >= 5) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK) },
    )

    private inline fun compose(block: VibrationEffect.Composition.() -> Unit): VibrationEffect =
        VibrationEffect.startComposition().apply(block).compose()

    private fun predefined(id: Int): VibrationEffect? =
        if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(id) else VibrationEffect.createOneShot(12, 120)

    private fun play(rich: () -> VibrationEffect?, fallback: () -> VibrationEffect?) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = runCatching { if (this.rich) rich() else fallback() }.getOrNull() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                v.vibrate(effect)
            }
        }
    }

    private companion object {
        val PRIMITIVE_TICK = VibrationEffect.Composition.PRIMITIVE_TICK
        val PRIMITIVE_CLICK = VibrationEffect.Composition.PRIMITIVE_CLICK
        val PRIMITIVE_QUICK_RISE = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
        val PRIMITIVE_LOW_TICK = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics?> { null }

/** The app's [Haptics]; provided at the root, created on demand in previews. */
@Composable
fun haptics(): Haptics {
    val provided = LocalHaptics.current
    val context = LocalContext.current
    return provided ?: remember(context) { Haptics(context.applicationContext) }
}
