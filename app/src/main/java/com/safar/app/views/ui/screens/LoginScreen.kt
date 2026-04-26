package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.safar.app.R
import com.safar.app.views.ui.theme.*

// ─── Data / State ─────────────────────────────────────────────────────────────
enum class AuthMode { SIGN_IN, SIGN_UP }

data class LoginUiState(
    val authMode: AuthMode = AuthMode.SIGN_IN,
    val phone: String = "",
    val countryCode: String = "+92",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val showPassword: Boolean = false,
    val error: String? = null
)

// Keep old enums for compatibility but deprecated
@Deprecated("Use AuthMode instead")
enum class LoginTab { PHONE, EMAIL }
@Deprecated("Use AuthMode instead")
enum class EmailMode { LOGIN, REGISTER }

/**
 * SAFAR LOGIN SCREEN
 * Redesigned authentication UI with Sign In / Sign Up approach
 * Sign In: Phone/Email + Password
 * Sign Up: Phone → OTP → Password Setup, Google → Direct sign in/sign up
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState = LoginUiState(),
    onAuthModeChanged: (AuthMode) -> Unit = {},
    onPhoneChanged: (String) -> Unit = {},
    onEmailChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignUpWithPhone: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onTogglePassword: () -> Unit = {},
    // Legacy callbacks for compatibility
    onTabSelected: (LoginTab) -> Unit = {},
    onNameChanged: (String) -> Unit = {},
    onContinuePhone: () -> Unit = {},
    onEmailAction: () -> Unit = {},
    onToggleEmailMode: () -> Unit = {}
) {
    SafarTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header Section
            LoginHeader()

            Spacer(modifier = Modifier.height(40.dp))

            // Sign In / Sign Up Tab Selector
            AuthModeSelector(
                selectedMode = uiState.authMode,
                onModeSelected = onAuthModeChanged
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error Message
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                uiState.error?.let { error ->
                    ErrorCard(message = error)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Content based on auth mode
            AnimatedContent(
                targetState = uiState.authMode,
                transitionSpec = {
                    val slideDirection = if (targetState == AuthMode.SIGN_UP) 1 else -1
                    slideInHorizontally(
                        initialOffsetX = { it * slideDirection },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200, delayMillis = 100)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { it * -slideDirection },
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(100))
                },
                label = "authModeContent"
            ) { mode ->
                when (mode) {
                    AuthMode.SIGN_IN -> SignInContent(
                        phone = uiState.phone,
                        email = uiState.email,
                        password = uiState.password,
                        showPassword = uiState.showPassword,
                        isLoading = uiState.isLoading,
                        countryCode = uiState.countryCode,
                        onPhoneChanged = onPhoneChanged,
                        onEmailChanged = onEmailChanged,
                        onPasswordChanged = onPasswordChanged,
                        onTogglePassword = onTogglePassword,
                        onSignIn = onSignIn,
                        onForgotPassword = onForgotPassword,
                        onGoogleSignIn = onGoogleSignIn
                    )
                    AuthMode.SIGN_UP -> SignUpContent(
                        phone = uiState.phone,
                        countryCode = uiState.countryCode,
                        isLoading = uiState.isLoading,
                        onPhoneChanged = onPhoneChanged,
                        onSignUpWithPhone = onSignUpWithPhone,
                        onGoogleSignIn = onGoogleSignIn
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Terms
            TermsText()

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Name with professional styling
        Text(
            text = "SAFAR",
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 4.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your trusted ride companion",
            fontSize = 16.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun AuthModeSelector(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit
) {
    val signInSelected = selectedMode == AuthMode.SIGN_IN
    val signUpSelected = selectedMode == AuthMode.SIGN_UP
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            // Sign In Tab
            Button(
                onClick = { onModeSelected(AuthMode.SIGN_IN) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (signInSelected) Green else Color.Transparent,
                    contentColor = if (signInSelected) Color.Black else TextSecondary
                ),
                elevation = if (signInSelected) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) 
                          else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Sign In",
                    fontSize = 16.sp,
                    fontWeight = if (signInSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Sign Up Tab
            Button(
                onClick = { onModeSelected(AuthMode.SIGN_UP) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (signUpSelected) Green else Color.Transparent,
                    contentColor = if (signUpSelected) Color.Black else TextSecondary
                ),
                elevation = if (signUpSelected) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) 
                          else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 16.sp,
                    fontWeight = if (signUpSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SignInContent(
    phone: String,
    email: String,
    password: String,
    showPassword: Boolean,
    isLoading: Boolean,
    countryCode: String,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Info text
        Text(
            text = "Welcome back! Sign in with your phone number or email and password.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Phone or Email Input
        OutlinedTextField(
            value = if (phone.isNotEmpty()) phone else email,
            onValueChange = { value ->
                // Detect if it's a phone number or email
                if (value.all { it.isDigit() || it == '+' }) {
                    onPhoneChanged(value)
                    onEmailChanged("")
                } else {
                    onEmailChanged(value)
                    onPhoneChanged("")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number or Email") },
            placeholder = { Text("Enter phone or email") },
            leadingIcon = {
                Icon(
                    imageVector = if (phone.isNotEmpty()) Icons.Rounded.Phone else Icons.Rounded.AlternateEmail,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = loginTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (phone.isNotEmpty()) KeyboardType.Phone else KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            colors = loginTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Forgot Password
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onForgotPassword) {
                Text(
                    text = "Forgot Password?",
                    color = Green,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sign In Button
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = (phone.isNotEmpty() || email.isNotEmpty()) && password.length >= 6 && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = SafarYellow,
                contentColor = Color.Black,
                disabledContainerColor = Surface2,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Surface2
            )
            Text(
                text = "  OR  ",
                color = TextMuted,
                fontSize = 12.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Surface2
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Google Sign In Button
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Surface2)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SignUpContent(
    phone: String,
    countryCode: String,
    isLoading: Boolean,
    onPhoneChanged: (String) -> Unit,
    onSignUpWithPhone: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subheading for new users
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SafarYellow.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = SafarYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "New to SAFAR? Create an account to start your journey!",
                    fontSize = 13.sp,
                    color = SafarTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Sign up with your phone number",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Text(
            text = "We'll send you a verification code",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Phone Input with Country Code
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgElevated)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countryCode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChanged,
                    placeholder = { 
                        Text(
                            text = "3xx xxxxxxx",
                            color = TextMuted,
                            fontSize = 16.sp
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = SafarYellow,
                        unfocusedBorderColor = Surface2,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SafarYellow
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Continue with Phone Button
        Button(
            onClick = onSignUpWithPhone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = phone.length >= 7 && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = SafarYellow,
                contentColor = Color.Black,
                disabledContainerColor = Surface2,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue with Phone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Surface2,
                thickness = 1.dp
            )
            Text(
                text = "OR",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Surface2,
                thickness = 1.dp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Google Sign Up Button
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, Surface2),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedBorderColor = SafarYellow,
    unfocusedBorderColor = Surface2,
    focusedLabelColor = SafarYellow,
    unfocusedLabelColor = TextMuted,
    cursorColor = SafarYellow,
    focusedLeadingIconColor = SafarYellow,
    unfocusedLeadingIconColor = TextMuted,
    focusedTrailingIconColor = TextSecondary,
    unfocusedTrailingIconColor = TextMuted,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted
)

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SafarError.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = SafarError,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = SafarError,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TermsText() {
    Text(
        text = "By continuing, you agree to our Terms of Service and Privacy Policy",
        style = MaterialTheme.typography.bodySmall.copy(
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
