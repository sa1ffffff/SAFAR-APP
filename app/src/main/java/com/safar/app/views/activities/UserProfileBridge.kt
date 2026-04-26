package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.models.Review
import com.safar.app.views.ui.screens.UserProfileScreen
import com.safar.app.views.ui.screens.UserProfileUiState
import com.safar.app.views.ui.screens.ReviewUiModel
import com.safar.app.views.ui.theme.SafarTheme
import com.safar.app.utils.DateUtils

/**
 * State holder for UserProfileActivity
 * Bridges Java activity with Kotlin Compose UI
 */
class UserProfileStateHolder {
    var userName by mutableStateOf("User")
    var bio by mutableStateOf("")
    var photoUrl by mutableStateOf<String?>(null)
    var rating by mutableFloatStateOf(0f)
    var ratingCount by mutableIntStateOf(0)
    var totalRides by mutableIntStateOf(0)
    var memberSince by mutableLongStateOf(0L)
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var reviews by mutableStateOf<List<Review>>(emptyList())
}

/**
 * Sets up Compose UI for UserProfileActivity
 */
fun setupComposeUI(activity: UserProfileActivity, stateHolder: UserProfileStateHolder) {
    activity.setContent {
        SafarTheme(darkTheme = false) {
            val reviewModels = stateHolder.reviews.map { review ->
                ReviewUiModel(
                    reviewId = review.reviewId ?: "",
                    reviewerName = review.reviewerName ?: "Anonymous",
                    reviewerPhoto = null,  // Review model doesn't store photo
                    rating = review.rating,
                    comment = review.comment ?: "",
                    timeAgo = DateUtils.timeAgo(review.timestamp)
                )
            }

            val uiState = UserProfileUiState(
                userName = stateHolder.userName,
                bio = stateHolder.bio,
                photoUrl = stateHolder.photoUrl,
                rating = stateHolder.rating,
                ratingCount = stateHolder.ratingCount,
                totalRides = stateHolder.totalRides,
                memberSince = if (stateHolder.memberSince > 0) {
                    DateUtils.formatMemberSince(stateHolder.memberSince)
                } else "",
                isLoading = stateHolder.isLoading,
                errorMessage = stateHolder.error,
                reviews = reviewModels
            )

            UserProfileScreen(
                state = uiState,
                onBack = { activity.finish() }
            )
        }
    }
}
