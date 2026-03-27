package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.LoginScreen
import com.safar.app.views.ui.screens.LoginTab
import com.safar.app.views.ui.screens.EmailMode
import com.safar.app.views.ui.screens.LoginUiState
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LOGIN BRIDGE
// Connects Java LoginActivity to Jetpack Compose LoginScreen
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
 * LoginTab selectedTab = stateHolder.getSelectedTab();
 * 
 * // Setting values
 * stateHolder.setPhone("3001234567");
 * stateHolder.setLoading(true);
 * stateHolder.selectTab(LoginTab.EMAIL);
 * ```
 */
class LoginStateHolder {
    
    private var _uiState by mutableStateOf(LoginUiState())
    val uiState: LoginUiState get() = _uiState
    
    // ─── Tab Management ──────────────────────────────────────────────────────
    
    fun selectTab(tab: LoginTab) {
        _uiState = _uiState.copy(selectedTab = tab, error = null)
    }
    
    fun getSelectedTab(): LoginTab = _uiState.selectedTab
    
    // ─── Phone Input ─────────────────────────────────────────────────────────
    
    fun setPhone(phone: String) {
        _uiState = _uiState.copy(phone = phone)
    }
    
    fun getPhone(): String = _uiState.phone
    
    fun setCountryCode(code: String) {
        _uiState = _uiState.copy(countryCode = code)
    }
    
    fun getCountryCode(): String = _uiState.countryCode
    
    // ─── Email Input ─────────────────────────────────────────────────────────
    
    fun setEmail(email: String) {
        _uiState = _uiState.copy(email = email)
    }
    
    fun getEmail(): String = _uiState.email
    
    fun setPassword(password: String) {
        _uiState = _uiState.copy(password = password)
    }
    
    fun getPassword(): String = _uiState.password
    
    fun setName(name: String) {
        _uiState = _uiState.copy(name = name)
    }
    
    fun getName(): String = _uiState.name
    
    fun togglePasswordVisibility() {
        _uiState = _uiState.copy(showPassword = !_uiState.showPassword)
    }
    
    fun getShowPassword(): Boolean = _uiState.showPassword
    
    // ─── Email Mode Management ───────────────────────────────────────────────
    
    fun toggleEmailMode() {
        val newMode = if (_uiState.emailMode == EmailMode.LOGIN) EmailMode.REGISTER else EmailMode.LOGIN
        _uiState = _uiState.copy(emailMode = newMode, error = null)
    }
    
    fun getEmailMode(): EmailMode = _uiState.emailMode
    
    fun isInRegisterMode(): Boolean = _uiState.emailMode == EmailMode.REGISTER
    
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
    
    fun isPhoneValid(): Boolean = _uiState.phone.length >= 7
    
    fun isEmailValid(): Boolean = _uiState.email.isNotEmpty() && 
                                  _uiState.email.contains("@") && 
                                  _uiState.email.contains(".")
    
    fun isPasswordValid(): Boolean = _uiState.password.length >= 6
    
    fun isRegisterFormValid(): Boolean = isEmailValid() && isPasswordValid() && _uiState.name.isNotEmpty()
    
    fun isLoginFormValid(): Boolean = isEmailValid() && isPasswordValid()
    
    // ─── Reset Functions ─────────────────────────────────────────────────────
    
    fun clearForm() {
        _uiState = _uiState.copy(
            phone = "",
            name = "",
            email = "",
            password = "",
            error = null,
            isLoading = false
        )
    }
    
    fun resetToDefaults() {
        _uiState = LoginUiState()
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
                onTabSelected = { tab -> stateHolder.selectTab(tab) },
                onPhoneChanged = { phone -> stateHolder.setPhone(phone) },
                onEmailChanged = { email -> stateHolder.setEmail(email) },
                onPasswordChanged = { password -> stateHolder.setPassword(password) },
                onNameChanged = { name -> stateHolder.setName(name) },
                onContinuePhone = { 
                    if (stateHolder.isPhoneValid()) {
                        handlePhoneAuth(stateHolder.getCountryCode() + stateHolder.getPhone())
                    }
                },
                onEmailAction = {
                    if (stateHolder.isInRegisterMode()) {
                        if (stateHolder.isRegisterFormValid()) {
                            handleEmailRegister(
                                stateHolder.getEmail(),
                                stateHolder.getPassword(),
                                stateHolder.getName()
                            )
                        }
                    } else {
                        if (stateHolder.isLoginFormValid()) {
                            handleEmailLogin(
                                stateHolder.getEmail(),
                                stateHolder.getPassword()
                            )
                        }
                    }
                },
                onToggleEmailMode = { stateHolder.toggleEmailMode() },
                onForgotPassword = { 
                    if (stateHolder.isEmailValid()) {
                        handleForgotPassword(stateHolder.getEmail())
                    } else {
                        stateHolder.setError("Enter your email first")
                    }
                },
                onGoogleSignIn = { handleGoogleSignIn() },
                onTogglePassword = { stateHolder.togglePasswordVisibility() }
            )
        }
    }
}