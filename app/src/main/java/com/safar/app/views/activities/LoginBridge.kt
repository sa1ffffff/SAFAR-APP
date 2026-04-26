package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.LoginScreen
import com.safar.app.views.ui.screens.AuthMode
import com.safar.app.views.ui.screens.LoginUiState
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LOGIN BRIDGE
// Connects Java LoginActivity to Jetpack Compose LoginScreen
// Redesigned for Sign In / Sign Up flow
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * State holder for login screen.
 * Manages all UI state and user interactions.
 *
 * Java Access Pattern:
 * ```java
 * LoginStateHolder stateHolder = new LoginStateHolder();
 * 
 * // Reading values
 * boolean isLoading = stateHolder.isLoading();
 * String phone = stateHolder.getPhone();
 * AuthMode authMode = stateHolder.getAuthMode();
 * 
 * // Setting values
 * stateHolder.setPhone("3001234567");
 * stateHolder.setLoading(true);
 * stateHolder.setAuthMode(AuthMode.SIGN_UP);
 * ```
 */
class LoginStateHolder {
    
    private var _uiState by mutableStateOf(LoginUiState())
    val uiState: LoginUiState get() = _uiState
    
    // ─── Auth Mode Management ────────────────────────────────────────────────
    
    fun setAuthMode(mode: AuthMode) {
        _uiState = _uiState.copy(authMode = mode, error = null)
    }
    
    fun getAuthMode(): AuthMode = _uiState.authMode
    
    fun isSignInMode(): Boolean = _uiState.authMode == AuthMode.SIGN_IN
    
    fun isSignUpMode(): Boolean = _uiState.authMode == AuthMode.SIGN_UP
    
    // ─── Phone Input ─────────────────────────────────────────────────────────
    
    fun setPhone(phone: String) {
        _uiState = _uiState.copy(phone = sanitizePhoneInput(phone))
    }
    
    fun getPhone(): String = _uiState.phone
    
    fun setCountryCode(code: String) {
        _uiState = _uiState.copy(countryCode = code)
    }
    
    fun getCountryCode(): String = _uiState.countryCode
    
    fun getFullPhone(): String {
        val normalizedPhone = sanitizePhoneInput(_uiState.phone)
        return _uiState.countryCode + normalizedPhone
    }
    
    // ─── Email Input ─────────────────────────────────────────────────────────
    
    fun setEmail(email: String) {
        _uiState = _uiState.copy(email = email)
    }
    
    fun getEmail(): String = _uiState.email
    
    fun setPassword(password: String) {
        _uiState = _uiState.copy(password = password)
    }
    
    fun getPassword(): String = _uiState.password
    
    fun togglePasswordVisibility() {
        _uiState = _uiState.copy(showPassword = !_uiState.showPassword)
    }
    
    fun getShowPassword(): Boolean = _uiState.showPassword
    
    // ─── Loading State ───────────────────────────────────────────────────────
    
    fun setLoading(loading: Boolean) {
        _uiState = _uiState.copy(isLoading = loading)
    }
    
    fun isLoading(): Boolean = _uiState.isLoading
    
    // ─── Error Management ────────────────────────────────────────────────────
    
    fun setError(error: String?) {
        _uiState = _uiState.copy(error = error)
    }
    
    fun getError(): String? = _uiState.error
    
    fun clearError() {
        _uiState = _uiState.copy(error = null)
    }
    
    // ─── Validation ──────────────────────────────────────────────────────────
    
    fun isPhoneValid(): Boolean = sanitizePhoneInput(_uiState.phone).length >= 9
    
    fun isEmailValid(): Boolean = _uiState.email.isNotEmpty() && 
                                  _uiState.email.contains("@") && 
                                  _uiState.email.contains(".")
    
    fun isPasswordValid(): Boolean = _uiState.password.length >= 6
    
    fun hasPhoneOrEmail(): Boolean = _uiState.phone.isNotEmpty() || _uiState.email.isNotEmpty()
    
    fun canSignIn(): Boolean = hasPhoneOrEmail() && isPasswordValid()
    
    // ─── Reset Functions ─────────────────────────────────────────────────────
    
    fun clearForm() {
        _uiState = _uiState.copy(
            phone = "",
            email = "",
            password = "",
            error = null,
            isLoading = false
        )
    }
    
    fun resetToDefaults() {
        _uiState = LoginUiState()
    }

    private fun sanitizePhoneInput(rawPhone: String): String {
        var digits = rawPhone.filter { it.isDigit() }
        if (digits.startsWith("00")) {
            digits = digits.removePrefix("00")
        }

        val countryDigits = _uiState.countryCode.filter { it.isDigit() }
        if (countryDigits.isNotEmpty() && digits.startsWith(countryDigits)) {
            digits = digits.removePrefix(countryDigits)
        }

        return digits.trimStart('0')
    }
}

/**
 * Sets up the Compose UI for LoginActivity.
 *
 * @receiver The LoginActivity instance
 * @param stateHolder The state holder managing login UI state
 */
fun LoginActivity.setupComposeUI(stateHolder: LoginStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            LoginScreen(
                uiState = stateHolder.uiState,
                onAuthModeChanged = { mode -> stateHolder.setAuthMode(mode) },
                onPhoneChanged = { phone -> stateHolder.setPhone(phone) },
                onEmailChanged = { email -> stateHolder.setEmail(email) },
                onPasswordChanged = { password -> stateHolder.setPassword(password) },
                onSignIn = { 
                    if (stateHolder.canSignIn()) {
                        handleSignIn(
                            stateHolder.getPhone(),
                            stateHolder.getEmail(),
                            stateHolder.getPassword()
                        )
                    }
                },
                onSignUpWithPhone = { 
                    if (stateHolder.isPhoneValid()) {
                        handlePhoneAuth(stateHolder.getFullPhone())
                    }
                },
                onGoogleSignIn = { handleGoogleSignIn() },
                onForgotPassword = { navigateToForgotPassword() },
                onTogglePassword = { stateHolder.togglePasswordVisibility() }
            )
        }
    }
}
