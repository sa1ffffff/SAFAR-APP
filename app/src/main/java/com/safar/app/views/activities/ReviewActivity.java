package com.safar.app.views.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.models.Review;
import com.safar.app.presenters.ReviewPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;

import java.util.List;

/**
 * SAFAR Review Activity
 * 
 * Allows users to rate and review each other after completing a ride.
 * Can only be accessed after both parties mark the ride as complete.
 */
public class ReviewActivity extends AppCompatActivity implements ReviewPresenter.ReviewView {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_REVIEWEE_ID = "extra_reviewee_id";
    public static final String EXTRA_REVIEWEE_NAME = "extra_reviewee_name";
    public static final String EXTRA_RIDE_AD_ID = "extra_ride_ad_id";
    public static final String EXTRA_IS_CREATOR = "extra_is_creator";

    private ReviewStateHolder stateHolder;
    private ReviewPresenter presenter;
    private SessionManager session;

    private String requestId;
    private String revieweeId;
    private String revieweeName;
    private String rideAdId;
    private boolean isCreator;
    private String currentUserId;
    private JoinRequest request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        revieweeId = getIntent().getStringExtra(EXTRA_REVIEWEE_ID);
        revieweeName = getIntent().getStringExtra(EXTRA_REVIEWEE_NAME);
        rideAdId = getIntent().getStringExtra(EXTRA_RIDE_AD_ID);
        isCreator = getIntent().getBooleanExtra(EXTRA_IS_CREATOR, false);

        session = new SessionManager(this);
        currentUserId = session.getCurrentUserId();

        stateHolder = new ReviewStateHolder();
        stateHolder.setRevieweeName(revieweeName != null ? revieweeName : "User");

        presenter = new ReviewPresenter(this);

        // Setup Compose UI
        ReviewBridgeKt.setupComposeUI(this, stateHolder);

        // Load request data and check eligibility
        loadRequestData();
    }

    private void loadRequestData() {
        stateHolder.setLoading(true);
        FirebaseManager.getInstance().getRequestsRef().child(requestId).get()
                .addOnSuccessListener(snapshot -> {
                    request = snapshot.getValue(JoinRequest.class);
                    if (request == null) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Request not found");
                        finish();
                        return;
                    }
                    request.setRequestId(requestId);
                    stateHolder.setLoading(false);

                    // Check if can review
                    if (!request.canUserReview(currentUserId, isCreator)) {
                        Toast.makeText(this, "Cannot review at this time", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Failed to load data");
                    finish();
                });
    }

    public void handleSubmitReview() {
        int rating = stateHolder.getRating();
        String comment = stateHolder.getComment();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (request == null) {
            Toast.makeText(this, "Data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review(
                rideAdId,
                requestId,
                currentUserId,
                session.getUserName(),
                revieweeId,
                rating,
                comment,
                request.getCompletedAt()
        );

        presenter.submitReview(review, request, isCreator);
    }

    @Override
    public void onReviewSubmitted() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }

    @Override
    public void onReviewsLoaded(List<Review> reviews) {
        // Not used in this activity
    }

    @Override
    public void onReviewEligibilityChecked(boolean canReview, String reason) {
        // Not used in this activity
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            stateHolder.setLoading(false);
            stateHolder.setError(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onLoading(boolean isLoading) {
        runOnUiThread(() -> stateHolder.setLoading(isLoading));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.detach();
        }
    }
}
