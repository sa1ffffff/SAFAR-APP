package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.SplashScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPLASH BRIDGE
// Connects Java SplashActivity to Jetpack Compose SplashScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In SplashActivity.java → onCreate():
//
//   REMOVE:  setContentView(R.layout.activity_splash)
//   ADD:     SplashBridgeKt.setupComposeUI(this)
//
// The existing Handler delay + routing logic stays 100% unchanged.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * State holder for Splash screen animations and transitions.
 * Provides observable state for smooth entrance animations.
 */
class SplashStateHolder {
    var isAnimationComplete by mutableStateOf(false)
        internal set
    
    var logoVisible by mutableStateOf(false)
        internal set
    
    var taglineVisible by mutableStateOf(false)
        internal set
    
    /**
     * Triggers the entrance animation sequence.
     * Called automatically when the composable enters composition.
     */
    internal fun startAnimation() {
        logoVisible = true
    }
    
    /**
     * Marks the animation as complete.
     * Can be used to trigger navigation after animation finishes.
     */
    internal fun completeAnimation() {
        taglineVisible = true
        isAnimationComplete = true
    }
}

/**
 * Sets up the Compose UI for SplashActivity.
 * 
 * This function replaces the XML layout with a modern Compose implementation
 * featuring smooth animations and the Safar brand identity.
 *
 * @receiver The SplashActivity instance
 */
fun SplashActivity.setupComposeUI() {
    setContent {
        SafarTheme(darkTheme = true) {
            SplashScreen()
        }
    }
}

/**
 * Sets up the Compose UI with animation state control.
 * Use this variant when you need to coordinate splash animation with navigation.
 *
 * @receiver The SplashActivity instance
 * @param stateHolder The state holder for managing animation states
 * @param onAnimationComplete Callback invoked when entrance animation completes
 */
fun SplashActivity.setupComposeUI(
    stateHolder: SplashStateHolder,
    onAnimationComplete: () -> Unit = {}
) {
    setContent {
        SafarTheme(darkTheme = true) {
            // Track animation completion
            LaunchedEffect(stateHolder.isAnimationComplete) {
                if (stateHolder.isAnimationComplete) {
                    onAnimationComplete()
                }
            }
            
            SplashScreen()
        }
    }
}
