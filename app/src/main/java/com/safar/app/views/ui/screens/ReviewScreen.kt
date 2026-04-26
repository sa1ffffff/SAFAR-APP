package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.views.ui.theme.*

data class ReviewUiState(
    val revieweeName: String = "",
    val revieweeAvatar: String? = null,
    val rating: Int = 0,
    val comment: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Experience", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafarWhite,
                    titleContentColor = SafarTextDark
                )
            )
        },
        containerColor = SafarBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = SafarGreen
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // User Avatar
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(SafarGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.revieweeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafarGreen
                        )
                    }

                    Text(
                        text = uiState.revieweeName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarTextDark
                    )

                    Text(
                        text = "How was your ride?",
                        fontSize = 16.sp,
                        color = SafarTextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Star Rating
                    RatingStars(
                        rating = uiState.rating,
                        onRatingChange = onRatingChange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comment Field
                    OutlinedTextField(
                        value = uiState.comment,
                        onValueChange = onCommentChange,
                        label = { Text("Share your experience (optional)") },
                        placeholder = { Text("Tell us about your ride...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafarGreen,
                            unfocusedBorderColor = SafarDividerLight,
                            focusedContainerColor = SafarWhite,
                            unfocusedContainerColor = SafarWhite,
                            focusedTextColor = SafarTextDark,
                            unfocusedTextColor = SafarTextDark,
                            cursorColor = SafarGreen
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Submit Button
                    Button(
                        onClick = onSubmit,
                        enabled = uiState.rating > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafarGreen,
                            contentColor = SafarBlack,
                            disabledContainerColor = SafarDividerLight,
                            disabledContentColor = SafarTextMuted
                        )
                    ) {
                        Text(
                            "Submit Review",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Error message
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = SafarError
                ) {
                    Text(uiState.errorMessage)
                }
            }
        }
    }
}

@Composable
private fun RatingStars(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        for (i in 1..5) {
            StarButton(
                filled = i <= rating,
                onClick = { onRatingChange(i) }
            )
        }
    }
}

@Composable
private fun StarButton(
    filled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .background(
                if (filled) SafarGreen.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .border(
                width = 2.dp,
                color = if (filled) SafarGreen else SafarDividerLight,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
            contentDescription = null,
            tint = if (filled) SafarGreen else SafarTextMuted,
            modifier = Modifier.size(28.dp)
        )
    }
}
