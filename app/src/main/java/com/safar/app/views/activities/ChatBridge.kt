package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.ChatMessageUiModel
import com.safar.app.views.ui.screens.ChatScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CHAT BRIDGE
// Connects Java ChatActivity to Jetpack Compose ChatScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Enhanced chat message model with additional metadata for UI.
 * Renamed to ChatBridgeMessage to avoid collision with com.safar.app.models.ChatMessage
 */
data class ChatBridgeMessage(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long,
    val status: String = "sent",
    val senderName: String? = null,
    val senderAvatar: String? = null
) {
    /**
     * Converts to the UI model used by ChatScreen.
     */
    fun toUiModel(): ChatMessageUiModel = ChatMessageUiModel(
        id = id,
        text = text,
        isMine = isFromMe,
        timestamp = timestamp,
        status = status,
        senderName = senderName
    )
}

/**
 * State holder for chat screen.
 */
class ChatStateHolder {
    
    /** Observable list of chat messages */
    val messages = mutableStateListOf<ChatMessageUiModel>()
    
    /** Loading state for initial message fetch */
    var isLoading by mutableStateOf(false)
    
    /** Whether a message is currently being sent */
    var isSending by mutableStateOf(false)
    
    /** Whether the peer is currently typing */
    var isPeerTyping by mutableStateOf(false)
    
    /** Name of the peer who is typing */
    var typingUserName by mutableStateOf<String?>(null)
    
    /** Error message to display */
    var errorMessage by mutableStateOf<String?>(null)
    
    /** Whether the chat is connected/online */
    var isConnected by mutableStateOf(true)
    
    fun addMessage(msg: ChatMessageUiModel) {
        messages.add(msg)
    }
    
    fun setMessages(list: List<ChatMessageUiModel>) {
        messages.clear()
        messages.addAll(list)
        isLoading = false
    }
    
    fun updateMessageStatus(messageId: String, status: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages[index] = messages[index].copy(status = status)
        }
    }
    
    fun setPeerTyping(isTyping: Boolean, userName: String? = null) {
        isPeerTyping = isTyping
        typingUserName = userName
    }
    
    fun setError(message: String) {
        errorMessage = message
        isSending = false
    }
    
    fun clearError() {
        errorMessage = null
    }
    
    fun startSending() {
        isSending = true
        clearError()
    }
    
    fun finishSending() {
        isSending = false
    }
}

/**
 * Sets up the Compose UI for ChatActivity.
 */
fun ChatActivity.setupComposeUI(
    stateHolder: ChatStateHolder,
    peerName: String,
    disclaimerText: String
) {
    setContent {
        SafarTheme(darkTheme = true) {
            ChatScreen(
                peerName = peerName,
                peerAvatar = null,
                messages = stateHolder.messages,
                isLoading = stateHolder.isLoading,
                isSending = stateHolder.isSending,
                isPeerTyping = stateHolder.isPeerTyping,
                typingUserName = stateHolder.typingUserName,
                isConnected = stateHolder.isConnected,
                errorMessage = stateHolder.errorMessage,
                disclaimerText = disclaimerText,
                onSend = { text ->
                    if (text.isNotBlank()) {
                        stateHolder.startSending()
                        handleSend(text.trim())
                    }
                },
                onRetry = { messageId ->
                    handleRetry(messageId)
                },
                onDismissError = {
                    stateHolder.clearError()
                },
                onBack = { onBackPressedDispatcher.onBackPressed() },
                onCallPeer = { handleCallPeer() },
                onReportIssue = { handleReportIssue() },
                onAvatarClick = { navigateToUserProfile() }
            )
        }
    }
}
