package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.views.ui.theme.*

/**
 * UI Model for notification display
 */
data class NotificationUiModel(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val fromUserId: String?,  // Added for profile navigation
    val fromUserName: String,
    val fromUserPhoto: String?,
    val relatedAdId: String?,
    val relatedRequestId: String?,
    val timestamp: Long,
    val isRead: Boolean,
    val timeAgo: String
)

data class NotificationsUiState(
    val notifications: List<NotificationUiModel> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onBack: () -> Unit,
    onNotificationClick: (NotificationUiModel) -> Unit,
    onMarkAllRead: () -> Unit,
    onUserClick: (String) -> Unit = {}  // Added for profile navigation
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = onMarkAllRead) {
                            Text(
                                "Mark all read",
                                color = SafarGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SafarGreen
                    )
                }
                uiState.notifications.isEmpty() -> {
                    EmptyNotificationsView(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // Today section
                        val todayNotifications = uiState.notifications.filter { 
                            isToday(it.timestamp) 
                        }
                        if (todayNotifications.isNotEmpty()) {
                            item {
                                SectionHeader("Today")
                            }
                            items(todayNotifications, key = { it.id }) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = { onNotificationClick(notification) },
                                    onAvatarClick = { notification.fromUserId?.let { onUserClick(it) } }
                                )
                            }
                        }
                        
                        // This Week section
                        val thisWeekNotifications = uiState.notifications.filter { 
                            !isToday(it.timestamp) && isThisWeek(it.timestamp) 
                        }
                        if (thisWeekNotifications.isNotEmpty()) {
                            item {
                                SectionHeader("This Week")
                            }
                            items(thisWeekNotifications, key = { it.id }) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = { onNotificationClick(notification) },
                                    onAvatarClick = { notification.fromUserId?.let { onUserClick(it) } }
                                )
                            }
                        }
                        
                        // Earlier section
                        val earlierNotifications = uiState.notifications.filter { 
                            !isToday(it.timestamp) && !isThisWeek(it.timestamp) 
                        }
                        if (earlierNotifications.isNotEmpty()) {
                            item {
                                SectionHeader("Earlier")
                            }
                            items(earlierNotifications, key = { it.id }) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = { onNotificationClick(notification) },
                                    onAvatarClick = { notification.fromUserId?.let { onUserClick(it) } }
                                )
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = SafarTextDark,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun NotificationItem(
    notification: NotificationUiModel,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val backgroundColor = if (notification.isRead) SafarWhite else SafarGreen.copy(alpha = 0.05f)
    val iconData = getNotificationIcon(notification.type)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // User Avatar with notification type indicator - Now clickable
            Box(
                modifier = Modifier.clickable { onAvatarClick() }
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SafarGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.fromUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarGreen
                    )
                }
                
                // Type indicator badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(iconData.second),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconData.first,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SafarTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Unread indicator dot
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SafarGreen)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    color = SafarTextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = notification.timeAgo,
                    color = SafarTextLight,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SafarGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = SafarGreen,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "No notifications yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = SafarTextDark
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "When you get notifications, they'll show up here",
            color = SafarTextMuted,
            fontSize = 14.sp
        )
    }
}

/**
 * Returns icon and color based on notification type
 */
private fun getNotificationIcon(type: String): Pair<ImageVector, Color> {
    return when (type) {
        "REQUEST_ACCEPTED" -> Pair(Icons.Rounded.CheckCircle, Color(0xFF4CAF50))
        "REQUEST_DECLINED" -> Pair(Icons.Rounded.Cancel, Color(0xFFEF5350))
        "NEW_REQUEST" -> Pair(Icons.Rounded.PersonAdd, Color(0xFF2196F3))
        "RIDE_COMPLETED" -> Pair(Icons.Rounded.DirectionsCar, Color(0xFF9C27B0))
        "NEW_REVIEW" -> Pair(Icons.Rounded.Star, Color(0xFFFF9800))
        "NEW_MESSAGE" -> Pair(Icons.Rounded.Chat, Color(0xFF00BCD4))
        else -> Pair(Icons.Rounded.Notifications, SafarGreen)
    }
}

/**
 * Helper function to check if timestamp is today
 */
private fun isToday(timestamp: Long): Boolean {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return diff < 24 * 60 * 60 * 1000
}

/**
 * Helper function to check if timestamp is within this week
 */
private fun isThisWeek(timestamp: Long): Boolean {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return diff < 7 * 24 * 60 * 60 * 1000
}
