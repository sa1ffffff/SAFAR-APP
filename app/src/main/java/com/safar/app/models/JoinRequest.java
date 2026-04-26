package com.safar.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class JoinRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String requestId;
    private String adId;
    private String requestorUid;
    private String requestorName;
    private String requestorPhone;
    private String adCreatorUid;
    private String status;
    private long timestamp;
    private String chatRoomId;
    
    // Ride completion fields
    private boolean rideCompleted;
    private long completedAt;
    private boolean requesterMarkedComplete;
    private boolean creatorMarkedComplete;
    
    // Review tracking
    private boolean requesterReviewedCreator;
    private boolean creatorReviewedRequester;
    
    // Last message info for Inbox
    private String lastMessage;
    private long lastMessageTimestamp;
    private String lastMessageSenderId;
    private boolean lastMessageRead;

    public JoinRequest() {}

    public JoinRequest(String adId, String requestorUid, String requestorName,
                       String requestorPhone, String adCreatorUid) {
        this.adId = adId;
        this.requestorUid = requestorUid;
        this.requestorName = requestorName;
        this.requestorPhone = requestorPhone;
        this.adCreatorUid = adCreatorUid;
        this.status = STATUS_PENDING;
        this.timestamp = System.currentTimeMillis();
        this.lastMessageRead = true; // Initial state
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAdId() { return adId; }
    public void setAdId(String adId) { this.adId = adId; }

    public String getRequestorUid() { return requestorUid; }
    public void setRequestorUid(String requestorUid) { this.requestorUid = requestorUid; }

    public String getRequestorName() { return requestorName; }
    public void setRequestorName(String requestorName) { this.requestorName = requestorName; }

    public String getRequestorPhone() { return requestorPhone; }
    public void setRequestorPhone(String requestorPhone) { this.requestorPhone = requestorPhone; }

    public String getAdCreatorUid() { return adCreatorUid; }
    public void setAdCreatorUid(String adCreatorUid) { this.adCreatorUid = adCreatorUid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public boolean isLastMessageRead() { return lastMessageRead; }
    public void setLastMessageRead(boolean lastMessageRead) { this.lastMessageRead = lastMessageRead; }
    
    // Ride completion getters and setters
    public boolean isRideCompleted() { return rideCompleted; }
    public void setRideCompleted(boolean rideCompleted) { this.rideCompleted = rideCompleted; }
    
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    
    public boolean isRequesterMarkedComplete() { return requesterMarkedComplete; }
    public void setRequesterMarkedComplete(boolean requesterMarkedComplete) { this.requesterMarkedComplete = requesterMarkedComplete; }
    
    public boolean isCreatorMarkedComplete() { return creatorMarkedComplete; }
    public void setCreatorMarkedComplete(boolean creatorMarkedComplete) { this.creatorMarkedComplete = creatorMarkedComplete; }
    
    public boolean isRequesterReviewedCreator() { return requesterReviewedCreator; }
    public void setRequesterReviewedCreator(boolean requesterReviewedCreator) { this.requesterReviewedCreator = requesterReviewedCreator; }
    
    public boolean isCreatorReviewedRequester() { return creatorReviewedRequester; }
    public void setCreatorReviewedRequester(boolean creatorReviewedRequester) { this.creatorReviewedRequester = creatorReviewedRequester; }
    
    /**
     * Checks if both parties have marked the ride as complete
     */
    public boolean isBothPartiesMarkedComplete() {
        return requesterMarkedComplete && creatorMarkedComplete;
    }
    
    /**
     * Checks if current user can review the other party
     */
    public boolean canUserReview(String currentUserId, boolean isCreator) {
        // Must be completed by both parties
        if (!rideCompleted || !isBothPartiesMarkedComplete()) {
            return false;
        }
        
        // Check if already reviewed
        if (isCreator) {
            return !creatorReviewedRequester;
        } else {
            return !requesterReviewedCreator;
        }
    }
}
