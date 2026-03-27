package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.PhoneAuthScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PHONE AUTH BRIDGE
// Connects Java PhoneAuthActivity to Jetpack Compose PhoneAuthScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In PhoneAuthActivity.java:
//
//   1. Create state holder in class:
//      private PhoneAuthStateHolder stateHolder = new PhoneAuthStateHolder();
//
//   2. In onCreate():
//      REMOVE:  setContentView(R.layout.activity_phone_auth)
//      ADD:     PhoneAuthBridgeKt.setupComposeUI(this, stateHolder)
//
//   3. Access phone value via:
//      stateHolder.getPhone()
//      stateHolder.getCountryCode()
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Country code data class for phone number input.
 */
data class CountryCode(
    val code: String,
    val country: String,
    val flag: String = ""
) {
    companion object {
        val PAKISTAN = CountryCode("+92", "Pakistan", "🇵🇰")
        val USA = CountryCode("+1", "USA", "🇺🇸")
        val UK = CountryCode("+44", "UK", "🇬🇧")
        val UAE = CountryCode("+971", "UAE", "🇦🇪")
        val SAUDI = CountryCode("+966", "Saudi Arabia", "🇸🇦")
        val INDIA = CountryCode("+91", "India", "🇮🇳")
        
        val DEFAULT = PAKISTAN
        
        val ALL = listOf(PAKISTAN, USA, UK, UAE, SAUDI, INDIA)
    }
}

/**
 * State holder for phone authentication screen.
 * Manages phone number input state with country code selection.
 *
 * Java Access Pattern:
 * ```java
 * PhoneAuthStateHolder stateHolder = new PhoneAuthStateHolder();
 * 
 * // Reading values
 * String phone = stateHolder.getPhone();
 * String countryCode = stateHolder.getCountryCode();
 * boolean isValid = stateHolder.isPhoneValid();
 * boolean isLoading = stateHolder.isLoading();
 * 
 * // Setting values
 * stateHolder.setPhone("3001234567");
 * stateHolder.setCountryCode("+92");
 * stateHolder.setLoading(true);
 * ```
 */
class PhoneAuthStateHolder {
    
    /** The phone number without country code */
    var phone by mutableStateOf("")
    
    /** The selected country code (e.g., "+92") */
    var countryCode by mutableStateOf(CountryCode.DEFAULT.code)
    
    /** Loading state for API calls */
    var isLoading by mutableStateOf(false)
    
    /** Error message to display, null if no error */
    var errorMessage by mutableStateOf<String?>(null)
    
    /** Whether the country code picker is expanded */
    var isCountryPickerExpanded by mutableStateOf(false)
        internal set
    
    /**
     * Validates the phone number format.
     * Returns true if the phone number appears valid.
     */
    val isPhoneValid: Boolean
        get() = phone.length >= 9 && phone.all { it.isDigit() }
    
    /**
     * Returns the full phone number with country code.
     */
    val fullPhoneNumber: String
        get() = "$countryCode${phone.trim()}"
    
    /**
     * Clears any displayed error message.
     */
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * Sets an error message to display.
     */
    fun setError(message: String) {
        errorMessage = message
    }
    
    /**
     * Toggles the country code picker visibility.
     */
    internal fun toggleCountryPicker() {
        isCountryPickerExpanded = !isCountryPickerExpanded
    }
    
    /**
     * Selects a country code and closes the picker.
     */
    internal fun selectCountryCode(code: String) {
        countryCode = code
        isCountryPickerExpanded = false
    }
}

/**
 * Sets up the Compose UI for PhoneAuthActivity.
 *
 * @receiver The PhoneAuthActivity instance
 * @param stateHolder The state holder managing phone input state
 */
fun PhoneAuthActivity.setupComposeUI(stateHolder: PhoneAuthStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            PhoneAuthScreen(
                phone = stateHolder.phone,
                countryCode = stateHolder.countryCode,
                isLoading = stateHolder.isLoading,
                errorMessage = stateHolder.errorMessage,
                onPhoneChange = { newPhone ->
                    stateHolder.phone = newPhone.filter { it.isDigit() }
                    stateHolder.clearError()
                },
                onCountryCodeChange = { stateHolder.countryCode = it },
                onContinue = {
                    if (stateHolder.isPhoneValid) {
                        handleContinue(stateHolder.fullPhoneNumber)
                    } else {
                        stateHolder.setError("Please enter a valid phone number")
                    }
                },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }
}
