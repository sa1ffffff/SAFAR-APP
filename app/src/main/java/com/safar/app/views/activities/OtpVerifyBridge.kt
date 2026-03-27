package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import com.safar.app.views.ui.screens.OtpVerifyScreen
import com.safar.app.views.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// OTP VERIFY BRIDGE
// Connects Java OtpVerifyActivity to Jetpack Compose OtpVerifyScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In OtpVerifyActivity.java:
//
//   1. Create state holder:
//      private OtpVerifyStateHolder stateHolder = new OtpVerifyStateHolder();
//
//   2. In onCreate():
//      REMOVE:  setContentView(R.layout.activity_otp_verify)
//      stateHolder.setPhoneNumber(getIntent().getStringExtra("phone"));
//      ADD:     OtpVerifyBridgeKt.setupComposeUI(this, stateHolder)
//
//   3. Access OTP value:
//      stateHolder.getOtpValue()
//
//   4. Control loading state:
//      stateHolder.setLoading(true/false)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * OTP verification configuration.
 */
object OtpConfig {
    /** Length of the OTP code */
    const val OTP_LENGTH = 6
    
    /** Initial countdown seconds for resend */
    const val RESEND_COUNTDOWN_SECONDS = 60
    
    /** Minimum seconds before allowing resend */
    const val MIN_RESEND_INTERVAL = 30
}

/**
 * State holder for OTP verification screen.
 * Manages OTP input, countdown timer, and verification state.
 *
 * Java Access Pattern:
 * ```java
 * OtpVerifyStateHolder stateHolder = new OtpVerifyStateHolder();
 * 
 * // Set phone number from intent
 * stateHolder.setPhoneNumber("+923001234567");
 * 
 * // Reading values
 * String otp = stateHolder.getOtpValue();
 * int seconds = stateHolder.getResendSeconds();
 * boolean loading = stateHolder.isLoading();
 * boolean canResend = stateHolder.canResend();
 * 
 * // Setting values
 * stateHolder.setOtpValue("123456");
 * stateHolder.setLoading(true);
 * stateHolder.resetResendTimer();
 * ```
 */
class OtpVerifyStateHolder {
    
    /** The entered OTP value */
    var otpValue by mutableStateOf("")
    
    /** Seconds remaining until resend is allowed */
    var resendSeconds by mutableStateOf(OtpConfig.RESEND_COUNTDOWN_SECONDS)
    
    /** Loading state for verification API call */
    var isLoading by mutableStateOf(false)
    
    /** The phone number being verified (formatted for display) */
    var phoneNumber by mutableStateOf("")
    
    /** Error message to display, null if no error */
    var errorMessage by mutableStateOf<String?>(null)
    
    /** Whether verification was successful */
    var isVerified by mutableStateOf(false)
        internal set
    
    /** Whether to show the name input dialog */
    var showNameDialog by mutableStateOf(false)
    
    /** Name input value */
    var nameInput by mutableStateOf("")
    
    /** Name input error */
    var nameError by mutableStateOf<String?>(null)
    
    /**
     * Whether the resend button should be enabled.
     */
    val canResend: Boolean
        get() = resendSeconds <= 0 && !isLoading
    
    /**
     * Whether the OTP is complete and ready for verification.
     */
    val isOtpComplete: Boolean
        get() = otpValue.length == OtpConfig.OTP_LENGTH
    
    /**
     * Formatted countdown text for resend button.
     */
    val resendCountdownText: String
        get() = if (resendSeconds > 0) {
            "Resend in ${resendSeconds}s"
        } else {
            "Resend Code"
        }
    
    /**
     * Masked phone number for display (e.g., "+92 *** *** 4567").
     */
    val maskedPhoneNumber: String
        get() {
            if (phoneNumber.length < 4) return phoneNumber
            val lastFour = phoneNumber.takeLast(4)
            val prefix = phoneNumber.take(3)
            return "$prefix *** *** $lastFour"
        }
    
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
        isLoading = false
    }
    
    /**
     * Resets the resend countdown timer.
     */
    fun resetResendTimer() {
        resendSeconds = OtpConfig.RESEND_COUNTDOWN_SECONDS
    }
    
    /**
     * Decrements the resend countdown by one second.
     * Called internally by the composable.
     */
    internal fun decrementResendTimer() {
        if (resendSeconds > 0) {
            resendSeconds--
        }
    }
    
    /**
     * Marks verification as successful.
     */
    internal fun markVerified() {
        isVerified = true
        isLoading = false
    }
    
    /**
     * Clears the OTP input.
     */
    fun clearOtp() {
        otpValue = ""
    }
    
    /**
     * Shows the name input dialog.
     */
    fun showNameInput() {
        showNameDialog = true
        nameInput = ""
        nameError = null
    }
    
    /**
     * Validates and returns the name if valid.
     */
    fun validateName(): String? {
        val trimmedName = nameInput.trim()
        return when {
            trimmedName.isEmpty() -> {
                nameError = "Please enter your name"
                null
            }
            trimmedName.length < 2 -> {
                nameError = "Name must be at least 2 characters"
                null
            }
            trimmedName.length > 50 -> {
                nameError = "Name is too long"
                null
            }
            else -> {
                nameError = null
                trimmedName
            }
        }
    }
}

/**
 * Sets up the Compose UI for OtpVerifyActivity.
 *
 * @receiver The OtpVerifyActivity instance
 * @param stateHolder The state holder managing OTP verification state
 */
fun OtpVerifyActivity.setupComposeUI(stateHolder: OtpVerifyStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            // Countdown timer effect
            LaunchedEffect(stateHolder.resendSeconds) {
                if (stateHolder.resendSeconds > 0) {
                    delay(1000L)
                    stateHolder.decrementResendTimer()
                }
            }
            
            // Auto-verify when OTP is complete
            LaunchedEffect(stateHolder.otpValue) {
                if (stateHolder.isOtpComplete && !stateHolder.isLoading) {
                    // Optional: auto-submit when all digits entered
                    // handleVerify(stateHolder.otpValue)
                }
            }
            
            OtpVerifyScreen(
                phoneNumber = stateHolder.phoneNumber,
                maskedPhoneNumber = stateHolder.maskedPhoneNumber,
                otpValue = stateHolder.otpValue,
                otpLength = OtpConfig.OTP_LENGTH,
                resendSeconds = stateHolder.resendSeconds,
                isLoading = stateHolder.isLoading,
                errorMessage = stateHolder.errorMessage,
                canResend = stateHolder.canResend,
                onOtpChange = { newOtp ->
                    if (newOtp.length <= OtpConfig.OTP_LENGTH && newOtp.all { it.isDigit() }) {
                        stateHolder.otpValue = newOtp
                        stateHolder.clearError()
                    }
                },
                onVerify = {
                    if (stateHolder.isOtpComplete) {
                        stateHolder.isLoading = true
                        handleVerify(stateHolder.otpValue)
                    }
                },
                onResend = {
                    if (stateHolder.canResend) {
                        stateHolder.resetResendTimer()
                        stateHolder.clearOtp()
                        handleResend()
                    }
                },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
            
            // Name Input Dialog - Material 3 Design
            if (stateHolder.showNameDialog) {
                WelcomeNameDialog(
                    nameValue = stateHolder.nameInput,
                    nameError = stateHolder.nameError,
                    isLoading = stateHolder.isLoading,
                    onNameChange = { 
                        stateHolder.nameInput = it
                        stateHolder.nameError = null
                    },
                    onContinue = {
                        val validName = stateHolder.validateName()
                        if (validName != null) {
                            stateHolder.isLoading = true
                            handleNameSubmit(validName)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Beautiful Welcome Name Dialog with M3 Design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeNameDialog(
    nameValue: String,
    nameError: String?,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }
    
    Dialog(
        onDismissRequest = { /* Non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = UberMidGray,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Welcome Icon
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = UberGreen.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = UberGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Title
                Text(
                    text = "Welcome to SAFAR!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = UberTextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = "Let's get you started. What should we call you?",
                    fontSize = 14.sp,
                    color = UberTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Name Input Field
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("Your Name") },
                    placeholder = { Text("Enter your full name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (nameError != null) SafarError else UberTextSecondary
                        )
                    },
                    isError = nameError != null,
                    supportingText = if (nameError != null) {
                        { Text(nameError, color = SafarError) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onContinue() }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UberGreen,
                        unfocusedBorderColor = UberLightGray,
                        focusedLabelColor = UberGreen,
                        unfocusedLabelColor = UberTextSecondary,
                        cursorColor = UberGreen,
                        focusedTextColor = UberTextPrimary,
                        unfocusedTextColor = UberTextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Continue Button
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = nameValue.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UberGreen,
                        contentColor = UberBlack,
                        disabledContainerColor = UberGreen.copy(alpha = 0.3f),
                        disabledContentColor = UberBlack.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = UberBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy note
                Text(
                    text = "Your name will be visible to other SAFAR users",
                    fontSize = 12.sp,
                    color = UberTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
