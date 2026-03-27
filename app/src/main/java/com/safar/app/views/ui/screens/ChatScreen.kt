package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

// ─── Data Models ─────────────────────────────────────────────────────────────
data class ChatMessageUiModel @JvmOverloads constructor(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long,
    val status: String = "sent",
    val senderName: String? = null
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ
}

/**
 * SAFAR CHAT SCREEN
 * Enterprise-level messaging interface with Uber-inspired design
 * 
 * Features:
 * - Modern chat bubbles with timestamps
 * - Message status indicators
 * - Safety disclaimer banner
 * - Smooth scroll animations
 * - Premium input bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerName: String = "Ride Partner",
    peerInitial: String = "",
    peerAvatar: String? = null,
    isOnline: Boolean = true,
    messages: List<ChatMessageUiModel> = emptyList(),
    disclaimerText: String = "Keep conversations respectful. Never share financial information.",
    isLoading: Boolean = false,
    isSending: Boolean = false,
    isPeerTyping: Boolean = false,
    typingUserName: String? = null,
    isConnected: Boolean = true,
    errorMessage: String? = null,
    onSend: (String) -> Unit = {},
    onRetry: (String) -> Unit = {},
    onDismissError: () -> Unit = {},
    onBack: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onCallPeer: () -> Unit = {},
    onReportIssue: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    SafarTheme(darkTheme = true) {
        Scaffold(
            containerColor = SafarBlack,
            topBar = {
                ChatTopBar(
                    peerName = peerName,
                    peerInitial = peerInitial.ifEmpty { 
                        peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?" 
                    },
                    isOnline = isOnline,
                    onBack = onBack,
                    onInfoClick = onInfoClick
                )
            },
            bottomBar = {
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    isSending = isSending,
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSend(inputText.trim())
                            inputText = ""
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Connection Status
                if (!isConnected) {
                    ConnectionBanner()
                }

                // Safety Disclaimer
                DisclaimerBanner(text = disclaimerText)

                // Messages List
                if (messages.isEmpty() && !isLoading) {
                    EmptyChatView(
                        peerName = peerName,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = messages,
                                key = { it.id }
                            ) { message ->
                                ChatBubble(message = message)
                            }
                        }
                        
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = SafarGreen
                            )
                        }
                    }
                }
                
                // Typing Indicator
                if (isPeerTyping) {
                    TypingIndicator(userName = typingUserName ?: peerName)
                }
                
                // Error SnackBar or Overlay
                if (errorMessage != null) {
                    ErrorBanner(message = errorMessage, onDismiss = onDismissError)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    peerName: String,
    peerInitial: String,
    isOnline: Boolean,
    onBack: () -> Unit,
    onInfoClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SafarGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        peerInitial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SafarGreen
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = peerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SafarTextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) SafarSuccess else SafarTextTertiary
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            fontSize = 12.sp,
                            color = SafarTextSecondary
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = SafarTextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Ride Info",
                    tint = SafarTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SafarDarkSurface
        )
    )
}

@Composable
private fun ConnectionBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafarError.copy(alpha = 0.9f)
    ) {
        Text(
            text = "Reconnecting...",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DisclaimerBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafarGreen.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = SafarGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = SafarGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun TypingIndicator(userName: String) {
    Text(
        text = "$userName is typing...",
        color = SafarTextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafarError,
        onClick = onDismiss
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun EmptyChatView(
    peerName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.ChatBubbleOutline,
            null,
            tint = SafarTextTertiary.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Start a conversation",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = SafarTextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Say hi to $peerName to coordinate your ride",
            fontSize = 13.sp,
            color = SafarTextTertiary
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessageUiModel) {
    val isMe = message.isMine

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isMe) 20.dp else 6.dp,
                        bottomEnd = if (isMe) 6.dp else 20.dp
                    ),
                    color = if (isMe) SafarGreen else SafarDarkCard
                ) {
                    Text(
                        text = message.text,
                        color = if (isMe) SafarBlack else SafarTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp and status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    // Simple timestamp formatting
                    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                    Text(
                        text = time,
                        color = SafarTextTertiary,
                        fontSize = 10.sp
                    )
                    
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (message.status.lowercase()) {
                                "sending" -> Icons.Rounded.Schedule
                                "sent" -> Icons.Rounded.Check
                                "delivered" -> Icons.Rounded.DoneAll
                                "read" -> Icons.Rounded.DoneAll
                                else -> Icons.Rounded.Check
                            },
                            contentDescription = null,
                            tint = if (message.status.lowercase() == "read")
                                SafarGreen else SafarTextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit
) {
    val canSend = value.isNotBlank() && !isSending

    Surface(
        color = SafarDarkSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text input
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Type a message...",
                        color = SafarTextTertiary,
                        fontSize = 15.sp
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SafarGreen.copy(alpha = 0.5f),
                    unfocusedBorderColor = SafarDivider,
                    focusedContainerColor = SafarDarkCard,
                    unfocusedContainerColor = SafarDarkCard,
                    focusedTextColor = SafarTextPrimary,
                    unfocusedTextColor = SafarTextPrimary,
                    cursorColor = SafarGreen
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
            )

            // Send button
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = canSend) { onSend() },
                shape = RoundedCornerShape(16.dp),
                color = if (canSend) SafarGreen else SafarDarkCard
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = SafarBlack,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (canSend) SafarBlack else SafarTextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
