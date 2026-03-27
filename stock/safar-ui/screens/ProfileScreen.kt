package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
// SAFAR - Profile/Menu Screen
// Premium user profile and settings UI
// ══════════════════════════════════════════════════════════════════════════════

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val totalRides: Int = 0,
    val memberSince: String = "",
    val rating: Float = 0f,
    val savedAddresses: Int = 0,
    val hasActiveSubscription: Boolean = false,
    val walletBalance: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onBackClick: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onWallet: () -> Unit = {},
    onPaymentMethods: () -> Unit = {},
    onMyRides: () -> Unit = {},
    onSavedAddresses: () -> Unit = {},
    onPromos: () -> Unit = {},
    onSupport: () -> Unit = {},
    onSettings: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    SafarTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Profile",
                            fontWeight = FontWeight.Bold
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
                    actions = {
                        IconButton(onClick = onSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SafarColors.surface,
                        titleContentColor = SafarColors.textPrimary,
                        navigationIconContentColor = SafarColors.textPrimary,
                        actionIconContentColor = SafarColors.textPrimary
                    )
                )
            },
            containerColor = SafarColors.surface
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ══════════════════════════════════════════════════════════════
                // User Profile Card
                // ══════════════════════════════════════════════════════════════
                item {
                    UserProfileCard(
                        profile = userProfile,
                        onEditClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditProfile()
                        }
                    )
                }

                // ══════════════════════════════════════════════════════════════
                // Stats Row
                // ══════════════════════════════════════════════════════════════
                item {
                    UserStatsRow(
                        totalRides = userProfile.totalRides,
                        rating = userProfile.rating,
                        memberSince = userProfile.memberSince
                    )
                }

                // ══════════════════════════════════════════════════════════════
                // Wallet & Payments Section
                // ══════════════════════════════════════════════════════════════
                item {
                    SectionHeader(title = "Wallet & Payments")
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "Wallet",
                            subtitle = userProfile.walletBalance.ifEmpty { "Add money to wallet" },
                            onClick = onWallet,
                            showBadge = userProfile.walletBalance.isNotEmpty(),
                            badgeColor = SafarColors.success,
                            isFirst = true
                        )
                        MenuItemCard(
                            icon = Icons.Rounded.CreditCard,
                            title = "Payment Methods",
                            subtitle = "Manage your payment options",
                            onClick = onPaymentMethods,
                            isLast = true
                        )
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // Activity Section
                // ══════════════════════════════════════════════════════════════
                item {
                    SectionHeader(title = "Activity")
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Rounded.History,
                            title = "My Rides",
                            subtitle = "${userProfile.totalRides} rides",
                            onClick = onMyRides,
                            isFirst = true
                        )
                        MenuItemCard(
                            icon = Icons.Rounded.LocationOn,
                            title = "Saved Addresses",
                            subtitle = "${userProfile.savedAddresses} saved places",
                            onClick = onSavedAddresses
                        )
                        MenuItemCard(
                            icon = Icons.Rounded.LocalOffer,
                            title = "Promos & Discounts",
                            subtitle = "View available offers",
                            onClick = onPromos,
                            showBadge = true,
                            badgeColor = SafarColors.primary,
                            isLast = true
                        )
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // Support Section
                // ══════════════════════════════════════════════════════════════
                item {
                    SectionHeader(title = "Support")
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Rounded.Support,
                            title = "Help & Support",
                            subtitle = "FAQs, chat with us",
                            onClick = onSupport,
                            isFirst = true
                        )
                        MenuItemCard(
                            icon = Icons.Rounded.Info,
                            title = "About SAFAR",
                            subtitle = "Version, terms, privacy",
                            onClick = onAbout,
                            isLast = true
                        )
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // Logout Button
                // ══════════════════════════════════════════════════════════════
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SafarSecondaryButton(
                        text = "Log Out",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLogout()
                        },
                        leadingIcon = Icons.Rounded.Logout
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun UserProfileCard(
    profile: UserProfile,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SafarColors.surfaceElevated,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with edit overlay
            Box {
                SafarAvatar(
                    imageUrl = profile.photoUrl,
                    name = profile.name,
                    size = 72.dp
                )
                
                // Edit badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(24.dp)
                        .clickable(onClick = onEditClick),
                    shape = CircleShape,
                    color = SafarColors.primary,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = SafarColors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.email,
                        fontSize = 13.sp,
                        color = SafarColors.textSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = SafarColors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.phone,
                        fontSize = 13.sp,
                        color = SafarColors.textSecondary
                    )
                }

                // Subscription badge
                if (profile.hasActiveSubscription) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SafarColors.primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = SafarColors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Premium Member",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafarColors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserStatsRow(
    totalRides: Int,
    rating: Float,
    memberSince: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Rounded.DirectionsCar,
            value = "$totalRides",
            label = "Total Rides",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.Star,
            value = if (rating > 0) String.format("%.1f", rating) else "—",
            label = "Rating",
            iconTint = SafarColors.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.CalendarMonth,
            value = memberSince.ifEmpty { "—" },
            label = "Member Since",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = SafarColors.textSecondary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SafarColors.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SafarColors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = SafarColors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SafarColors.textTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun MenuItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showBadge: Boolean = false,
    badgeColor: Color = SafarColors.primary,
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
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SafarColors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SafarColors.textSecondary,
                    modifier = Modifier.size(20.dp)
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

            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = SafarColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
