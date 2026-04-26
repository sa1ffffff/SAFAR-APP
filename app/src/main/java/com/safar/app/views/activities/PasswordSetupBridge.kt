package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.PasswordSetupScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PASSWORD SETUP BRIDGE
// Connects Java PasswordSetupActivity to Jetpack Compose PasswordSetupScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * State holder for password setup screen.
 */
class PasswordSetupStateHolder {
    
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var showPassword by mutableStateOf(false)
    var showConfirmPassword by mutableStateOf(false)
    var error: String? by mutableStateOf(null)
    
    @JvmName("updateError")
    fun setError(msg: String?) {
        error = msg
    }
    
    fun clearError() {
        error = null
    }
    
    @JvmName("updateLoading")
    fun setLoading(loading: Boolean) {
        isLoading = loading
    }
    
    @JvmName("updateName")
    fun setName(value: String) {
        name = value
    }
    
    @JvmName("updateEmail")
    fun setEmail(value: String) {
        email = value
    }
    
    // Validation helpers
    fun isNameValid(): Boolean = name.isNotBlank()
    
    fun isEmailValid(): Boolean = email.isNotBlank() && email.contains("@") && email.contains(".")
    
    fun isPasswordValid(): Boolean = password.length >= 6
    
    fun doPasswordsMatch(): Boolean = password == confirmPassword && password.isNotEmpty()
    
    fun isFormValid(): Boolean = isNameValid() && isEmailValid() && isPasswordValid() && doPasswordsMatch()
}

/**
 * Sets up the Compose UI for PasswordSetupActivity.
 */
fun PasswordSetupActivity.setupComposeUI(stateHolder: PasswordSetupStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            PasswordSetupScreen(
                name = stateHolder.name,
                email = stateHolder.email,
                password = stateHolder.password,
                confirmPassword = stateHolder.confirmPassword,
                isLoading = stateHolder.isLoading,
                showPassword = stateHolder.showPassword,
                showConfirmPassword = stateHolder.showConfirmPassword,
                error = stateHolder.error,
                isFormValid = stateHolder.isFormValid(),
                onNameChanged = { stateHolder.name = it },
                onEmailChanged = { stateHolder.email = it },
                onPasswordChanged = { stateHolder.password = it },
                onConfirmPasswordChanged = { stateHolder.confirmPassword = it },
                onTogglePassword = { stateHolder.showPassword = !stateHolder.showPassword },
                onToggleConfirmPassword = { stateHolder.showConfirmPassword = !stateHolder.showConfirmPassword },
                onSubmit = {
                    if (stateHolder.isFormValid()) {
                        handleSubmit(
                            stateHolder.name,
                            stateHolder.email,
                            stateHolder.password,
                            stateHolder.confirmPassword
                        )
                    }
                },
                onBack = { handleBack() }
            )
        }
    }
}
