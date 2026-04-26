package com.safar.app.presenters;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.models.Review;
import com.safar.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SAFAR Review Presenter
 * 
 * Handles all review-related operations:
 * - Submitting reviews
 * - Checking review eligibility
 * - Loading user reviews
 * - Updating user ratings
 */
public class ReviewPresenter {

    private static final String TAG = "ReviewPresenter";

    public interface ReviewView {
        void onReviewSubmitted();
        void onReviewsLoaded(List<Review> reviews);
        void onReviewEligibilityChecked(boolean canReview, String reason);
        void onError(String message);
        void onLoading(boolean isLoading);
    }

    private final ReviewView view;
    private final FirebaseManager firebaseManager;

    public ReviewPresenter(ReviewView view) {
        this.view = view;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Submits a review for a user after ride completion
     */
    public void submitReview(Review review, JoinRequest request, boolean isCreatorReviewing) {
        if (view != null) view.onLoading(true);

        if (!review.isValid()) {
            if (view != null) {
                view.onLoading(false);
                view.onError("Invalid review data");
            }
            return;
        }

        // Generate review ID
        String reviewId = firebaseManager.getReviewsRef().push().getKey();
        if (reviewId == null) {
            if (view != null) {
                view.onLoading(false);
                view.onError("Failed to generate review ID");
            }
            return;
        }

        review.setReviewId(reviewId);

        // Prepare batch update
        Map<String, Object> updates = new HashMap<>();
        
        // Save review
        updates.put("/reviews/" + reviewId, review);
        
        // Update join request to mark as reviewed
        String reviewField = isCreatorReviewing 
                ? "/joinRequests/" + request.getRequestId() + "/creatorReviewedRequester"
                : "/joinRequests/" + request.getRequestId() + "/requesterReviewedCreator";
        updates.put(reviewField, true);

        // Perform atomic update
        firebaseManager.getUsersRef().getRoot().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update reviewee's rating
                    updateUserRating(review.getRevieweeId(), review.getRating());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit review", e);
                    if (view != null) {
                        view.onLoading(false);
                        view.onError("Failed to submit review: " + e.getMessage());
                    }
                });
    }

    /**
     * Updates a user's aggregated rating after receiving a new review
     */
    private void updateUserRating(String userId, int newRating) {
        DatabaseReference userRef = firebaseManager.getUserRef(userId);
        
        userRef.get().addOnSuccessListener(snapshot -> {
            User user = snapshot.getValue(User.class);
            if (user == null) {
                if (view != null) {
                    view.onLoading(false);
                    view.onError("User not found");
                }
                return;
            }

            // Calculate new average rating
            int currentCount = user.getRatingCount();
            double currentRating = user.getRating();
            
            int newCount = currentCount + 1;
            double newAverage = ((currentRating * currentCount) + newRating) / newCount;

            // Update user's rating
            Map<String, Object> ratingUpdates = new HashMap<>();
            ratingUpdates.put("rating", newAverage);
            ratingUpdates.put("ratingCount", newCount);

            userRef.updateChildren(ratingUpdates)
                    .addOnSuccessListener(aVoid2 -> {
                        Log.d(TAG, "User rating updated successfully");
                        if (view != null) {
                            view.onLoading(false);
                            view.onReviewSubmitted();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update user rating", e);
                        // Review is saved but rating update failed
                        if (view != null) {
                            view.onLoading(false);
                            view.onReviewSubmitted(); // Still consider it success
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load user for rating update", e);
            if (view != null) {
                view.onLoading(false);
                view.onError("Failed to update rating");
            }
        });
    }

    /**
     * Checks if a user can review another user for a specific ride
     */
    public void checkReviewEligibility(String requestId, String currentUserId, boolean isCreator) {
        firebaseManager.getRequestsRef().child(requestId).get()
                .addOnSuccessListener(snapshot -> {
                    JoinRequest request = snapshot.getValue(JoinRequest.class);
                    if (request == null) {
                        if (view != null) {
                            view.onReviewEligibilityChecked(false, "Request not found");
                        }
                        return;
                    }

                    // Check if ride is completed
                    if (!request.isRideCompleted() || !request.isBothPartiesMarkedComplete()) {
                        if (view != null) {
                            view.onReviewEligibilityChecked(false, "Ride not completed by both parties");
                        }
                        return;
                    }

                    // Check if already reviewed
                    boolean alreadyReviewed = isCreator 
                            ? request.isCreatorReviewedRequester()
                            : request.isRequesterReviewedCreator();

                    if (alreadyReviewed) {
                        if (view != null) {
                            view.onReviewEligibilityChecked(false, "Already reviewed");
                        }
                        return;
                    }

                    // Can review
                    if (view != null) {
                        view.onReviewEligibilityChecked(true, "Eligible to review");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check review eligibility", e);
                    if (view != null) {
                        view.onReviewEligibilityChecked(false, "Error checking eligibility");
                    }
                });
    }

    /**
     * Loads all reviews for a specific user
     */
    public void loadUserReviews(String userId) {
        if (view != null) view.onLoading(true);

        firebaseManager.getReviewsRef()
                .orderByChild("revieweeId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Review> reviews = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review review = child.getValue(Review.class);
                            if (review != null) {
                                review.setReviewId(child.getKey());
                                reviews.add(review);
                            }
                        }
                        
                        if (view != null) {
                            view.onLoading(false);
                            view.onReviewsLoaded(reviews);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load reviews", error.toException());
                        if (view != null) {
                            view.onLoading(false);
                            view.onError("Failed to load reviews");
                        }
                    }
                });
    }

    /**
     * Marks a ride as completed from one party's side
     */
    public void markRideComplete(String requestId, String currentUserId, boolean isCreator) {
        DatabaseReference requestRef = firebaseManager.getRequestsRef().child(requestId);
        
        requestRef.get().addOnSuccessListener(snapshot -> {
            JoinRequest request = snapshot.getValue(JoinRequest.class);
            if (request == null) {
                if (view != null) {
                    view.onError("Request not found");
                }
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            long now = System.currentTimeMillis();

            if (isCreator) {
                updates.put("creatorMarkedComplete", true);
                // Check if requester also marked complete
                if (request.isRequesterMarkedComplete()) {
                    updates.put("rideCompleted", true);
                    updates.put("completedAt", now);
                    updates.put("status", JoinRequest.STATUS_COMPLETED);
                }
            } else {
                updates.put("requesterMarkedComplete", true);
                // Check if creator also marked complete
                if (request.isCreatorMarkedComplete()) {
                    updates.put("rideCompleted", true);
                    updates.put("completedAt", now);
                    updates.put("status", JoinRequest.STATUS_COMPLETED);
                }
            }

            requestRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Ride marked as complete");
                        if (view != null) {
                            view.onReviewSubmitted(); // Reuse callback for completion
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to mark ride complete", e);
                        if (view != null) {
                            view.onError("Failed to mark ride complete");
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load request", e);
            if (view != null) {
                view.onError("Failed to load request");
            }
        });
    }

    public void detach() {
        // Clean up if needed
    }
}
