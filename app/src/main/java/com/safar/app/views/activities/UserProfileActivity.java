package com.safar.app.views.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.Review;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * SAFAR User Profile Activity
 * 
 * Displays another user's public profile including:
 * - Name and profile picture
 * - Bio
 * - Rating and total rides
 * - Reviews from other users
 * 
 * This is a read-only view of another user's profile.
 */
public class UserProfileActivity extends AppCompatActivity {

    private UserProfileStateHolder stateHolder;
    private String userId;
    private ValueEventListener reviewsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get user ID from intent
        userId = getIntent().getStringExtra(AppConstants.KEY_OTHER_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        stateHolder = new UserProfileStateHolder();

        // Setup Compose UI
        UserProfileBridgeKt.setupComposeUI(this, stateHolder);

        // Load user data and reviews
        loadUserProfile();
        loadUserReviews();
    }

    private void loadUserProfile() {
        stateHolder.setLoading(true);

        FirebaseManager.getInstance().getUserRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        stateHolder.setUserName(user.getName() != null ? user.getName() : "User");
                        stateHolder.setBio(user.getBio() != null ? user.getBio() : "");
                        stateHolder.setPhotoUrl(user.getPhotoUrl());
                        stateHolder.setRating((float) user.getRating());
                        stateHolder.setRatingCount(user.getRatingCount());
                        stateHolder.setTotalRides(user.getTotalRides());
                        stateHolder.setMemberSince(user.getJoinedAt());
                        stateHolder.setLoading(false);
                    } else {
                        stateHolder.setLoading(false);
                        stateHolder.setError("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Failed to load profile");
                });
    }

    private void loadUserReviews() {
        FirebaseManager.getInstance().getReviewsRef()
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
                        stateHolder.setReviews(reviews);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Silently fail for reviews - profile still shows
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsListener != null) {
            FirebaseManager.getInstance().getReviewsRef().removeEventListener(reviewsListener);
        }
    }
}
