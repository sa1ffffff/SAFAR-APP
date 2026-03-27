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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.safar.app.views.ui.theme.*

// ─── Data / State ─────────────────────────────────────────────────────────────
enum class LoginTab { PHONE, EMAIL }
enum class EmailMode { LOGIN, REGISTER }

data class LoginUiState(
    val selectedTab: LoginTab = LoginTab.PHONE,
    val emailMode: EmailMode = EmailMode.LOGIN,
    val phone: String = "",
    val countryCode: String = "+92",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val showPassword: Boolean = false,
    val error: String? = null
)

/**
 * SAFAR LOGIN SCREEN
 * Instagram-inspired authentication UI with sliding tab animations
 * Production-ready with Uber dark aesthetic
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState = LoginUiState(),
    onTabSelected: (LoginTab) -> Unit = {},
    onPhoneChanged: (String) -> Unit = {},
    onEmailChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onNameChanged: (String) -> Unit = {},
    onContinuePhone: () -> Unit = {},
    onEmailAction: () -> Unit = {},
    onToggleEmailMode: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onTogglePassword: () -> Unit = {}
) {
    SafarTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark) // Dark background for login
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header Section
            LoginHeader()

            Spacer(modifier = Modifier.height(40.dp))

            // Professional Tab Selector (updated call)
            ProfessionalTabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected
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

            // Tab Content with Instagram-style transitions
            InstagramTabContent(
                uiState = uiState,
                onPhoneChanged = onPhoneChanged,
                onContinuePhone = onContinuePhone,
                onNameChanged = onNameChanged,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onEmailAction = onEmailAction,
                onToggleEmailMode = onToggleEmailMode,
                onForgotPassword = onForgotPassword,
                onTogglePassword = onTogglePassword
            )

            Spacer(modifier = Modifier.height(32.dp))

            // OR Divider
            OrDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign In
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                isLoading = uiState.isLoading
            )

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
private fun ProfessionalTabSelector(
    selectedTab: LoginTab,
    onTabSelected: (LoginTab) -> Unit
) {
    // Simple animation without complex sliding
    val phoneSelected = selectedTab == LoginTab.PHONE
    val emailSelected = selectedTab == LoginTab.EMAIL
    
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
            // Phone Tab
            Button(
                onClick = { onTabSelected(LoginTab.PHONE) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (phoneSelected) Green else Color.Transparent,
                    contentColor = if (phoneSelected) Color.Black else TextSecondary
                ),
                elevation = if (phoneSelected) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) 
                          else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Phone",
                    fontSize = 16.sp,
                    fontWeight = if (phoneSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Email Tab
            Button(
                onClick = { onTabSelected(LoginTab.EMAIL) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (emailSelected) Green else Color.Transparent,
                    contentColor = if (emailSelected) Color.Black else TextSecondary
                ),
                elevation = if (emailSelected) ButtonDefaults.buttonElevation(defaultElevation = 3.dp) 
                          else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Email",
                    fontSize = 16.sp,
                    fontWeight = if (emailSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun InstagramTabContent(
    uiState: LoginUiState,
    onPhoneChanged: (String) -> Unit,
    onContinuePhone: () -> Unit,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEmailAction: () -> Unit,
    onToggleEmailMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onTogglePassword: () -> Unit
) {
    // Instagram-style content sliding
    AnimatedContent(
        targetState = uiState.selectedTab,
        transitionSpec = {
            val slideDirection = if (targetState == LoginTab.EMAIL) 1 else -1
            slideInHorizontally(
                initialOffsetX = { it * slideDirection },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200, delayMillis = 100)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { it * -slideDirection },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(100))
        },
        label = "tabContent"
    ) { tab ->
        when (tab) {
            LoginTab.PHONE -> PhoneTabContent(
                phone = uiState.phone,
                countryCode = uiState.countryCode,
                isLoading = uiState.isLoading,
                onPhoneChanged = onPhoneChanged,
                onContinue = onContinuePhone
            )
            LoginTab.EMAIL -> EmailTabContent(
                mode = uiState.emailMode,
                name = uiState.name,
                email = uiState.email,
                password = uiState.password,
                showPassword = uiState.showPassword,
                isLoading = uiState.isLoading,
                onNameChanged = onNameChanged,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onEmailAction = onEmailAction,
                onToggleEmailMode = onToggleEmailMode,
                onForgotPassword = onForgotPassword,
                onTogglePassword = onTogglePassword
            )
        }
    }
}

@Composable
private fun PhoneTabContent(
    phone: String,
    countryCode: String,
    isLoading: Boolean,
    onPhoneChanged: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phone Input with Country Code - Professional styling
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
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Surface2,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Green
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue Button - Professional styling
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = phone.length >= 7 && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Color.Black,
                disabledContainerColor = Surface2,
                disabledContentColor = TextMuted
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmailTabContent(
    mode: EmailMode,
    name: String,
    email: String,
    password: String,
    showPassword: Boolean,
    isLoading: Boolean,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEmailAction: () -> Unit,
    onToggleEmailMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onTogglePassword: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Name field (only in register mode)
        AnimatedVisibility(
            visible = mode == EmailMode.REGISTER,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                PremiumTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    placeholder = "Full Name",
                    leadingIcon = Icons.Rounded.Person
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Email field
        PremiumTextField(
            value = email,
            onValueChange = onEmailChanged,
            placeholder = "Email",
            leadingIcon = Icons.Rounded.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        PremiumTextField(
            value = password,
            onValueChange = onPasswordChanged,
            placeholder = "Password",
            leadingIcon = Icons.Rounded.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = onTogglePassword
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Forgot Password (only in login mode)
        AnimatedVisibility(
            visible = mode == EmailMode.LOGIN,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = "Forgot Password?",
                        color = Green, // Changed from Yellow to Green
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Button
        Button(
            onClick = onEmailAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = email.isNotEmpty() && password.length >= 6 && 
                     (mode == EmailMode.LOGIN || name.isNotEmpty()) && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                contentColor = Color.Black,
                disabledContainerColor = Surface2,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (mode == EmailMode.LOGIN) "Sign In" else "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Toggle
        TextButton(onClick = onToggleEmailMode) {
            Text(
                text = if (mode == EmailMode.LOGIN) 
                    "Don't have an account? Sign up" 
                else 
                    "Already have an account? Sign in",
                color = TextMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { 
            Text(
                text = placeholder,
                color = TextMuted,
                fontSize = 16.sp
            ) 
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedBorderColor = Green, // Changed from Yellow to Green
            unfocusedBorderColor = Surface2,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
    )
}

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
private fun OrDivider() {
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
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = TextPrimary
        ),
        border = BorderStroke(1.dp, Surface2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Login,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
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