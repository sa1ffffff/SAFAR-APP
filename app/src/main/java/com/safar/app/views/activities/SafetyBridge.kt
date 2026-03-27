package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.SafetyScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SAFETY BRIDGE
// Connects Java SafetyActivity to Jetpack Compose SafetyScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In SafetyActivity.java:
//
//   1. Create state holder:
//      private SafetyStateHolder stateHolder = new SafetyStateHolder();
//
//   2. In onCreate():
//      REMOVE:  setContentView(R.layout.activity_safety)
//      ADD:     SafetyBridgeKt.setupComposeUI(this, stateHolder)
//
//   3. Read checkbox state:
//      stateHolder.isChecked()
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Safety guidelines data model.
 */
data class SafetyGuideline(
    val icon: String,
    val title: String,
    val description: String
)

/**
 * Default safety guidelines for the app.
 */
object SafetyGuidelines {
    val VERIFY_IDENTITY = SafetyGuideline(
        icon = "shield_check",
        title = "Verify Identity",
        description = "Always verify the driver's identity and vehicle details before starting your ride."
    )
    
    val SHARE_TRIP = SafetyGuideline(
        icon = "share",
        title = "Share Your Trip",
        description = "Share your trip details with trusted contacts so they can track your journey."
    )
    
    val STAY_ALERT = SafetyGuideline(
        icon = "visibility",
        title = "Stay Alert",
        description = "Stay aware of your surroundings and trust your instincts during the ride."
    )
    
    val EMERGENCY_CONTACTS = SafetyGuideline(
        icon = "emergency",
        title = "Emergency Contacts",
        description = "Keep emergency numbers handy. Safar provides quick access to emergency services."
    )
    
    val WEAR_SEATBELT = SafetyGuideline(
        icon = "airline_seat_recline_normal",
        title = "Wear Seatbelt",
        description = "Always wear your seatbelt for your safety throughout the journey."
    )
    
    val ALL = listOf(
        VERIFY_IDENTITY,
        SHARE_TRIP,
        STAY_ALERT,
        EMERGENCY_CONTACTS,
        WEAR_SEATBELT
    )
}

/**
 * State holder for safety acknowledgment screen.
 * Manages the checkbox state and acknowledgment flow.
 *
 * Java Access Pattern:
 * ```java
 * SafetyStateHolder stateHolder = new SafetyStateHolder();
 * 
 * // Reading values
 * boolean isChecked = stateHolder.isChecked();
 * boolean canProceed = stateHolder.canProceed();
 * 
 * // Setting values
 * stateHolder.setChecked(true);
 * ```
 */
class SafetyStateHolder {
    
    /** Whether the user has checked the acknowledgment checkbox */
    var isChecked by mutableStateOf(false)
    
    /** Whether the user has scrolled to the bottom of the content */
    var hasScrolledToBottom by mutableStateOf(false)
        internal set
    
    /** Whether the user can proceed (must acknowledge) */
    val canProceed: Boolean
        get() = isChecked
    
    /**
     * Marks that the user has scrolled to the bottom of the safety content.
     */
    internal fun markScrolledToBottom() {
        hasScrolledToBottom = true
    }
    
    /**
     * Resets the state (useful for re-showing the screen).
     */
    fun reset() {
        isChecked = false
        hasScrolledToBottom = false
    }
}

/**
 * Sets up the Compose UI for SafetyActivity.
 *
 * @receiver The SafetyActivity instance
 * @param stateHolder The state holder managing acknowledgment state
 */
fun SafetyActivity.setupComposeUI(stateHolder: SafetyStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            SafetyScreen(
                guidelines = SafetyGuidelines.ALL,
                isChecked = stateHolder.isChecked,
                hasScrolledToBottom = stateHolder.hasScrolledToBottom,
                onCheckedChange = { stateHolder.isChecked = it },
                onScrolledToBottom = { stateHolder.markScrolledToBottom() },
                onIUnderstand = {
                    if (stateHolder.canProceed) {
                        handleIUnderstand()
                    }
                },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }
}
