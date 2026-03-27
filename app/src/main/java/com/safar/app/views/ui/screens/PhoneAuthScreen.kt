package com.safar.app.views.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.views.ui.theme.*
import kotlinx.coroutines.delay

/**
 * PhoneAuthScreen - Enterprise-grade phone authentication screen
 *
 * Signature matches PhoneAuthBridge.kt's call:
 *   PhoneAuthScreen(phone, countryCode, isLoading, errorMessage,
 *                   onPhoneChange, onCountryCodeChange, onContinue, onBack)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    phone: String = "",
    countryCode: String = "+92",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onPhoneChange: (String) -> Unit = {},
    onCountryCodeChange: (String) -> Unit = {},
    onContinue: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(300)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val isValidPhone = phone.length >= 9

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
                        text = "Enter your mobile number",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = SafarTextPrimary
                    )
                    Text(
                        text = "We'll send you a verification code to confirm your identity",
                        fontSize = 16.sp,
                        color = SafarTextSecondary,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Input Section
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 150)) +
                        slideInVertically(tween(400, delayMillis = 150)) { it / 2 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code Selector
                        CountryCodeSelector(
                            selectedCode = countryCode,
                            onCodeSelected = onCountryCodeChange,
                            modifier = Modifier.width(100.dp)
                        )

                        // Phone Number Input
                        PhoneNumberInput(
                            value = phone,
                            onValueChange = onPhoneChange,
                            focusRequester = focusRequester,
                            onDone = { if (isValidPhone) { keyboardController?.hide(); onContinue() } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Error Message
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            color = SafarError,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Security Notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = SafarTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Your number is secure and will not be shared",
                            fontSize = 13.sp,
                            color = SafarTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Continue Button
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 300)) +
                        slideInVertically(tween(400, delayMillis = 300)) { it }
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isValidPhone && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafarGreen,
                        contentColor = SafarBlack,
                        disabledContainerColor = SafarGreen.copy(alpha = 0.25f),
                        disabledContentColor = SafarBlack.copy(alpha = 0.25f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SafarBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Country code selector component with dropdown functionality
 */
@Composable
private fun CountryCodeSelector(
    selectedCode: String,
    onCodeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val countryCodes = listOf(
        "+92" to "PK",
        "+1" to "US",
        "+44" to "UK",
        "+971" to "AE",
        "+966" to "SA",
        "+91" to "IN"
    )

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = SafarDarkCard,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = SafarDivider
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCode,
                    fontSize = 16.sp,
                    color = SafarTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SafarDarkSurface)
        ) {
            countryCodes.forEach { (code, country) ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country,
                                fontSize = 14.sp,
                                color = SafarTextSecondary
                            )
                            Text(
                                text = code,
                                fontSize = 16.sp,
                                color = SafarTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        onCodeSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Custom phone number input field
 */
@Composable
private fun PhoneNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = SafarDarkCard,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (value.isNotEmpty()) SafarGreen else SafarDivider
        )
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SafarTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                letterSpacing = 2.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            singleLine = true,
            cursorBrush = SolidColor(SafarGreen),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "3XX XXXXXXX",
                            fontSize = 16.sp,
                            color = SafarTextTertiary.copy(alpha = 0.5f),
                            letterSpacing = 2.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
