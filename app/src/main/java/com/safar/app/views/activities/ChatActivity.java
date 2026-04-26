package com.safar.app.views.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.models.ChatMessage;
import com.safar.app.models.Conversation;
import com.safar.app.presenters.ChatPresenter;
import com.safar.app.presenters.ConversationPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;
import com.safar.app.views.ui.screens.ChatMessageUiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * SAFAR CHAT ACTIVITY
 * 
 * Handles both ride-linked chats (legacy) and user-to-user direct messaging (new).
 * When KEY_OTHER_USER_ID is present, uses ConversationPresenter for direct messaging.
 * Otherwise, falls back to ChatPresenter for ride-linked chats.
 */
public class ChatActivity extends AppCompatActivity 
        implements ChatPresenter.ChatView, ConversationPresenter.ConversationView {

    private ChatPresenter chatPresenter;
    private ConversationPresenter conversationPresenter;
    private String currentUid;
    private String chatRoomId;
    private String requestId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhoto;
    private SessionManager session;
    private ChatStateHolder stateHolder;
    
    // Flag to determine which messaging system to use
    private boolean isDirectMessaging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateHolder = new ChatStateHolder();
        otherUserName = getIntent().getStringExtra(AppConstants.KEY_OTHER_USER_NAME);
        otherUserId = getIntent().getStringExtra(AppConstants.KEY_OTHER_USER_ID);
        otherUserPhoto = getIntent().getStringExtra(AppConstants.KEY_OTHER_USER_PHOTO);
        
        ChatBridgeKt.setupComposeUI(this, stateHolder, 
                otherUserName != null ? otherUserName : "Ride Partner", 
                "Keep conversations respectful. Do not share personal financial info.");

        chatRoomId = getIntent().getStringExtra(AppConstants.KEY_CHAT_ROOM_ID);
        requestId = getIntent().getStringExtra(AppConstants.KEY_REQUEST_ID);

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        session = new SessionManager(this);

        // Determine messaging mode
        isDirectMessaging = (otherUserId != null && !otherUserId.isEmpty());
        
        if (isDirectMessaging) {
            // Use new user-to-user direct messaging
            conversationPresenter = new ConversationPresenter(this);
            initializeDirectMessaging();
        } else {
            // Use legacy ride-linked chat
            chatPresenter = new ChatPresenter(this, chatRoomId, requestId);
            chatPresenter.startListening();
        }
    }
    
    /**
     * Initialize direct messaging conversation.
     * Creates conversation if it doesn't exist, then starts listening.
     */
    private void initializeDirectMessaging() {
        if (currentUid == null || otherUserId == null) return;
        
        // Get or create the conversation
        conversationPresenter.getOrCreateConversation(
                currentUid,
                otherUserId,
                session.getUserName(),
                otherUserName,
                session.getUserPhotoUrl(),
                otherUserPhoto
        );
    }

    public void handleSend(String text) {
        if (text.isEmpty()) return;
        
        if (isDirectMessaging && conversationPresenter != null) {
            // Send via direct messaging
            conversationPresenter.sendMessage(
                    chatRoomId,
                    currentUid,
                    session.getUserName(),
                    session.getUserPhotoUrl(),
                    otherUserId,
                    text
            );
        } else if (chatPresenter != null) {
            // Send via legacy chat
            ChatMessage msg = new ChatMessage(currentUid, session.getUserName(), text, ChatMessage.TYPE_TEXT);
            chatPresenter.sendMessage(msg);
        }
        stateHolder.finishSending();
    }

    public void handleRetry(String messageId) {
        Toast.makeText(this, "Retrying message...", Toast.LENGTH_SHORT).show();
    }

    public void handleCallPeer() {
        Toast.makeText(this, "Calling feature coming soon", Toast.LENGTH_SHORT).show();
    }

    public void handleReportIssue() {
        Toast.makeText(this, "Report issue feature coming soon", Toast.LENGTH_SHORT).show();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ChatPresenter.ChatView Implementation (Legacy)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void onMessageReceived(com.safar.app.models.ChatMessage message) {
        addMessageToUI(message);
        
        // Mark as read if from other user
        if (!message.getSenderId().equals(currentUid)) {
            if (isDirectMessaging && conversationPresenter != null) {
                conversationPresenter.markAsRead(chatRoomId, currentUid);
            } else if (chatPresenter != null) {
                chatPresenter.markAsRead();
            }
        }
    }

    @Override
    public void onMessagesLoaded(List<com.safar.app.models.ChatMessage> messages) {
        List<ChatMessageUiModel> uiModels = new ArrayList<>();
        if (messages != null) {
            for (com.safar.app.models.ChatMessage m : messages) {
                String msgId = m.getMessageId() != null ? m.getMessageId() : String.valueOf(m.getTimestamp());
                uiModels.add(new ChatMessageUiModel(
                        msgId,
                        m.getText(),
                        m.getSenderId().equals(currentUid),
                        m.getTimestamp(),
                        m.isRead() ? "read" : "sent",
                        m.getSenderName()
                ));
            }
        }
        stateHolder.setMessages(uiModels);
    }

    @Override 
    public void onMessageSent() {}
    
    @Override 
    public void onError(String message) {
        stateHolder.setError(message);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ConversationPresenter.ConversationView Implementation (New Direct Messaging)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void onConversationsLoaded(List<Conversation> conversations) {
        // Not used in chat screen - handled by InboxFragment
    }

    @Override
    public void onConversationCreated(String conversationId) {
        // Conversation is ready, store the ID and start listening
        this.chatRoomId = conversationId;
        conversationPresenter.startListeningMessages(conversationId);
        conversationPresenter.markAsRead(conversationId, currentUid);
    }

    @Override
    public void onUnreadCountUpdated(int totalUnread) {
        // Not used in chat screen - handled by MainBridge
    }
    
    /**
     * Helper to add a message to the UI.
     */
    private void addMessageToUI(ChatMessage message) {
        String msgId = message.getMessageId() != null ? message.getMessageId() : String.valueOf(message.getTimestamp());
        
        // Check if message already exists to avoid duplicates
        boolean exists = false;
        for (ChatMessageUiModel m : stateHolder.getMessages()) {
            if (m.getId().equals(msgId)) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            stateHolder.addMessage(new ChatMessageUiModel(
                    msgId,
                    message.getText(),
                    message.getSenderId().equals(currentUid),
                    message.getTimestamp(),
                    message.isRead() ? "read" : "sent",
                    message.getSenderName()
            ));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark messages as read when entering chat
        if (isDirectMessaging && conversationPresenter != null && chatRoomId != null) {
            conversationPresenter.markAsRead(chatRoomId, currentUid);
        } else if (chatPresenter != null) {
            chatPresenter.markAsRead();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatPresenter != null) chatPresenter.detach();
        if (conversationPresenter != null) conversationPresenter.detach();
    }

    /**
     * Navigates to view the chat peer's profile
     */
    public void navigateToUserProfile() {
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "User profile not available", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.Intent intent = new android.content.Intent(this, UserProfileActivity.class);
        intent.putExtra(AppConstants.KEY_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
