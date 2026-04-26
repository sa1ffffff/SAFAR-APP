package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.NotificationUiModel
import com.safar.app.views.ui.screens.NotificationsScreen
import com.safar.app.views.ui.screens.NotificationsUiState
import com.safar.app.views.ui.theme.SafarTheme

/**
 * State holder for NotificationsActivity
 * Bridges Java activity with Kotlin Compose UI
 *
 * Note: Manual setters are not needed as Kotlin generates them for var properties,
 * which are then accessible from Java. Manual setters with same names cause
 * "Platform declaration clash".
 */
class NotificationsStateHolder {
    var notifications by mutableStateOf<List<NotificationUiModel>>(emptyList())
    var unreadCount by mutableIntStateOf(0)
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
}

/**
 * Sets up Compose UI for NotificationsActivity
 */
fun setupComposeUI(activity: NotificationsActivity, stateHolder: NotificationsStateHolder) {
    activity.setContent {
        SafarTheme {
            val uiState = NotificationsUiState(
                notifications = stateHolder.notifications,
                unreadCount = stateHolder.unreadCount,
                isLoading = stateHolder.isLoading,
                errorMessage = stateHolder.error
            )

            NotificationsScreen(
                uiState = uiState,
                onBack = { activity.finish() },
                onNotificationClick = { notification ->
                    activity.handleNotificationClick(notification)
                },
                onMarkAllRead = {
                    activity.handleMarkAllRead()
                },
                onUserClick = { userId ->
                    activity.navigateToUserProfile(userId)
                }
            )
        }
    }
}
