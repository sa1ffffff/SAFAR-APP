package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.ReviewScreen
import com.safar.app.views.ui.screens.ReviewUiState
import com.safar.app.views.ui.theme.SafarTheme

/**
 * State holder for review screen
 */
class ReviewStateHolder {
    
    private var _uiState by mutableStateOf(ReviewUiState())
    val uiState: ReviewUiState get() = _uiState
    
    fun setRevieweeName(name: String) {
        _uiState = _uiState.copy(revieweeName = name)
    }
    
    fun setRevieweeAvatar(avatar: String?) {
        _uiState = _uiState.copy(revieweeAvatar = avatar)
    }
    
    fun setRating(rating: Int) {
        _uiState = _uiState.copy(rating = rating)
    }
    
    fun getRating(): Int = _uiState.rating
    
    fun setComment(comment: String) {
        _uiState = _uiState.copy(comment = comment)
    }
    
    fun getComment(): String = _uiState.comment
    
    fun setLoading(loading: Boolean) {
        _uiState = _uiState.copy(isLoading = loading)
    }
    
    fun setError(error: String?) {
        _uiState = _uiState.copy(errorMessage = error)
    }
}

/**
 * Sets up the Compose UI for ReviewActivity
 */
fun ReviewActivity.setupComposeUI(stateHolder: ReviewStateHolder) {
    setContent {
        SafarTheme(darkTheme = false) {
            ReviewScreen(
                uiState = stateHolder.uiState,
                onRatingChange = { rating ->
                    stateHolder.setRating(rating)
                },
                onCommentChange = { comment ->
                    stateHolder.setComment(comment)
                },
                onSubmit = {
                    handleSubmitReview()
                },
                onBack = {
                    onBackPressedDispatcher.onBackPressed()
                }
            )
        }
    }
}
