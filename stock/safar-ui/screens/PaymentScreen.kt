package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*
import com.safar.app.views.ui.components.*

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Payment Screen
// Premium payment selection and management UI
// ══════════════════════════════════════════════════════════════════════════════

data class PaymentMethod(
    val id: String,
    val type: PaymentType,
    val name: String,
    val details: String,
    val isDefault: Boolean = false,
    val lastFourDigits: String? = null,
    val expiryDate: String? = null
)

enum class PaymentType {
    Cash, Card, Wallet, UPI, ApplePay, GooglePay
}

data class PaymentScreenState(
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedMethodId: String? = null,
    val walletBalance: String = "",
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    state: PaymentScreenState,
    onBackClick: () -> Unit = {},
    onSelectMethod: (String) -> Unit = {},
    onAddCard: () -> Unit = {},
    onAddWalletMoney: () -> Unit = {},
    onLinkUPI: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    SafarTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Payment",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SafarColors.surface,
                        titleContentColor = SafarColors.textPrimary,
                        navigationIconContentColor = SafarColors.textPrimary
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = SafarColors.surfaceElevated,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {
                        SafarPrimaryButton(
                            text = "Confirm Payment Method",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm()
                            },
                            enabled = state.selectedMethodId != null,
                            isLoading = state.isLoading
                        )
                    }
                }
            },
            containerColor = SafarColors.surface
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ══════════════════════════════════════════════════════════════
                // Wallet Section
                // ══════════════════════════════════════════════════════════════
                item {
                    WalletCard(
                        balance = state.walletBalance,
                        isSelected = state.paymentMethods.find { 
                            it.id == state.selectedMethodId 
                        }?.type == PaymentType.Wallet,
                        onSelect = {
                            val walletMethod = state.paymentMethods.find { 
                                it.type == PaymentType.Wallet 
                            }
                            walletMethod?.let { onSelectMethod(it.id) }
                        },
                        onAddMoney = onAddWalletMoney
                    )
                }

                // ══════════════════════════════════════════════════════════════
                // Payment Methods Section
                // ══════════════════════════════════════════════════════════════
                item {
                    Text(
                        text = "PAYMENT METHODS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.textTertiary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Cash option
                item {
                    PaymentMethodCard(
                        method = PaymentMethod(
                            id = "cash",
                            type = PaymentType.Cash,
                            name = "Cash",
                            details = "Pay with cash"
                        ),
                        isSelected = state.selectedMethodId == "cash",
                        onClick = { onSelectMethod("cash") }
                    )
                }

                // Saved cards
                val cards = state.paymentMethods.filter { it.type == PaymentType.Card }
                if (cards.isNotEmpty()) {
                    item {
                        Text(
                            text = "SAVED CARDS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarColors.textTertiary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(cards) { card ->
                        PaymentMethodCard(
                            method = card,
                            isSelected = state.selectedMethodId == card.id,
                            onClick = { onSelectMethod(card.id) }
                        )
                    }
                }

                // Add new payment options
                item {
                    Text(
                        text = "ADD NEW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.textTertiary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AddPaymentOptionCard(
                            icon = Icons.Rounded.CreditCard,
                            title = "Add Debit/Credit Card",
                            subtitle = "Visa, Mastercard, Amex",
                            onClick = onAddCard,
                            isFirst = true
                        )
                        AddPaymentOptionCard(
                            icon = Icons.Rounded.AccountBalance,
                            title = "Link UPI",
                            subtitle = "GPay, PhonePe, Paytm",
                            onClick = onLinkUPI,
                            isLast = true
                        )
                    }
                }

                // Security note
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecurityNote()
                }
            }
        }
    }
}

@Composable
private fun WalletCard(
    balance: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddMoney: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SafarColors.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "borderColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelect()
            },
        shape = RoundedCornerShape(20.dp),
        color = SafarColors.primary.copy(alpha = 0.1f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) borderColor else SafarColors.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SafarColors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SafarColors.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "SAFAR Wallet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarColors.textPrimary
                        )
                        Text(
                            text = "Available balance",
                            fontSize = 12.sp,
                            color = SafarColors.textSecondary
                        )
                    }
                }

                // Selection indicator
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SafarColors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = balance.ifEmpty { "₹0.00" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarColors.primary
                )

                Surface(
                    modifier = Modifier.clickable(onClick = onAddMoney),
                    shape = RoundedCornerShape(10.dp),
                    color = SafarColors.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Money",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SafarColors.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            SafarColors.primary.copy(alpha = 0.08f)
        } else {
            SafarColors.surfaceElevated
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) borderColor else SafarColors.divider
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            PaymentTypeIcon(type = method.type)

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = method.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafarColors.textPrimary
                    )
                    if (method.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SafarColors.success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Default",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafarColors.success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (method.lastFourDigits != null) {
                        "•••• ${method.lastFourDigits}"
                    } else {
                        method.details
                    },
                    fontSize = 12.sp,
                    color = SafarColors.textSecondary
                )
            }

            // Expiry date or selection indicator
            if (method.expiryDate != null && !isSelected) {
                Text(
                    text = method.expiryDate,
                    fontSize = 12.sp,
                    color = SafarColors.textTertiary
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SafarColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentTypeIcon(type: PaymentType) {
    val (icon, bgColor, iconTint) = when (type) {
        PaymentType.Cash -> Triple(
            Icons.Rounded.Money,
            SafarColors.success.copy(alpha = 0.15f),
            SafarColors.success
        )
        PaymentType.Card -> Triple(
            Icons.Rounded.CreditCard,
            SafarColors.info.copy(alpha = 0.15f),
            SafarColors.info
        )
        PaymentType.Wallet -> Triple(
            Icons.Rounded.AccountBalanceWallet,
            SafarColors.primary.copy(alpha = 0.15f),
            SafarColors.primary
        )
        PaymentType.UPI -> Triple(
            Icons.Rounded.AccountBalance,
            SafarColors.warning.copy(alpha = 0.15f),
            SafarColors.warning
        )
        PaymentType.ApplePay, PaymentType.GooglePay -> Triple(
            Icons.Rounded.Smartphone,
            SafarColors.textSecondary.copy(alpha = 0.15f),
            SafarColors.textSecondary
        )
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AddPaymentOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(14.dp)
        isFirst -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        isLast -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
        else -> RoundedCornerShape(0.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = shape,
        color = SafarColors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafarColors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SafarColors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafarColors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = SafarColors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add",
                tint = SafarColors.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SecurityNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SafarColors.surfaceElevated.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = SafarColors.success,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Your payment info is securely encrypted and protected",
                fontSize = 12.sp,
                color = SafarColors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
