package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

/**
 * SAFAR Password Setup Screen
 * Shown after phone OTP verification to collect password and name
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSetupScreen(
    name: String = "",
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    isLoading: Boolean = false,
    showPassword: Boolean = false,
    showConfirmPassword: Boolean = false,
    error: String? = null,
    isFormValid: Boolean = false,
    onNameChanged: (String) -> Unit = {},
    onEmailChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onConfirmPasswordChanged: (String) -> Unit = {},
    onTogglePassword: () -> Unit = {},
    onToggleConfirmPassword: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SafarTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark
                )
            )
        },
        containerColor = BgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Header
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = SafarYellow,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Complete Your Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SafarTextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Set up a password to secure your account.\nYou'll use this to sign in next time.",
                fontSize = 14.sp,
                color = SafarTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Error Message
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                error?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SafarRed.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = SafarRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = it,
                                color = SafarRed,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("Full Name") },
                placeholder = { Text("Enter your name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = SafarTextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = safarTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChanged,
                label = { Text("Email Address") },
                placeholder = { Text("your@email.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = SafarTextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = safarTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We'll use this for password recovery and notifications",
                fontSize = 12.sp,
                color = SafarTextTertiary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = { Text("Create Password") },
                placeholder = { Text("Minimum 6 characters") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = SafarTextTertiary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = SafarTextTertiary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                colors = safarTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm Password Input
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                label = { Text("Confirm Password") },
                placeholder = { Text("Re-enter your password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = SafarTextTertiary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onToggleConfirmPassword) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                            tint = SafarTextTertiary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                colors = safarTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Password mismatch indicator
            AnimatedVisibility(
                visible = confirmPassword.isNotEmpty() && password != confirmPassword
            ) {
                Text(
                    text = "Passwords do not match",
                    color = SafarRed,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Submit Button
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafarYellow,
                    contentColor = Color.Black,
                    disabledContainerColor = SafarYellow.copy(alpha = 0.4f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
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
                        text = "Complete Registration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Security note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = SafarGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your password is securely encrypted",
                    fontSize = 12.sp,
                    color = SafarTextTertiary
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun safarTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SafarTextPrimary,
    unfocusedTextColor = SafarTextPrimary,
    focusedContainerColor = SafarDarkSurface,
    unfocusedContainerColor = SafarDarkSurface,
    focusedBorderColor = SafarYellow,
    unfocusedBorderColor = SafarTextTertiary.copy(alpha = 0.3f),
    focusedLabelColor = SafarYellow,
    unfocusedLabelColor = SafarTextTertiary,
    cursorColor = SafarYellow,
    focusedLeadingIconColor = SafarYellow,
    unfocusedLeadingIconColor = SafarTextTertiary,
    focusedTrailingIconColor = SafarTextSecondary,
    unfocusedTrailingIconColor = SafarTextTertiary,
    errorBorderColor = SafarRed,
    errorLabelColor = SafarRed,
    focusedPlaceholderColor = SafarTextTertiary,
    unfocusedPlaceholderColor = SafarTextTertiary
)
