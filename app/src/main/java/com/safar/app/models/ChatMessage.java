package com.safar.app.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChatMessage {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_SYSTEM = "system";
    public static final String TYPE_IMAGE = "image";

    private String messageId;
    private String senderId;
    private String senderName;
    private String senderPhotoUrl;
    private String text;
    private long timestamp;
    private String type;
    private boolean read;

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String text, String type) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public ChatMessage(String senderId, String senderName, String senderPhotoUrl, String text, String type) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPhotoUrl = senderPhotoUrl;
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhotoUrl() { return senderPhotoUrl; }
    public void setSenderPhotoUrl(String senderPhotoUrl) { this.senderPhotoUrl = senderPhotoUrl; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Exclude
    public boolean isFromMe(String currentUid) {
        return senderId != null && senderId.equals(currentUid);
    }
}
