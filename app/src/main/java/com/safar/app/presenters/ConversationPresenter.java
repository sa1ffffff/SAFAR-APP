package com.safar.app.presenters;

import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.ChatMessage;
import com.safar.app.models.Conversation;
import com.safar.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CONVERSATION PRESENTER
 * 
 * Handles user-to-user direct messaging operations.
 * This is for WhatsApp-style persistent chats between users,
 * not tied to specific rides.
 */
public class ConversationPresenter {

    public interface ConversationView {
        void onConversationsLoaded(List<Conversation> conversations);
        void onConversationCreated(String conversationId);
        void onMessageReceived(ChatMessage message);
        void onMessageSent();
        void onUnreadCountUpdated(int totalUnread);
        void onError(String message);
    }

    private final ConversationView view;
    private final FirebaseManager firebaseManager;
    private ValueEventListener conversationsListener;
    private ChildEventListener messagesListener;
    private String currentConversationId;

    public ConversationPresenter(ConversationView view) {
        this.view = view;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Load all conversations for the current user.
     * Conversations are loaded where user is a participant.
     */
    public void loadConversations(String currentUserId) {
        if (currentUserId == null) return;

        conversationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Conversation> conversations = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Conversation conv = child.getValue(Conversation.class);
                    if (conv != null) {
                        conv.setConversationId(child.getKey());
                        // Only include conversations where current user is a participant
                        if (conv.getParticipants() != null && 
                            conv.getParticipants().containsKey(currentUserId)) {
                            conversations.add(conv);
                        }
                    }
                }
                // Sort by last message timestamp (most recent first)
                conversations.sort((a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                view.onConversationsLoaded(conversations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                view.onError(error.getMessage());
            }
        };

        // Query conversations where this user is a participant
        firebaseManager.getConversationsRef()
                .orderByChild("participants/" + currentUserId)
                .equalTo(true)
                .addValueEventListener(conversationsListener);
    }

    /**
     * Get or create a conversation between two users.
     */
    public void getOrCreateConversation(String currentUserId, String otherUserId, 
                                         String currentUserName, String otherUserName,
                                         String currentUserPhoto, String otherUserPhoto) {
        String conversationId = firebaseManager.generateConversationId(currentUserId, otherUserId);
        
        firebaseManager.getConversationsRef().child(conversationId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Conversation already exists
                        view.onConversationCreated(conversationId);
                    } else {
                        // Create new conversation
                        createConversation(conversationId, currentUserId, otherUserId,
                                currentUserName, otherUserName, currentUserPhoto, otherUserPhoto);
                    }
                })
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    private void createConversation(String conversationId, String uid1, String uid2,
                                     String name1, String name2, String photo1, String photo2) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setCreatedAt(System.currentTimeMillis());
        conversation.setLastMessageTimestamp(System.currentTimeMillis());
        
        // Set participants
        Map<String, Boolean> participants = new HashMap<>();
        participants.put(uid1, true);
        participants.put(uid2, true);
        conversation.setParticipants(participants);
        
        // Set participant info for quick display
        Map<String, Conversation.UserInfo> participantInfo = new HashMap<>();
        participantInfo.put(uid1, new Conversation.UserInfo(name1, photo1));
        participantInfo.put(uid2, new Conversation.UserInfo(name2, photo2));
        conversation.setParticipantInfo(participantInfo);
        
        // Initialize unread counts
        Map<String, Long> unreadCount = new HashMap<>();
        unreadCount.put(uid1, 0L);
        unreadCount.put(uid2, 0L);
        conversation.setUnreadCount(unreadCount);
        
        firebaseManager.getConversationsRef().child(conversationId).setValue(conversation)
                .addOnSuccessListener(unused -> view.onConversationCreated(conversationId))
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    /**
     * Start listening for messages in a conversation.
     */
    public void startListeningMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) return;
        currentConversationId = conversationId;
        
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(snapshot.getKey());
                    view.onMessageReceived(msg);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Handle message updates (e.g., read receipts)
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle message deletion
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                    view.onError(error.getMessage());
                }
            }
        };

        firebaseManager.getMessagesRef().child(conversationId)
                .orderByChild("timestamp")
                .addChildEventListener(messagesListener);
    }

    /**
     * Send a message in the current conversation.
     */
    public void sendMessage(String conversationId, String currentUserId, 
                            String currentUserName, String currentUserPhoto,
                            String otherUserId, String text) {
        if (conversationId == null || conversationId.isEmpty()) return;
        if (text == null || text.trim().isEmpty()) return;
        
        String msgId = firebaseManager.getMessagesRef().child(conversationId).push().getKey();
        if (msgId == null) {
            view.onError("Failed to send message.");
            return;
        }
        
        ChatMessage message = new ChatMessage();
        message.setMessageId(msgId);
        message.setSenderId(currentUserId);
        message.setSenderName(currentUserName);
        message.setSenderPhotoUrl(currentUserPhoto);
        message.setText(text.trim());
        message.setTimestamp(System.currentTimeMillis());
        message.setType(ChatMessage.TYPE_TEXT);
        
        // Save message
        firebaseManager.getMessagesRef().child(conversationId).child(msgId).setValue(message)
                .addOnSuccessListener(unused -> {
                    // Update conversation metadata
                    updateConversationLastMessage(conversationId, message, otherUserId);
                    view.onMessageSent();
                })
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    private void updateConversationLastMessage(String conversationId, ChatMessage message, String otherUserId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getText());
        updates.put("lastMessageTimestamp", message.getTimestamp());
        updates.put("lastMessageSenderId", message.getSenderId());
        
        // Increment unread count for the other user
        firebaseManager.getConversationsRef().child(conversationId)
                .child("unreadCount").child(otherUserId)
                .get().addOnSuccessListener(snapshot -> {
                    long currentCount = 0;
                    if (snapshot.exists()) {
                        Long val = snapshot.getValue(Long.class);
                        if (val != null) currentCount = val;
                    }
                    updates.put("unreadCount/" + otherUserId, currentCount + 1);
                    
                    firebaseManager.getConversationsRef().child(conversationId)
                            .updateChildren(updates);
                });
    }

    /**
     * Mark messages as read for current user.
     */
    public void markAsRead(String conversationId, String currentUserId) {
        if (conversationId == null || currentUserId == null) return;
        
        firebaseManager.getConversationsRef().child(conversationId)
                .child("unreadCount").child(currentUserId)
                .setValue(0L);
    }

    /**
     * Listen for total unread count across all conversations.
     */
    public void listenForUnreadCount(String currentUserId) {
        if (currentUserId == null) return;
        
        firebaseManager.getConversationsRef()
                .orderByChild("participants/" + currentUserId)
                .equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int totalUnread = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Conversation conv = child.getValue(Conversation.class);
                            if (conv != null && conv.getUnreadCount() != null) {
                                Long count = conv.getUnreadCount().get(currentUserId);
                                if (count != null) totalUnread += count.intValue();
                            }
                        }
                        view.onUnreadCountUpdated(totalUnread);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * Clean up listeners.
     */
    public void detach() {
        if (conversationsListener != null) {
            firebaseManager.getConversationsRef().removeEventListener(conversationsListener);
        }
        if (messagesListener != null && currentConversationId != null) {
            firebaseManager.getMessagesRef().child(currentConversationId).removeEventListener(messagesListener);
        }
    }
}
