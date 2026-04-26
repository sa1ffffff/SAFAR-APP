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
 * SAFAR Forgot Password Screen
 * Simplified flow using Firebase Auth password reset link
 * User enters email → Firebase sends reset link → User clicks link in email
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    email: String = "",
    isLoading: Boolean = false,
    emailSent: Boolean = false,
    error: String? = null,
    onEmailChanged: (String) -> Unit = {},
    onRequestReset: () -> Unit = {},
    onResend: () -> Unit = {},
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
                            tint = TextPrimary
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
            Spacer(modifier = Modifier.height(40.dp))
            
            // Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Green.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (emailSent) Icons.Rounded.MarkEmailRead else Icons.Rounded.LockReset,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = if (emailSent) "Check Your Email" else "Reset Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtitle
            Text(
                text = if (emailSent) 
                    "We've sent a password reset link to your email. Click the link to reset your password."
                else 
                    "Enter your email address and we'll send you a link to reset your password.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Content based on state
            AnimatedContent(
                targetState = emailSent,
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 2 } togetherWith
                            fadeOut() + slideOutVertically { -it / 2 }
                },
                label = "forgot_password_content"
            ) { sent ->
                if (sent) {
                    EmailSentContent(
                        email = email,
                        onResend = onResend,
                        onBack = onBack,
                        isLoading = isLoading
                    )
                } else {
                    EmailInputContent(
                        email = email,
                        isLoading = isLoading,
                        error = error,
                        onEmailChanged = onEmailChanged,
                        onRequestReset = onRequestReset
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailInputContent(
    email: String,
    isLoading: Boolean,
    error: String?,
    onEmailChanged: (String) -> Unit,
    onRequestReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error message
        AnimatedVisibility(
            visible = error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            error?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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
                            text = it,
                            color = SafarError,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // Email input
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email Address") },
            placeholder = { Text("Enter your email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                focusedBorderColor = Green,
                unfocusedBorderColor = Surface2,
                focusedLabelColor = Green,
                unfocusedLabelColor = TextMuted,
                cursorColor = Green,
                focusedLeadingIconColor = Green,
                unfocusedLeadingIconColor = TextMuted
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Send button
        Button(
            onClick = onRequestReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = email.isNotBlank() && email.contains("@") && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
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
                    imageVector = Icons.Rounded.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Reset Link",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmailSentContent(
    email: String,
    onResend: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Green.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Email Sent Successfully!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "We sent a reset link to:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                Text(
                    text = email,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Green
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InstructionRow(
                    number = "1",
                    text = "Open the email from SAFAR"
                )
                Spacer(modifier = Modifier.height(12.dp))
                InstructionRow(
                    number = "2",
                    text = "Click the password reset link"
                )
                Spacer(modifier = Modifier.height(12.dp))
                InstructionRow(
                    number = "3",
                    text = "Create your new password"
                )
                Spacer(modifier = Modifier.height(12.dp))
                InstructionRow(
                    number = "4",
                    text = "Return to app and sign in"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Didn't receive email?
        Text(
            text = "Didn't receive the email?",
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Resend button
        TextButton(
            onClick = onResend,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Green,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Resend Email",
                color = Green,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Back to login button
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Surface2)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Sign In",
                fontSize = 15.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InstructionRow(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(6.dp),
            color = Green.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}
