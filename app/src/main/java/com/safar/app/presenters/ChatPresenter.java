package com.safar.app.presenters;

import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.ChatMessage;

import java.util.List;
import java.util.ArrayList;

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
    private ChildEventListener chatListener;

    public ChatPresenter(ChatView view, String chatRoomId) {
        this.view = view;
        this.chatRoomId = chatRoomId;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    public void startListening() {
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
                view.onError(error.getMessage());
            }
        };

        firebaseManager.getChatRef(chatRoomId).addChildEventListener(chatListener);
    }

    public void sendMessage(ChatMessage message) {
        String msgId = firebaseManager.getChatRef(chatRoomId).push().getKey();
        if (msgId == null) {
            view.onError("Failed to send message.");
            return;
        }
        message.setMessageId(msgId);
        firebaseManager.getChatRef(chatRoomId).child(msgId).setValue(message)
                .addOnSuccessListener(unused -> view.onMessageSent())
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    public void detach() {
        if (chatListener != null) {
            firebaseManager.getChatRef(chatRoomId).removeEventListener(chatListener);
        }
    }
}
