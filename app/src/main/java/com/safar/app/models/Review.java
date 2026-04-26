package com.safar.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * SAFAR Review Model
 * 
 * Represents a review given by one user to another after completing a ride.
 * Reviews can only be submitted after both parties confirm ride completion.
 * 
 * Firebase Structure:
 * reviews/
 *   {reviewId}/
 *     - reviewId
 *     - rideAdId
 *     - joinRequestId
 *     - reviewerId (who gave the review)
 *     - revieweeId (who received the review)
 *     - rating (1-5)
 *     - comment
 *     - timestamp
 *     - rideCompletedAt
 */
@IgnoreExtraProperties
public class Review {
    
    private String reviewId;
    private String rideAdId;
    private String joinRequestId;
    private String reviewerId;         // User ID who gave the review
    private String reviewerName;       // Name of reviewer
    private String revieweeId;         // User ID who received the review
    private int rating;                // 1-5 stars
    private String comment;            // Optional review text
    private long timestamp;            // When review was submitted
    private long rideCompletedAt;      // When the ride was completed
    
    // Required empty constructor for Firebase
    public Review() {}
    
    /**
     * Full constructor for creating a new review
     */
    public Review(String rideAdId, String joinRequestId, String reviewerId, String reviewerName,
                  String revieweeId, int rating, String comment, long rideCompletedAt) {
        this.rideAdId = rideAdId;
        this.joinRequestId = joinRequestId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.revieweeId = revieweeId;
        this.rating = Math.max(1, Math.min(5, rating)); // Clamp between 1-5
        this.comment = comment;
        this.timestamp = System.currentTimeMillis();
        this.rideCompletedAt = rideCompletedAt;
    }
    
    // Getters and Setters
    
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    
    public String getRideAdId() { return rideAdId; }
    public void setRideAdId(String rideAdId) { this.rideAdId = rideAdId; }
    
    public String getJoinRequestId() { return joinRequestId; }
    public void setJoinRequestId(String joinRequestId) { this.joinRequestId = joinRequestId; }
    
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    
    public String getRevieweeId() { return revieweeId; }
    public void setRevieweeId(String revieweeId) { this.revieweeId = revieweeId; }
    
    public int getRating() { return rating; }
    public void setRating(int rating) { 
        this.rating = Math.max(1, Math.min(5, rating)); // Ensure 1-5 range
    }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public long getRideCompletedAt() { return rideCompletedAt; }
    public void setRideCompletedAt(long rideCompletedAt) { this.rideCompletedAt = rideCompletedAt; }
    
    /**
     * Validates if the review data is valid
     */
    public boolean isValid() {
        return reviewerId != null && !reviewerId.isEmpty()
                && revieweeId != null && !revieweeId.isEmpty()
                && rideAdId != null && !rideAdId.isEmpty()
                && rating >= 1 && rating <= 5
                && !reviewerId.equals(revieweeId); // Can't review yourself
    }
}
