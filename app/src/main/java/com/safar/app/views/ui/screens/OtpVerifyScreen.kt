package com.safar.app.views.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.views.ui.theme.*
import kotlinx.coroutines.delay

/**
 * OtpVerifyScreen - Enterprise-grade OTP verification screen
 *
 * Signature matches OtpVerifyBridge.kt's call:
 *   OtpVerifyScreen(phoneNumber, maskedPhoneNumber, otpValue, otpLength,
 *                   resendSeconds, isLoading, errorMessage, canResend,
 *                   onOtpChange, onVerify, onResend, onBack)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerifyScreen(
    phoneNumber: String = "",
    maskedPhoneNumber: String = "",
    otpValue: String = "",
    otpLength: Int = 6,
    resendSeconds: Int = 60,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    canResend: Boolean = false,
    onOtpChange: (String) -> Unit = {},
    onVerify: () -> Unit = {},
    onResend: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Entrance animation
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(300)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SafarBlack,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = SafarTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafarBlack
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 2 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter verification code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = SafarTextPrimary
                    )
                    Text(
                        text = "We sent a $otpLength-digit code to",
                        fontSize = 16.sp,
                        color = SafarTextSecondary
                    )
                    Text(
                        text = maskedPhoneNumber.ifEmpty { phoneNumber },
                        fontSize = 16.sp,
                        color = SafarTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OTP Input Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 150)) +
                        slideInVertically(tween(400, delayMillis = 150)) { it / 2 }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // OTP Input Boxes
                    OtpInputField(
                        otpValue = otpValue,
                        otpLength = otpLength,
                        onOtpChange = onOtpChange,
                        isError = errorMessage != null,
                        focusRequester = focusRequester,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error Message
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            color = SafarError,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Loading Indicator
                    if (isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SafarGreen,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Verifying...",
                                fontSize = 14.sp,
                                color = SafarTextSecondary
                            )
                        }
                    }

                    // Verify Button
                    Button(
                        onClick = onVerify,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = otpValue.length == otpLength && !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafarGreen,
                            contentColor = SafarBlack,
                            disabledContainerColor = SafarGreen.copy(alpha = 0.25f),
                            disabledContentColor = SafarBlack.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = "Verify",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Resend Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 300)) +
                        slideInVertically(tween(400, delayMillis = 300)) { it }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Didn't receive the code?",
                        fontSize = 14.sp,
                        color = SafarTextSecondary
                    )

                    if (canResend) {
                        TextButton(onClick = onResend) {
                            Text(
                                text = "Resend Code",
                                fontSize = 16.sp,
                                color = SafarGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "Resend in ${resendSeconds}s",
                            fontSize = 14.sp,
                            color = SafarTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * OTP input field with individual digit boxes
 */
@Composable
private fun OtpInputField(
    otpValue: String,
    otpLength: Int,
    onOtpChange: (String) -> Unit,
    isError: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Hidden text field for actual input
        BasicTextField(
            value = otpValue,
            onValueChange = onOtpChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            decorationBox = { }
        )

        // Visual OTP boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(otpLength) { index ->
                val char = otpValue.getOrNull(index)
                val isFilled = char != null
                val isCurrentIndex = index == otpValue.length

                val borderColor = when {
                    isError -> SafarError
                    isCurrentIndex -> SafarGreen
                    isFilled -> SafarGreen.copy(alpha = 0.5f)
                    else -> SafarDivider
                }

                val backgroundColor = when {
                    isFilled -> SafarDarkCard
                    else -> SafarDarkSurface
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .border(
                            width = if (isCurrentIndex || isFilled) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFilled) {
                        Text(
                            text = char.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = SafarTextPrimary
                        )
                    } else if (isCurrentIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(
                                    SafarGreen,
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
