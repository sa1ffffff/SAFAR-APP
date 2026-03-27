package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

// ── UI models ─────────────────────────────────────────────────────────────────
enum class InboxItemType { CHAT, JOIN_REQUEST }

data class InboxItem(
    val id: String,
    val type: InboxItemType,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val adId: String = "",
    val chatRoomId: String = "",
    val peerName: String = "",
    val statusLabel: String = "",
    val isRequest: Boolean = false
)

data class InboxUiState(
    val items: List<InboxItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * SAFAR INBOX SCREEN
 * Enterprise-level messaging interface with Uber-inspired design
 * 
 * Features:
 * - Sectioned list (Requests & Messages)
 * - Unread badges with count
 * - Status chips for requests
 * - Premium empty state
 * - Pull-to-refresh ready
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxUiState = InboxUiState(),
    onChatOpen: (String, String) -> Unit = { _, _ -> },
    onRequestOpen: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    SafarTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SafarBlack)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Surface(
                color = SafarDarkSurface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBackIosNew,
                                "Back",
                                tint = SafarTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Inbox",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = SafarTextPrimary
                            )
                            val unread = state.items.sumOf { it.unreadCount }
                            if (unread > 0) {
                                Text(
                                    "$unread unread message${if (unread > 1) "s" else ""}",
                                    fontSize = 12.sp,
                                    color = SafarGreen
                                )
                            }
                        }
                    }
                    
                    // Filter/Settings button
                    IconButton(onClick = { /* Filter messages */ }) {
                        Icon(
                            Icons.Rounded.FilterList,
                            null,
                            tint = SafarTextSecondary
                        )
                    }
                }
            }

            // ── Content ────────────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = SafarGreen,
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading messages...",
                                color = SafarTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                state.items.isEmpty() -> {
                    EmptyInboxView(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    val chats = state.items.filter { it.type == InboxItemType.CHAT }
                    val requests = state.items.filter { it.type == InboxItemType.JOIN_REQUEST }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Join Requests Section
                        if (requests.isNotEmpty()) {
                            item {
                                InboxSectionHeader(
                                    title = "RIDE REQUESTS",
                                    count = requests.size
                                )
                            }
                            items(requests, key = { it.id }) { item ->
                                InboxRequestRow(
                                    item = item,
                                    onClick = { onRequestOpen(item.adId) }
                                )
                            }
                        }

                        // Messages Section
                        if (chats.isNotEmpty()) {
                            item {
                                InboxSectionHeader(
                                    title = "MESSAGES",
                                    count = chats.size
                                )
                            }
                            items(chats, key = { it.id }) { item ->
                                InboxChatRow(
                                    item = item,
                                    onClick = { onChatOpen(item.chatRoomId, item.peerName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = SafarTextTertiary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SafarGreen.copy(alpha = 0.12f)
        ) {
            Text(
                text = count.toString(),
                color = SafarGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun InboxChatRow(item: InboxItem, onClick: () -> Unit) {
    val hasUnread = item.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(SafarGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SafarGreen
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.title,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = SafarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    item.timestamp,
                    fontSize = 11.sp,
                    color = if (hasUnread) SafarGreen else SafarTextTertiary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.subtitle,
                    fontSize = 13.sp,
                    color = if (hasUnread) SafarTextSecondary else SafarTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = SafarGreen
                    ) {
                        Text(
                            text = if (item.unreadCount > 9) "9+" else item.unreadCount.toString(),
                            color = SafarBlack,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
    
    HorizontalDivider(
        color = SafarDivider.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 86.dp)
    )
}

@Composable
private fun InboxRequestRow(item: InboxItem, onClick: () -> Unit) {
    val statusColor = when (item.statusLabel.lowercase()) {
        "accepted" -> StatusOpen
        "rejected" -> StatusFilled
        else -> SafarWarning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SafarDarkCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.DirectionsCar,
                null,
                tint = SafarGreen,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = SafarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    item.timestamp,
                    fontSize = 11.sp,
                    color = SafarTextTertiary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    item.subtitle,
                    fontSize = 12.sp,
                    color = SafarTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (item.statusLabel.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            item.statusLabel,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = SafarTextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
    
    HorizontalDivider(
        color = SafarDivider.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 86.dp)
    )
}

@Composable
private fun EmptyInboxView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SafarDarkCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Inbox,
                null,
                tint = SafarTextTertiary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "No messages yet",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = SafarTextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Your conversations and ride requests\nwill appear here",
            fontSize = 14.sp,
            color = SafarTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
