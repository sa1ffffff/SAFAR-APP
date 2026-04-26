package com.safar.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * SAFAR Notification Model
 * 
 * Represents an in-app notification for ride request updates.
 * Notifications are stored in Firebase and displayed in the notifications screen.
 * 
 * Firebase Structure:
 * notifications/
 *   {userId}/
 *     {notificationId}/
 *       - notificationId
 *       - type (REQUEST_ACCEPTED, REQUEST_DECLINED, RIDE_COMPLETED, etc.)
 *       - title
 *       - message
 *       - relatedAdId
 *       - relatedRequestId
 *       - fromUserId
 *       - fromUserName
 *       - fromUserPhoto
 *       - timestamp
 *       - read
 */
@IgnoreExtraProperties
public class Notification {
    
    // Notification types
    public static final String TYPE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    public static final String TYPE_REQUEST_DECLINED = "REQUEST_DECLINED";
    public static final String TYPE_NEW_REQUEST = "NEW_REQUEST";
    public static final String TYPE_RIDE_COMPLETED = "RIDE_COMPLETED";
    public static final String TYPE_NEW_REVIEW = "NEW_REVIEW";
    public static final String TYPE_NEW_MESSAGE = "NEW_MESSAGE";
    
    private String notificationId;
    private String type;
    private String title;
    private String message;
    private String relatedAdId;
    private String relatedRequestId;
    private String fromUserId;
    private String fromUserName;
    private String fromUserPhoto;
    private long timestamp;
    private boolean read;
    
    // Required empty constructor for Firebase
    public Notification() {}
    
    /**
     * Constructor for creating a new notification
     */
    public Notification(String type, String title, String message, 
                        String fromUserId, String fromUserName, String fromUserPhoto) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserPhoto = fromUserPhoto;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }
    
    // Static factory methods for common notification types
    
    public static Notification requestAccepted(String adOrigin, String adDestination,
                                               String fromUserId, String fromUserName, String fromUserPhoto,
                                               String adId, String requestId) {
        Notification n = new Notification(
                TYPE_REQUEST_ACCEPTED,
                "Request Accepted! 🎉",
                fromUserName + " accepted your request to join the ride from " + adOrigin + " to " + adDestination,
                fromUserId, fromUserName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    /**
     * Simplified version without origin/destination
     */
    public static Notification requestAccepted(String fromUserId, String fromUserName, String fromUserPhoto,
                                               String adId, String requestId) {
        Notification n = new Notification(
                TYPE_REQUEST_ACCEPTED,
                "Request Accepted! 🎉",
                fromUserName + " accepted your ride request. You can now chat and coordinate!",
                fromUserId, fromUserName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    public static Notification requestDeclined(String adOrigin, String adDestination,
                                               String fromUserId, String fromUserName, String fromUserPhoto,
                                               String adId, String requestId) {
        Notification n = new Notification(
                TYPE_REQUEST_DECLINED,
                "Request Declined",
                fromUserName + " declined your request for the ride from " + adOrigin + " to " + adDestination,
                fromUserId, fromUserName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    /**
     * Simplified version without origin/destination
     */
    public static Notification requestDeclined(String fromUserId, String fromUserName, String fromUserPhoto,
                                               String adId, String requestId) {
        Notification n = new Notification(
                TYPE_REQUEST_DECLINED,
                "Request Declined",
                fromUserName + " declined your ride request.",
                fromUserId, fromUserName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    public static Notification newRequest(String requesterName, String adOrigin, String adDestination,
                                          String fromUserId, String fromUserPhoto,
                                          String adId, String requestId) {
        Notification n = new Notification(
                TYPE_NEW_REQUEST,
                "New Ride Request",
                requesterName + " wants to join your ride from " + adOrigin + " to " + adDestination,
                fromUserId, requesterName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    /**
     * Simplified version without origin/destination
     */
    public static Notification newRequest(String fromUserId, String fromUserName, String fromUserPhoto,
                                          String adId, String requestId) {
        Notification n = new Notification(
                TYPE_NEW_REQUEST,
                "New Ride Request",
                fromUserName + " wants to join your ride.",
                fromUserId, fromUserName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    public static Notification rideCompleted(String partnerName, String adOrigin, String adDestination,
                                             String fromUserId, String fromUserPhoto,
                                             String adId, String requestId) {
        Notification n = new Notification(
                TYPE_RIDE_COMPLETED,
                "Ride Completed ✓",
                "Your ride with " + partnerName + " from " + adOrigin + " to " + adDestination + " is complete. Leave a review!",
                fromUserId, partnerName, fromUserPhoto
        );
        n.setRelatedAdId(adId);
        n.setRelatedRequestId(requestId);
        return n;
    }
    
    public static Notification newReview(String reviewerName, int rating, String fromUserId, String fromUserPhoto) {
        Notification n = new Notification(
                TYPE_NEW_REVIEW,
                "New Review ⭐",
                reviewerName + " gave you " + rating + " stars!",
                fromUserId, reviewerName, fromUserPhoto
        );
        return n;
    }
    
    // Getters and Setters
    
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getRelatedAdId() { return relatedAdId; }
    public void setRelatedAdId(String relatedAdId) { this.relatedAdId = relatedAdId; }
    
    public String getRelatedRequestId() { return relatedRequestId; }
    public void setRelatedRequestId(String relatedRequestId) { this.relatedRequestId = relatedRequestId; }
    
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    
    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }
    
    public String getFromUserPhoto() { return fromUserPhoto; }
    public void setFromUserPhoto(String fromUserPhoto) { this.fromUserPhoto = fromUserPhoto; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
