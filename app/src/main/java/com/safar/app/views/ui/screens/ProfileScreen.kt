package com.safar.app.views.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.safar.app.views.ui.theme.*

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    val bio: String = "",
    val avatarUrl: String? = null,
    val totalRides: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val memberSince: String = "",
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null,
    val isUploadingPhoto: Boolean = false
)

/**
 * SAFAR PROFILE SCREEN
 * Enterprise-level user profile with Uber-inspired design
 * 
 * Features:
 * - Clean avatar with initials or profile picture
 * - Stats display (rides, ratings)
 * - Action cards with icons
 * - Edit profile dialog with bio
 * - Premium logout button
 * - Light theme for clarity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState = ProfileUiState(),
    showLogoutDialog: Boolean = false,
    showEditDialog: Boolean = false,
    editName: String = "",
    editBio: String = "",
    onEditNameChange: (String) -> Unit = {},
    onEditBioChange: (String) -> Unit = {},
    onShowEditDialog: () -> Unit = {},
    onDismissEditDialog: () -> Unit = {},
    onSaveEdit: () -> Unit = {},
    onChangePhoto: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onMyRides: () -> Unit = {},
    onPaymentMethods: () -> Unit = {},
    onSavedPlaces: () -> Unit = {},
    onSafety: () -> Unit = {},
    onHelp: () -> Unit = {},
    onSettings: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
    onConfirmLogout: () -> Unit = {},
    onDismissLogout: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    SafarTheme(darkTheme = false) {
        Scaffold(
            containerColor = SafarBg,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Profile",
                            fontWeight = FontWeight.Bold,
                            color = SafarTextDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Rounded.ArrowBackIosNew,
                                "Back",
                                tint = SafarTextDark
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettings) {
                            Icon(
                                Icons.Rounded.Settings,
                                "Settings",
                                tint = SafarTextDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SafarWhite
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Avatar & Info Card ────────────────────────────────────────
                ProfileHeaderCard(
                    state = state,
                    onEditProfile = onShowEditDialog,
                    onAvatarClick = onAvatarClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Stats Section ─────────────────────────────────────────────
                StatsSection(state = state)

                Spacer(modifier = Modifier.height(24.dp))

                // ── Action Cards ──────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarTextMuted,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    ProfileActionCard(
                        icon = Icons.Rounded.DirectionsCar,
                        title = "My Rides",
                        subtitle = "View all your posted and joined rides",
                        onClick = onMyRides
                    )
                    
                    ProfileActionCard(
                        icon = Icons.Rounded.CreditCard,
                        title = "Payment Methods",
                        subtitle = "Manage your payment options",
                        onClick = onPaymentMethods
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SUPPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarTextMuted,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    ProfileActionCard(
                        icon = Icons.Rounded.Help,
                        title = "Help & Support",
                        subtitle = "FAQs and contact support",
                        onClick = onHelp
                    )
                    
                    ProfileActionCard(
                        icon = Icons.Rounded.Policy,
                        title = "Community Guidelines",
                        subtitle = "Read our community guidelines",
                        onClick = onSafety
                    )
                    
                    ProfileActionCard(
                        icon = Icons.Rounded.Info,
                        title = "About Safar",
                        subtitle = "Version 1.0.0",
                        onClick = onAbout
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Logout button
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, SafarError.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SafarError
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Logout,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Sign Out",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = onDismissLogout,
            title = { Text("Sign Out?") },
            text = { Text("Are you sure you want to sign out of your account?") },
            confirmButton = {
                TextButton(onClick = onConfirmLogout) {
                    Text("Sign Out", color = SafarError)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLogout) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit Profile Dialog
    if (showEditDialog) {
        EditProfileDialog(
            currentName = editName,
            currentBio = editBio,
            avatarUrl = state.avatarUrl,
            isUploading = state.isUploadingPhoto,
            onNameChange = onEditNameChange,
            onBioChange = onEditBioChange,
            onChangePhoto = onChangePhoto,
            onSave = onSaveEdit,
            onDismiss = onDismissEditDialog
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    state: ProfileUiState,
    onEditProfile: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with actual profile picture
            Box(
                modifier = Modifier.clickable { onAvatarClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SafarGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading || state.isUploadingPhoto) {
                        CircularProgressIndicator(
                            color = SafarGreen,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (!state.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = state.avatarUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = state.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarGreen
                        )
                    }
                }
                
                // Verified badge
                if (state.isVerified) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp),
                        shape = CircleShape,
                        color = SafarSuccess,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                null,
                                tint = SafarWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Text(
                text = state.name.ifEmpty { "Guest User" },
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = SafarTextDark
            )
            
            // Bio
            if (state.bio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.bio,
                    fontSize = 14.sp,
                    color = SafarTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contact info
            if (state.phone.isNotEmpty()) {
                Text(
                    text = state.phone,
                    fontSize = 14.sp,
                    color = SafarTextMuted
                )
            }
            
            if (!state.email.isNullOrEmpty()) {
                Text(
                    text = state.email,
                    fontSize = 14.sp,
                    color = SafarTextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            OutlinedButton(
                onClick = onEditProfile,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SafarDividerLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SafarTextDark
                )
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Edit Profile",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatsSection(state: ProfileUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rides Stat
        StatCard(
            icon = Icons.Rounded.DirectionsCar,
            value = state.totalRides.toString(),
            label = "Rides",
            color = SafarGreen,
            modifier = Modifier.weight(1f)
        )
        
        // Rating Stat
        StatCard(
            icon = Icons.Rounded.Star,
            value = if (state.ratingCount > 0) String.format("%.1f", state.rating) else "—",
            label = if (state.ratingCount > 0) "${state.ratingCount} reviews" else "No reviews",
            color = SafarWarning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SafarTextDark
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = SafarTextMuted
                )
            }
        }
    }
}

@Composable
private fun ProfileActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafarGreen.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = SafarGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SafarTextDark
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = SafarTextMuted
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = SafarTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Edit Profile Dialog
 * Allows users to update their name, bio, and profile picture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    avatarUrl: String?,
    isUploading: Boolean,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SafarWhite),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    "Edit Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SafarTextDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Profile picture with change option
                Box(
                    modifier = Modifier.clickable { onChangePhoto() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SafarGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = SafarGreen,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (!avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = currentName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafarGreen
                            )
                        }
                    }
                    
                    // Camera icon overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp),
                        shape = CircleShape,
                        color = SafarGreen,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CameraAlt,
                                null,
                                tint = SafarWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                
                Text(
                    "Tap to change photo",
                    fontSize = 12.sp,
                    color = SafarTextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Name field
                OutlinedTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafarGreen,
                        cursorColor = SafarGreen,
                        focusedLabelColor = SafarGreen
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Bio field
                OutlinedTextField(
                    value = currentBio,
                    onValueChange = onBioChange,
                    label = { Text("Bio") },
                    placeholder = { Text("Tell others about yourself...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafarGreen,
                        cursorColor = SafarGreen,
                        focusedLabelColor = SafarGreen
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SafarDividerLight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SafarTextMuted
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafarGreen,
                            contentColor = SafarBlack
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
