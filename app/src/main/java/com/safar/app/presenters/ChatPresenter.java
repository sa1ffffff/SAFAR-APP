package com.safar.app.presenters;

import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.ChatMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatPresenter {

    public interface ChatView {
        void onMessageReceived(ChatMessage message);
        void onMessagesLoaded(List<ChatMessage> messages);
        void onMessageSent();
        void onError(String message);
    }

    private final ChatView view;
    private final FirebaseManager firebaseManager;
    private final String chatRoomId;
    private final String requestId;
    private ChildEventListener chatListener;

    public ChatPresenter(ChatView view, String chatRoomId, String requestId) {
        this.view = view;
        this.chatRoomId = chatRoomId;
        this.requestId = requestId;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    public void startListening() {
        if (chatRoomId == null || chatRoomId.isEmpty()) return;
        
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(snapshot.getKey());
                    view.onMessageReceived(msg);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                    view.onError(error.getMessage());
                }
            }
        };

        firebaseManager.getChatRef(chatRoomId).addChildEventListener(chatListener);
        markAsRead();
    }

    public void sendMessage(ChatMessage message) {
        if (chatRoomId == null || chatRoomId.isEmpty()) return;
        
        String msgId = firebaseManager.getChatRef(chatRoomId).push().getKey();
        if (msgId == null) {
            view.onError("Failed to send message.");
            return;
        }
        message.setMessageId(msgId);
        firebaseManager.getChatRef(chatRoomId).child(msgId).setValue(message)
                .addOnSuccessListener(unused -> {
                    updateInbox(message);
                    view.onMessageSent();
                })
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    private void updateInbox(ChatMessage message) {
        if (requestId == null || requestId.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getText());
        updates.put("lastMessageTimestamp", message.getTimestamp());
        updates.put("lastMessageSenderId", message.getSenderId());
        updates.put("lastMessageRead", false);

        firebaseManager.getRequestsRef().child(requestId).updateChildren(updates);
    }

    public void markAsRead() {
        if (requestId == null || requestId.isEmpty()) return;
        
        // Only mark as read if the last message wasn't sent by current user
        firebaseManager.getRequestsRef().child(requestId).child("lastMessageRead").setValue(true);
    }

    public void detach() {
        if (chatListener != null && chatRoomId != null) {
            firebaseManager.getChatRef(chatRoomId).removeEventListener(chatListener);
        }
    }
}
