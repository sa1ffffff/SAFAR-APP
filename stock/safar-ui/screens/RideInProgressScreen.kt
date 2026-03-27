package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.google.android.gms.maps.MapView
import com.safar.app.views.ui.SafarMapView
import com.safar.app.views.ui.theme.*
import com.safar.app.views.ui.components.*

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Ride In Progress Screen
// Premium live ride tracking experience
// ══════════════════════════════════════════════════════════════════════════════

data class DriverInfo(
    val name: String = "",
    val rating: Float = 0f,
    val totalTrips: Int = 0,
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val licensePlate: String = "",
    val photoUrl: String? = null
)

data class RideInProgressState(
    val rideStatus: RideStatus = RideStatus.DriverArriving,
    val driverInfo: DriverInfo = DriverInfo(),
    val etaMinutes: Int = 0,
    val distanceRemaining: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val estimatedPrice: String = "",
    val paymentMethod: String = "Cash",
    val rideCode: String? = null,
    val canShareRide: Boolean = true
)

enum class RideStatus {
    DriverArriving,
    WaitingForPickup,
    OnTheWay,
    Arriving,
    Completed
}

@Composable
fun RideInProgressScreen(
    state: RideInProgressState,
    onMapReady: (MapView) -> Unit = {},
    onCallDriver: () -> Unit = {},
    onMessageDriver: () -> Unit = {},
    onShareRide: () -> Unit = {},
    onEmergency: () -> Unit = {},
    onCancelRide: () -> Unit = {},
    onMyLocation: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    SafarTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ══════════════════════════════════════════════════════════════
            // Map Background
            // ══════════════════════════════════════════════════════════════
            SafarMapView(
                modifier = Modifier.fillMaxSize(),
                onMapReady = onMapReady
            )

            // ══════════════════════════════════════════════════════════════
            // Top Status Bar
            // ══════════════════════════════════════════════════════════════
            RideStatusBar(
                status = state.rideStatus,
                etaMinutes = state.etaMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )

            // ══════════════════════════════════════════════════════════════
            // Right side FABs
            // ══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SafarIconButton(
                    icon = Icons.Rounded.MyLocation,
                    onClick = onMyLocation,
                    tint = SafarColors.primary
                )
                SafarIconButton(
                    icon = Icons.Rounded.Share,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShareRide()
                    },
                    tint = SafarColors.textSecondary
                )
                SafarIconButton(
                    icon = Icons.Rounded.Warning,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEmergency()
                    },
                    tint = SafarColors.error,
                    backgroundColor = SafarColors.error.copy(alpha = 0.15f)
                )
            }

            // ══════════════════════════════════════════════════════════════
            // Bottom Panel
            // ══════════════════════════════════════════════════════════════
            RideInProgressBottomPanel(
                state = state,
                onCallDriver = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCallDriver()
                },
                onMessageDriver = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMessageDriver()
                },
                onCancelRide = onCancelRide,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun RideStatusBar(
    status: RideStatus,
    etaMinutes: Int,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (status) {
        RideStatus.DriverArriving -> "Driver is on the way" to SafarColors.info
        RideStatus.WaitingForPickup -> "Driver has arrived" to SafarColors.success
        RideStatus.OnTheWay -> "On the way to destination" to SafarColors.primary
        RideStatus.Arriving -> "Arriving at destination" to SafarColors.success
        RideStatus.Completed -> "Ride completed" to SafarColors.success
    }

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = SafarColors.surfaceElevated.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated status indicator
            val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarColors.textPrimary
                )
                if (etaMinutes > 0) {
                    Text(
                        text = "ETA: $etaMinutes min",
                        fontSize = 12.sp,
                        color = SafarColors.textSecondary
                    )
                }
            }

            // Live indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SafarColors.error.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SafarColors.error)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RideInProgressBottomPanel(
    state: RideInProgressState,
    onCallDriver: () -> Unit,
    onMessageDriver: () -> Unit,
    onCancelRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SafarColors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 12.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SafarColors.divider)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════════
            // Driver Info Card
            // ══════════════════════════════════════════════════════════════
            DriverInfoCard(
                driverInfo = state.driverInfo,
                rideCode = state.rideCode,
                onCallDriver = onCallDriver,
                onMessageDriver = onMessageDriver
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════════
            // Vehicle Info
            // ══════════════════════════════════════════════════════════════
            VehicleInfoCard(
                vehicleModel = state.driverInfo.vehicleModel,
                vehicleColor = state.driverInfo.vehicleColor,
                licensePlate = state.driverInfo.licensePlate
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════════
            // Route Info
            // ══════════════════════════════════════════════════════════════
            RouteInfoCard(
                pickupAddress = state.pickupAddress,
                destinationAddress = state.destinationAddress,
                price = state.estimatedPrice,
                paymentMethod = state.paymentMethod
            )

            // Cancel option (only when driver is arriving)
            if (state.rideStatus == RideStatus.DriverArriving) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onCancelRide,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Cancel Ride",
                        color = SafarColors.error,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverInfoCard(
    driverInfo: DriverInfo,
    rideCode: String?,
    onCallDriver: () -> Unit,
    onMessageDriver: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Driver avatar
        SafarAvatar(
            imageUrl = driverInfo.photoUrl,
            name = driverInfo.name,
            size = 60.dp,
            showBadge = true,
            badgeColor = SafarColors.success
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Driver details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driverInfo.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SafarColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafarRatingStars(
                    rating = driverInfo.rating,
                    starSize = 14.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${driverInfo.rating}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafarColors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${driverInfo.totalTrips} trips",
                    fontSize = 12.sp,
                    color = SafarColors.textSecondary
                )
            }
            
            // Ride code if available
            rideCode?.let { code ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SafarColors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Code: $code",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarColors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Contact buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ContactButton(
                icon = Icons.Rounded.Message,
                onClick = onMessageDriver,
                backgroundColor = SafarColors.surfaceElevated
            )
            ContactButton(
                icon = Icons.Rounded.Call,
                onClick = onCallDriver,
                backgroundColor = SafarColors.success.copy(alpha = 0.15f),
                iconTint = SafarColors.success
            )
        }
    }
}

@Composable
private fun ContactButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconTint: Color = SafarColors.textPrimary
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VehicleInfoCard(
    vehicleModel: String,
    vehicleColor: String,
    licensePlate: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SafarColors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafarColors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    tint = SafarColors.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$vehicleColor $vehicleModel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafarColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap to see vehicle details",
                    fontSize = 12.sp,
                    color = SafarColors.textSecondary
                )
            }

            // License plate badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SafarColors.surface,
                border = BorderStroke(1.5.dp, SafarColors.divider)
            ) {
                Text(
                    text = licensePlate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun RouteInfoCard(
    pickupAddress: String,
    destinationAddress: String,
    price: String,
    paymentMethod: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SafarColors.surfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Route visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SafarColors.success)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        SafarColors.success,
                                        SafarColors.primary
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SafarColors.primary)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pickupAddress,
                        fontSize = 13.sp,
                        color = SafarColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = destinationAddress,
                        fontSize = 13.sp,
                        color = SafarColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SafarColors.divider)
            Spacer(modifier = Modifier.height(12.dp))

            // Payment info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (paymentMethod == "Cash") {
                            Icons.Rounded.Money
                        } else {
                            Icons.Rounded.CreditCard
                        },
                        contentDescription = null,
                        tint = SafarColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = paymentMethod,
                        fontSize = 13.sp,
                        color = SafarColors.textSecondary
                    )
                }
                Text(
                    text = price,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarColors.primary
                )
            }
        }
    }
}
