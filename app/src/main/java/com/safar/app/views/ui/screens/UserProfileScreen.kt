package com.safar.app.views.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.safar.app.views.ui.theme.*

/**
 * UI Model for displaying a review
 */
data class ReviewUiModel(
    val reviewId: String,
    val reviewerName: String,
    val reviewerPhoto: String?,
    val rating: Int,
    val comment: String,
    val timeAgo: String
)

/**
 * UI State for UserProfileScreen
 */
data class UserProfileUiState(
    val userName: String = "User",
    val bio: String = "",
    val photoUrl: String? = null,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val totalRides: Int = 0,
    val memberSince: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reviews: List<ReviewUiModel> = emptyList()
)

/**
 * SAFAR User Profile Screen
 * 
 * Displays another user's public profile with:
 * - Profile picture and name
 * - Bio
 * - Stats (rating, rides)
 * - Reviews from other users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    state: UserProfileUiState,
    onBack: () -> Unit
) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafarWhite
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SafarGreen)
            }
        } else if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.PersonOff,
                        contentDescription = null,
                        tint = SafarTextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        state.errorMessage,
                        color = SafarTextMuted,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header Card
                item {
                    ProfileHeaderCard(state)
                }

                // Bio Card (if bio exists)
                if (state.bio.isNotEmpty()) {
                    item {
                        BioCard(state.bio)
                    }
                }

                // Stats Card
                item {
                    StatsCard(state)
                }

                // Reviews Section
                item {
                    Text(
                        text = "Reviews",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SafarTextDark,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.reviews.isEmpty()) {
                    item {
                        EmptyReviewsCard()
                    }
                } else {
                    items(state.reviews) { review ->
                        ReviewItemCard(review)
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(state: UserProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SafarGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!state.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = state.photoUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = state.userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Text(
                text = state.userName,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = SafarTextDark
            )

            // Member since
            if (state.memberSince.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Member since ${state.memberSince}",
                    fontSize = 14.sp,
                    color = SafarTextMuted
                )
            }

            // Rating
            if (state.ratingCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", state.rating),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SafarTextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${state.ratingCount} reviews)",
                        fontSize = 14.sp,
                        color = SafarTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun BioCard(bio: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "About",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = SafarTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bio,
                fontSize = 15.sp,
                color = SafarTextDark,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun StatsCard(state: UserProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Total Rides
            StatItem(
                icon = Icons.Rounded.DirectionsCar,
                value = state.totalRides.toString(),
                label = "Rides"
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(SafarDividerLight)
            )

            // Rating
            StatItem(
                icon = Icons.Rounded.Star,
                value = if (state.ratingCount > 0) String.format("%.1f", state.rating) else "-",
                label = "Rating"
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(SafarDividerLight)
            )

            // Reviews
            StatItem(
                icon = Icons.Rounded.RateReview,
                value = state.ratingCount.toString(),
                label = "Reviews"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = SafarGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = SafarTextDark
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = SafarTextMuted
        )
    }
}

@Composable
private fun EmptyReviewsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.RateReview,
                contentDescription = null,
                tint = SafarTextLight,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No reviews yet",
                fontSize = 16.sp,
                color = SafarTextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reviews will appear here after completed rides",
                fontSize = 13.sp,
                color = SafarTextLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReviewItemCard(review: ReviewUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reviewer Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SafarGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!review.reviewerPhoto.isNullOrEmpty()) {
                        AsyncImage(
                            model = review.reviewerPhoto,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = review.reviewerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.reviewerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SafarTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = review.timeAgo,
                        fontSize = 12.sp,
                        color = SafarTextLight
                    )
                }

                // Star rating
                Row {
                    repeat(5) { index ->
                        Icon(
                            if (index < review.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFB800) else SafarTextLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Comment
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review.comment,
                    fontSize = 14.sp,
                    color = SafarTextDark,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
