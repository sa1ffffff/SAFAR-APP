package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.google.android.gms.maps.MapView
import com.safar.app.views.ui.SafarMapView
import com.safar.app.views.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR MAP SCREEN - UBER-INSPIRED PREMIUM UI
// Enterprise-level ride-hailing interface
// Material 3 + Modern UX patterns
// ══════════════════════════════════════════════════════════════════════════════

data class MapUiState(
    val pickupLocation: String = "",
    val destinationLocation: String = "",
    val ride1Route: String = "",
    val ride1Meta: String = "",
    val ride1Price: String = "",
    val ride1Time: String = "",
    val ride2Route: String = "",
    val ride2Meta: String = "",
    val ride2Price: String = "",
    val ride2Time: String = "",
    val showRide1: Boolean = false,
    val showRide2: Boolean = false,
    val isLoading: Boolean = false,
    val userName: String = "Guest",
    val userAvatarUrl: String? = null,
    val currentPromo: String? = null,
    val isOnline: Boolean = true,
    val notificationCount: Int = 0
)

/**
 * SAFAR MAP SCREEN
 * Premium ride-hailing interface matching Uber's design quality
 * 
 * Features:
 * - Glassmorphism navigation bar
 * - Floating search card with pickup/destination
 * - Premium bottom sheet with quick actions
 * - Smooth animations & haptic feedback
 * - Recent rides with rich cards
 * - Profile avatar with online indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: MapUiState = MapUiState(),
    onMapReady: (MapView) -> Unit = {},
    onPostRide: () -> Unit = {},
    onExplore: () -> Unit = {},
    onMyRides: () -> Unit = {},
    onMyLocation: () -> Unit = {},
    onSearchTap: () -> Unit = {},
    onSeeAllRides: () -> Unit = {},
    onRide1Click: () -> Unit = {},
    onRide2Click: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPromoClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    SafarTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ══════════════════════════════════════════════════════════════════
            // LAYER 1: Full-screen Google Map
            // ══════════════════════════════════════════════════════════════════
            SafarMapView(
                modifier = Modifier.fillMaxSize(),
                onMapReady = onMapReady
            )

            // ══════════════════════════════════════════════════════════════════
            // LAYER 2: Top Navigation Bar
            // ══════════════════════════════════════════════════════════════════
            TopNavigationBar(
                userName = state.userName,
                userAvatarUrl = state.userAvatarUrl,
                isOnline = state.isOnline,
                notificationCount = state.notificationCount,
                onMenuClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenuClick()
                },
                onNotificationsClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNotificationsClick()
                },
                onProfileClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onProfileClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )

            // ══════════════════════════════════════════════════════════════════
            // LAYER 3: Floating Search Card
            // ══════════════════════════════════════════════════════════════════
            FloatingSearchCard(
                pickupLocation = state.pickupLocation,
                destinationLocation = state.destinationLocation,
                onSearchTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSearchTap()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
                    .padding(horizontal = 16.dp)
            )

            // ══════════════════════════════════════════════════════════════════
            // LAYER 4: Promo Banner
            // ══════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = state.currentPromo != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 190.dp)
                    .padding(horizontal = 16.dp)
            ) {
                state.currentPromo?.let { promo ->
                    PromoBanner(
                        promoText = promo,
                        onClick = onPromoClick
                    )
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // LAYER 5: Floating Action Buttons
            // ══════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumFAB(
                    icon = Icons.Rounded.MyLocation,
                    contentDescription = "My Location",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMyLocation()
                    },
                    isPrimary = true
                )
                
                PremiumFAB(
                    icon = Icons.Rounded.Explore,
                    contentDescription = "Compass",
                    onClick = { /* Reset map rotation */ },
                    isPrimary = false
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // LAYER 6: Premium Bottom Sheet
            // ══════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                // Loading Indicator
                AnimatedVisibility(
                    visible = state.isLoading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    PremiumLoadingIndicator()
                }

                PremiumBottomSheet(
                    state = state,
                    onPostRide = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPostRide()
                    },
                    onExplore = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExplore()
                    },
                    onMyRides = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMyRides()
                    },
                    onSeeAllRides = onSeeAllRides,
                    onRide1Click = onRide1Click,
                    onRide2Click = onRide2Click
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TopNavigationBar(
    userName: String,
    userAvatarUrl: String?,
    isOnline: Boolean,
    notificationCount: Int,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu Button
        GlassButton(
            icon = Icons.Rounded.Menu,
            onClick = onMenuClick,
            contentDescription = "Menu"
        )

        // User Greeting Badge with Profile Avatar
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SafarDarkSurface.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SafarGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatarUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarGreen
                        )
                    }
                    
                    // Online indicator dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SafarDarkSurface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) SafarSuccess else SafarTextTertiary)
                    )
                }
                
                Text(
                    text = "Hello, $userName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafarTextPrimary
                )
            }
        }

        // Notifications Button
        GlassButton(
            icon = Icons.Rounded.Notifications,
            onClick = onNotificationsClick,
            contentDescription = "Notifications",
            badgeCount = notificationCount
        )
    }
}

@Composable
private fun GlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    badgeCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "buttonScale"
    )

    Box {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(14.dp),
            color = SafarDarkSurface.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = SafarTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        // Notification badge
        if (badgeCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(if (badgeCount > 9) 20.dp else 16.dp),
                shape = CircleShape,
                color = SafarError
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingSearchCard(
    pickupLocation: String,
    destinationLocation: String,
    onSearchTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 12.dp,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cardElevation"
    )

    Surface(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onSearchTap
        ),
        shape = RoundedCornerShape(20.dp),
        color = SafarDarkSurface,
        tonalElevation = 6.dp,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Pickup Location Row
            LocationInputRow(
                icon = Icons.Rounded.RadioButtonChecked,
                iconTint = SafarSuccess,
                label = "PICKUP",
                placeholder = "Enter pickup location",
                value = pickupLocation
            )

            // Connector dots
            Row(
                modifier = Modifier.padding(start = 11.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(SafarTextTertiary.copy(alpha = 0.5f))
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    color = SafarDivider
                )
            }

            // Destination Location Row
            LocationInputRow(
                icon = Icons.Rounded.LocationOn,
                iconTint = SafarGreen,
                label = "DESTINATION",
                placeholder = "Where to?",
                value = destinationLocation,
                showSchedule = true
            )
        }
    }
}

@Composable
private fun LocationInputRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    placeholder: String,
    value: String,
    showSchedule: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SafarTextTertiary,
                letterSpacing = 1.sp
            )
            Text(
                text = value.ifEmpty { placeholder },
                fontSize = 15.sp,
                fontWeight = if (value.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                color = if (value.isNotEmpty()) SafarTextPrimary else SafarTextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showSchedule) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SafarGreen.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "Schedule",
                    tint = SafarGreen,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PromoBanner(
    promoText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = SafarGreen.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, SafarGreen.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalOffer,
                contentDescription = null,
                tint = SafarGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = promoText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafarGreen,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = SafarGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PremiumFAB(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "fabScale"
    )

    Surface(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = SafarDarkSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isPrimary) SafarGreen else SafarTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PremiumLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        SafarGreen,
                        SafarGreen,
                        Color.Transparent
                    ),
                    startX = (progress - 0.3f) * 1000f,
                    endX = (progress + 0.3f) * 1000f
                )
            )
    )
}

@Composable
private fun PremiumBottomSheet(
    state: MapUiState,
    onPostRide: () -> Unit,
    onExplore: () -> Unit,
    onMyRides: () -> Unit,
    onSeeAllRides: () -> Unit,
    onRide1Click: () -> Unit,
    onRide2Click: () -> Unit
) {
    Surface(
        color = SafarDarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 12.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SafarDivider)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section Title
            Text(
                text = "QUICK ACTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SafarTextTertiary,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumActionTile(
                    icon = Icons.Rounded.AddCircleOutline,
                    label = "Post Ride",
                    subtitle = "Share your trip",
                    onClick = onPostRide,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
                PremiumActionTile(
                    icon = Icons.Rounded.Search,
                    label = "Find Ride",
                    subtitle = "Explore nearby",
                    onClick = onExplore,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
                PremiumActionTile(
                    icon = Icons.Rounded.History,
                    label = "My Rides",
                    subtitle = "View history",
                    onClick = onMyRides,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // Recent Rides Section
            AnimatedVisibility(
                visible = state.showRide1 || state.showRide2,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Rides",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SafarTextPrimary
                        )
                        TextButton(onClick = onSeeAllRides) {
                            Text(
                                text = "See all",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SafarGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = SafarGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.showRide1) {
                        PremiumRideCard(
                            route = state.ride1Route,
                            meta = state.ride1Meta,
                            price = state.ride1Price,
                            time = state.ride1Time,
                            onClick = onRide1Click
                        )
                    }
                    if (state.showRide2) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PremiumRideCard(
                            route = state.ride2Route,
                            meta = state.ride2Meta,
                            price = state.ride2Price,
                            time = state.ride2Time,
                            onClick = onRide2Click
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PremiumActionTile(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "tileScale"
    )

    val backgroundColor = if (isPrimary) {
        SafarGreen.copy(alpha = 0.12f)
    } else {
        SafarDarkCard
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        border = if (isPrimary) BorderStroke(1.dp, SafarGreen.copy(alpha = 0.25f)) else null,
        tonalElevation = if (isPrimary) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPrimary) SafarGreen.copy(alpha = 0.2f)
                        else SafarDarkSurface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) SafarGreen else SafarTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafarTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = SafarTextTertiary
            )
        }
    }
}

@Composable
private fun PremiumRideCard(
    route: String,
    meta: String,
    price: String,
    time: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cardElevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = SafarDarkCard,
        tonalElevation = 2.dp,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Route indicator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SafarSuccess)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(SafarSuccess, SafarGreen)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SafarGreen)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Route info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = SafarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = SafarTextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meta,
                        fontSize = 12.sp,
                        color = SafarTextTertiary,
                        maxLines = 1
                    )
                }
            }

            // Price & Time
            Column(horizontalAlignment = Alignment.End) {
                if (price.isNotEmpty()) {
                    Text(
                        text = price,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SafarGreen
                    )
                }
                if (time.isNotEmpty()) {
                    Text(
                        text = time,
                        fontSize = 11.sp,
                        color = SafarTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = SafarTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
