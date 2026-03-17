package com.safar.app.presenters;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.RideAd;

import java.util.ArrayList;
import java.util.List;

public class AdsPresenter {

    private static final String TAG = "AdsPresenter";

    public interface AdsView {
        void onAdsLoaded(List<RideAd> ads);
        void onAdPosted(String adId);
        void onAdDeleted();
        void onError(String message);
        void onLoading(boolean isLoading);
    }

    private final AdsView view;
    private final FirebaseManager firebaseManager;
    private ValueEventListener activeAdsListener;
    private ValueEventListener myAdsListener;

    public AdsPresenter(AdsView view) {
        this.view = view;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * FIX: Uses addValueEventListener (real-time) NOT singleValueEvent.
     * Also removed the departure-time filter that was hiding newly posted rides
     * whose time might be slightly off. Now shows ALL OPEN rides.
     */
    public void loadActiveAds() {
        if (view != null) view.onLoading(true);

        // Remove old listener first to prevent duplicates
        if (activeAdsListener != null) {
            firebaseManager.getAdsRef().removeEventListener(activeAdsListener);
            activeAdsListener = null;
        }

        activeAdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: total nodes = " + snapshot.getChildrenCount());
                List<RideAd> ads = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        RideAd ad = ds.getValue(RideAd.class);
                        if (ad != null) {
                            // Always set the key from Firebase
                            ad.setAdId(ds.getKey());

                            Log.d(TAG, "Ad found: " + ad.getAdId()
                                    + " status=" + ad.getStatus()
                                    + " origin=" + ad.getOrigin());

                            // Only filter by status — NOT by departure time
                            // (avoids bug where ride not shown due to clock drift)
                            if (RideAd.STATUS_OPEN.equals(ad.getStatus())
                                    || ad.getStatus() == null) {
                                ads.add(ad);
                            }
                        } else {
                            Log.w(TAG, "Null ad at key: " + ds.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing ad at " + ds.getKey() + ": " + e.getMessage());
                    }
                }

                Log.d(TAG, "Filtered open ads count: " + ads.size());

                if (view != null) {
                    view.onLoading(false);
                    view.onAdsLoaded(ads);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase cancelled: " + error.getMessage());
                if (view != null) {
                    view.onLoading(false);
                    view.onError("Database error: " + error.getMessage()
                            + "\nCheck Firebase rules and internet connection.");
                }
            }
        };

        // KEY FIX: addValueEventListener fires on every change (not just once)
        firebaseManager.getAdsRef().addValueEventListener(activeAdsListener);
    }

    /**
     * Load rides posted by a specific user - also real-time.
     */
    public void loadMyAds(String uid) {
        if (view != null) view.onLoading(true);

        if (myAdsListener != null) {
            firebaseManager.getAdsRef().removeEventListener(myAdsListener);
            myAdsListener = null;
        }

        myAdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RideAd> ads = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        RideAd ad = ds.getValue(RideAd.class);
                        if (ad != null && uid.equals(ad.getCreatorUid())) {
                            ad.setAdId(ds.getKey());
                            ads.add(ad);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing ad: " + e.getMessage());
                    }
                }
                if (view != null) {
                    view.onLoading(false);
                    view.onAdsLoaded(ads);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (view != null) {
                    view.onLoading(false);
                    view.onError("Failed to load your rides: " + error.getMessage());
                }
            }
        };

        firebaseManager.getAdsRef().addValueEventListener(myAdsListener);
    }

    /**
     * Post a new ride. Guaranteed to write to Firebase and confirm.
     */
    public void postAd(RideAd ad) {
        if (view != null) view.onLoading(true);

        // Push a new unique key
        String adId = firebaseManager.getAdsRef().push().getKey();
        if (adId == null) {
            Log.e(TAG, "Firebase push() returned null key");
            if (view != null) {
                view.onLoading(false);
                view.onError("Cannot connect to database. Check your internet connection.");
            }
            return;
        }

        ad.setAdId(adId);
        // Ensure status is always set to OPEN
        ad.setStatus(RideAd.STATUS_OPEN);
        ad.setCreatedAt(System.currentTimeMillis());

        Log.d(TAG, "Posting ad with key: " + adId + " origin=" + ad.getOrigin());

        firebaseManager.getAdRef(adId).setValue(ad)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Ad posted successfully: " + adId);
                    if (view != null) {
                        view.onLoading(false);
                        view.onAdPosted(adId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to post ad: " + e.getMessage());
                    if (view != null) {
                        view.onLoading(false);
                        view.onError("Failed to post ride: " + e.getMessage()
                                + "\nCheck Firebase Database rules.");
                    }
                });
    }

    public void updateAdStatus(String adId, String status) {
        firebaseManager.getAdRef(adId).child("status").setValue(status)
                .addOnFailureListener(e -> {
                    if (view != null) view.onError("Update failed: " + e.getMessage());
                });
    }

    public void deleteAd(String adId) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUid == null) {
            if (view != null) view.onError("Not authenticated.");
            return;
        }
        firebaseManager.getAdRef(adId).removeValue()
                .addOnSuccessListener(unused -> {
                    if (view != null) view.onAdDeleted();
                })
                .addOnFailureListener(e -> {
                    if (view != null) view.onError("Delete failed: " + e.getMessage());
                });
    }

    /**
     * MUST call this when Fragment/Activity is destroyed.
     */
    public void detach() {
        if (activeAdsListener != null) {
            try {
                firebaseManager.getAdsRef().removeEventListener(activeAdsListener);
            } catch (Exception ignored) {}
            activeAdsListener = null;
        }
        if (myAdsListener != null) {
            try {
                firebaseManager.getAdsRef().removeEventListener(myAdsListener);
            } catch (Exception ignored) {}
            myAdsListener = null;
        }
    }
}
