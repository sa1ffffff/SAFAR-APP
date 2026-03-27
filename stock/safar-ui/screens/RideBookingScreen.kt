package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*
import com.safar.app.views.ui.components.*

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Ride Booking Screen
// Premium ride selection & confirmation UI
// ══════════════════════════════════════════════════════════════════════════════

data class RideOption(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val originalPrice: String? = null,
    val eta: String,
    val capacity: Int,
    val iconType: RideIconType = RideIconType.Economy,
    val isPromo: Boolean = false,
    val promoLabel: String? = null
)

enum class RideIconType {
    Economy, Comfort, Premium, Bike, Van
}

data class RideBookingState(
    val pickupLocation: String = "",
    val destinationLocation: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val estimatedDistance: String = "",
    val estimatedDuration: String = "",
    val rideOptions: List<RideOption> = emptyList(),
    val selectedRideId: String? = null,
    val isLoading: Boolean = false,
    val paymentMethod: String = "Cash",
    val promoCode: String? = null,
    val scheduledTime: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideBookingScreen(
    state: RideBookingState,
    onBackClick: () -> Unit = {},
    onRideSelect: (String) -> Unit = {},
    onConfirmRide: () -> Unit = {},
    onPaymentClick: () -> Unit = {},
    onPromoClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onEditPickup: () -> Unit = {},
    onEditDestination: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    SafarTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                RideBookingTopBar(
                    onBackClick = onBackClick
                )
            },
            containerColor = SafarColors.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ══════════════════════════════════════════════════════════════
                // Route Summary Card
                // ══════════════════════════════════════════════════════════════
                RouteSummaryCard(
                    pickupAddress = state.pickupAddress,
                    destinationAddress = state.destinationAddress,
                    distance = state.estimatedDistance,
                    duration = state.estimatedDuration,
                    onEditPickup = onEditPickup,
                    onEditDestination = onEditDestination,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // ══════════════════════════════════════════════════════════════
                // Ride Options Section
                // ══════════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Choose your ride",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    state.rideOptions.forEach { option ->
                        RideOptionCard(
                            option = option,
                            isSelected = state.selectedRideId == option.id,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRideSelect(option.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ══════════════════════════════════════════════════════════════
                // Bottom Booking Panel
                // ══════════════════════════════════════════════════════════════
                BookingBottomPanel(
                    selectedRide = state.rideOptions.find { it.id == state.selectedRideId },
                    paymentMethod = state.paymentMethod,
                    promoCode = state.promoCode,
                    scheduledTime = state.scheduledTime,
                    isLoading = state.isLoading,
                    onPaymentClick = onPaymentClick,
                    onPromoClick = onPromoClick,
                    onScheduleClick = onScheduleClick,
                    onConfirmRide = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirmRide()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideBookingTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Select Ride",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SafarColors.surface,
            titleContentColor = SafarColors.textPrimary,
            navigationIconContentColor = SafarColors.textPrimary
        )
    )
}

@Composable
private fun RouteSummaryCard(
    pickupAddress: String,
    destinationAddress: String,
    distance: String,
    duration: String,
    onEditPickup: () -> Unit,
    onEditDestination: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SafarColors.surfaceElevated,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Pickup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditPickup),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(SafarColors.success)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PICKUP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.textTertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = pickupAddress.ifEmpty { "Select pickup location" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SafarColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    tint = SafarColors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Dotted line
            Row(
                modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Column {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(SafarColors.textTertiary.copy(alpha = 0.5f))
                        )
                        if (it < 2) Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }

            // Destination
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditDestination),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SafarColors.primary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DROP-OFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.textTertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = destinationAddress.ifEmpty { "Select destination" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SafarColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    tint = SafarColors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Trip info
            if (distance.isNotEmpty() || duration.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SafarColors.divider)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (distance.isNotEmpty()) {
                        TripInfoChip(
                            icon = Icons.Rounded.Route,
                            value = distance,
                            label = "Distance"
                        )
                    }
                    if (duration.isNotEmpty()) {
                        TripInfoChip(
                            icon = Icons.Rounded.Schedule,
                            value = duration,
                            label = "Duration"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripInfoChip(
    icon: ImageVector,
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SafarColors.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SafarColors.textPrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = SafarColors.textTertiary
            )
        }
    }
}

@Composable
private fun RideOptionCard(
    option: RideOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cardScale"
    )

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
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) borderColor else SafarColors.divider
        ),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Icon
            RideTypeIcon(type = option.iconType)

            Spacer(modifier = Modifier.width(14.dp))

            // Ride Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.textPrimary
                    )
                    if (option.isPromo && option.promoLabel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SafarColors.success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = option.promoLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafarColors.success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    fontSize = 12.sp,
                    color = SafarColors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = SafarColors.textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = option.eta,
                        fontSize = 11.sp,
                        color = SafarColors.textTertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = SafarColors.textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${option.capacity}",
                        fontSize = 11.sp,
                        color = SafarColors.textTertiary
                    )
                }
            }

            // Price
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = option.price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarColors.primary
                )
                option.originalPrice?.let { original ->
                    Text(
                        text = original,
                        fontSize = 12.sp,
                        color = SafarColors.textTertiary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                }
            }

            // Selection indicator
            Spacer(modifier = Modifier.width(10.dp))
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
private fun RideTypeIcon(type: RideIconType) {
    val (icon, bgColor) = when (type) {
        RideIconType.Economy -> Icons.Rounded.DirectionsCar to SafarColors.info.copy(alpha = 0.15f)
        RideIconType.Comfort -> Icons.Rounded.AirlineSeatReclineNormal to SafarColors.primary.copy(alpha = 0.15f)
        RideIconType.Premium -> Icons.Rounded.Diamond to SafarColors.warning.copy(alpha = 0.15f)
        RideIconType.Bike -> Icons.Rounded.TwoWheeler to SafarColors.success.copy(alpha = 0.15f)
        RideIconType.Van -> Icons.Rounded.AirportShuttle to SafarColors.error.copy(alpha = 0.15f)
    }
    
    val iconTint = when (type) {
        RideIconType.Economy -> SafarColors.info
        RideIconType.Comfort -> SafarColors.primary
        RideIconType.Premium -> SafarColors.warning
        RideIconType.Bike -> SafarColors.success
        RideIconType.Van -> SafarColors.error
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun BookingBottomPanel(
    selectedRide: RideOption?,
    paymentMethod: String,
    promoCode: String?,
    scheduledTime: String?,
    isLoading: Boolean,
    onPaymentClick: () -> Unit,
    onPromoClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onConfirmRide: () -> Unit
) {
    Surface(
        color = SafarColors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Payment method
                QuickActionButton(
                    icon = Icons.Rounded.Payment,
                    text = paymentMethod,
                    onClick = onPaymentClick,
                    modifier = Modifier.weight(1f)
                )
                
                // Promo code
                QuickActionButton(
                    icon = Icons.Rounded.LocalOffer,
                    text = promoCode ?: "Add Promo",
                    onClick = onPromoClick,
                    isHighlighted = promoCode != null,
                    modifier = Modifier.weight(1f)
                )
                
                // Schedule
                QuickActionButton(
                    icon = Icons.Rounded.Schedule,
                    text = scheduledTime ?: "Now",
                    onClick = onScheduleClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm button
            SafarPrimaryButton(
                text = if (selectedRide != null) {
                    "Confirm ${selectedRide.name} • ${selectedRide.price}"
                } else {
                    "Select a ride"
                },
                onClick = onConfirmRide,
                enabled = selectedRide != null,
                isLoading = isLoading,
                trailingIcon = Icons.Rounded.ArrowForward
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    
    Surface(
        modifier = modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) {
            SafarColors.success.copy(alpha = 0.1f)
        } else {
            SafarColors.surfaceCard
        },
        border = if (isHighlighted) {
            BorderStroke(1.dp, SafarColors.success.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) SafarColors.success else SafarColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isHighlighted) SafarColors.success else SafarColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
