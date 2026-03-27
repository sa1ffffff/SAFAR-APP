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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.android.gms.maps.MapView
import com.safar.app.views.ui.SafarMapView
import com.safar.app.views.ui.theme.*
import com.safar.app.views.ui.components.*
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Driver Searching Screen
// Premium loading experience while finding a driver
// ══════════════════════════════════════════════════════════════════════════════

data class DriverSearchingState(
    val searchingStatus: SearchingStatus = SearchingStatus.Searching,
    val rideType: String = "",
    val estimatedPrice: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val searchingMessage: String = "Looking for nearby drivers...",
    val driversNearby: Int = 0
)

enum class SearchingStatus {
    Searching,
    DriverFound,
    NoDriversAvailable,
    Cancelled
}

@Composable
fun DriverSearchingScreen(
    state: DriverSearchingState,
    onMapReady: (MapView) -> Unit = {},
    onCancelRide: () -> Unit = {},
    onContactSupport: () -> Unit = {}
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
            // Dark overlay for better visibility
            // ══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                SafarColors.surface.copy(alpha = 0.7f),
                                SafarColors.surface.copy(alpha = 0.95f)
                            ),
                            startY = 0f,
                            endY = 800f
                        )
                    )
            )

            // ══════════════════════════════════════════════════════════════
            // Searching Animation Center
            // ══════════════════════════════════════════════════════════════
            SearchingAnimation(
                modifier = Modifier.align(Alignment.Center)
            )

            // ══════════════════════════════════════════════════════════════
            // Bottom Panel
            // ══════════════════════════════════════════════════════════════
            SearchingBottomPanel(
                state = state,
                onCancelRide = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCancelRide()
                },
                onContactSupport = onContactSupport,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SearchingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "searchPulse")
    
    // Multiple pulsing rings
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3Scale"
    )
    
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3Alpha"
    )

    // Car rotation
    val carRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "carRotation"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing rings
        listOf(
            ring1Scale to ring1Alpha,
            ring2Scale to ring2Alpha,
            ring3Scale to ring3Alpha
        ).forEach { (scale, alpha) ->
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(SafarColors.primary.copy(alpha = alpha))
            )
        }

        // Center circle
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = SafarColors.surfaceElevated,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Rotating car icons around center
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(carRotation)
                ) {
                    // Multiple car indicators
                    listOf(0f, 72f, 144f, 216f, 288f).forEachIndexed { index, angle ->
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = SafarColors.primary.copy(alpha = 0.6f + (index * 0.08f)),
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopCenter)
                                .rotate(angle)
                                .offset(y = (-30).dp)
                        )
                    }
                }
                
                // Center icon
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = SafarColors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchingBottomPanel(
    state: DriverSearchingState,
    onCancelRide: () -> Unit,
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated dots for searching text
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }
    val dots = ".".repeat(dotCount)

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            SafarBottomSheetHandle()

            Spacer(modifier = Modifier.height(24.dp))

            // Status message
            Text(
                text = when (state.searchingStatus) {
                    SearchingStatus.Searching -> "${state.searchingMessage}$dots"
                    SearchingStatus.DriverFound -> "Driver found!"
                    SearchingStatus.NoDriversAvailable -> "No drivers available nearby"
                    SearchingStatus.Cancelled -> "Ride cancelled"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafarColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Drivers nearby info
            if (state.searchingStatus == SearchingStatus.Searching && state.driversNearby > 0) {
                Text(
                    text = "${state.driversNearby} drivers nearby",
                    fontSize = 14.sp,
                    color = SafarColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ride details card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SafarColors.surfaceElevated
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsCar,
                                contentDescription = null,
                                tint = SafarColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.rideType,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SafarColors.textPrimary
                            )
                        }
                        Text(
                            text = state.estimatedPrice,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarColors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SafarColors.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Route mini display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SafarColors.success)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(SafarColors.divider)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SafarColors.primary)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.pickupAddress,
                                fontSize = 13.sp,
                                color = SafarColors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.destinationAddress,
                                fontSize = 13.sp,
                                color = SafarColors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SafarSecondaryButton(
                    text = "Contact Support",
                    onClick = onContactSupport,
                    leadingIcon = Icons.Rounded.Support,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel button
            TextButton(
                onClick = onCancelRide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel Ride",
                    color = SafarColors.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
