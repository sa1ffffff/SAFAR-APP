package com.safar.app.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * SAFAR Conversation Model
 * 
 * Represents a user-to-user conversation
 * Conversations are NOT linked to specific rides - they persist between users.
 */
@IgnoreExtraProperties
public class Conversation {

    private String conversationId;
    private Map<String, Boolean> participants; // uid -> true
    private String lastMessage;
    private long lastMessageTimestamp;
    private String lastMessageSenderId;
    private Map<String, Long> unreadCount; // uid -> count
    private Map<String, UserInfo> participantInfo; // uid -> UserInfo
    private long createdAt;

    // Required empty constructor for Firebase
    public Conversation() {
        participants = new HashMap<>();
        unreadCount = new HashMap<>();
        participantInfo = new HashMap<>();
    }

    public Conversation(String uid1, String uid2) {
        this.conversationId = generateId(uid1, uid2);
        this.participants = new HashMap<>();
        this.participants.put(uid1, true);
        this.participants.put(uid2, true);
        this.unreadCount = new HashMap<>();
        this.unreadCount.put(uid1, 0L);
        this.unreadCount.put(uid2, 0L);
        this.participantInfo = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public static String generateId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Getters and Setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Map<String, Boolean> getParticipants() { return participants; }
    public void setParticipants(Map<String, Boolean> participants) { this.participants = participants; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Map<String, Long> getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Map<String, Long> unreadCount) { this.unreadCount = unreadCount; }

    public Map<String, UserInfo> getParticipantInfo() { return participantInfo; }
    public void setParticipantInfo(Map<String, UserInfo> participantInfo) { this.participantInfo = participantInfo; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Exclude
    public String getOtherParticipantId(String currentUid) {
        for (String uid : participants.keySet()) {
            if (!uid.equals(currentUid)) {
                return uid;
            }
        }
        return null;
    }

    @Exclude
    public long getUnreadCountForUser(String uid) {
        if (unreadCount != null && unreadCount.containsKey(uid)) {
            return unreadCount.get(uid);
        }
        return 0;
    }

    @Exclude
    public UserInfo getOtherParticipantInfo(String currentUid) {
        String otherId = getOtherParticipantId(currentUid);
        if (otherId != null && participantInfo != null) {
            return participantInfo.get(otherId);
        }
        return null;
    }

    /**
     * Embedded user info for quick access without extra DB queries
     */
    @IgnoreExtraProperties
    public static class UserInfo {
        private String name;
        private String photoUrl;
        private String phone;

        public UserInfo() {}

        public UserInfo(String name, String photoUrl) {
            this.name = name;
            this.photoUrl = photoUrl;
        }

        public UserInfo(String name, String photoUrl, String phone) {
            this.name = name;
            this.photoUrl = photoUrl;
            this.phone = phone;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}