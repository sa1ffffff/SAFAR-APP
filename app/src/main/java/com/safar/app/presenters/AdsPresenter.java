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
import java.util.Collections;
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

    public void loadActiveAds() {
        if (view != null) view.onLoading(true);

        if (activeAdsListener != null) {
            firebaseManager.getAdsRef().removeEventListener(activeAdsListener);
            activeAdsListener = null;
        }

        activeAdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RideAd> ads = new ArrayList<>();
                long now = System.currentTimeMillis();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        RideAd ad = ds.getValue(RideAd.class);
                        if (ad != null) {
                            ad.setAdId(ds.getKey());
                            if (ad.isExpired(now)) {
                                ds.getRef().removeValue()
                                        .addOnFailureListener(e ->
                                                Log.w(TAG, "Failed removing expired ad " + ds.getKey() + ": " + e.getMessage()));
                                continue;
                            }
                            if (RideAd.STATUS_OPEN.equals(ad.getStatus()) || ad.getStatus() == null) {
                                ads.add(ad);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing ad: " + e.getMessage());
                    }
                }

                // Sort by earliest posted first (ascending order of createdAt)
                Collections.sort(ads, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));

                if (view != null) {
                    view.onLoading(false);
                    view.onAdsLoaded(ads);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (view != null) {
                    view.onLoading(false);
                    // Ignore permission errors that often happen during logout
                    if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                        view.onError("Database error: " + error.getMessage());
                    }
                }
            }
        };

        firebaseManager.getAdsRef().addValueEventListener(activeAdsListener);
    }

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
                long now = System.currentTimeMillis();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        RideAd ad = ds.getValue(RideAd.class);
                        if (ad != null) {
                            ad.setAdId(ds.getKey());
                            if (ad.isExpired(now)) {
                                ds.getRef().removeValue()
                                        .addOnFailureListener(e ->
                                                Log.w(TAG, "Failed removing expired ad " + ds.getKey() + ": " + e.getMessage()));
                                continue;
                            }
                            if (uid.equals(ad.getCreatorUid())) {
                                ads.add(ad);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing ad: " + e.getMessage());
                    }
                }

                // Sort by earliest posted first (ascending order of createdAt)
                Collections.sort(ads, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));

                if (view != null) {
                    view.onLoading(false);
                    view.onAdsLoaded(ads);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (view != null) {
                    view.onLoading(false);
                    if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                        view.onError("Failed to load your rides: " + error.getMessage());
                    }
                }
            }
        };

        firebaseManager.getAdsRef().addValueEventListener(myAdsListener);
    }

    public void postAd(RideAd ad) {
        if (view != null) view.onLoading(true);

        String adId = firebaseManager.getAdsRef().push().getKey();
        if (adId == null) {
            if (view != null) {
                view.onLoading(false);
                view.onError("Cannot connect to database.");
            }
            return;
        }

        ad.setAdId(adId);
        ad.setStatus(RideAd.STATUS_OPEN);
        ad.setCreatedAt(System.currentTimeMillis());
        ad.setExpiresAt(ad.getCreatedAt() + RideAd.EXPIRY_WINDOW_MS);

        firebaseManager.getAdRef(adId).setValue(ad)
                .addOnSuccessListener(unused -> {
                    if (view != null) {
                        view.onLoading(false);
                        view.onAdPosted(adId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (view != null) {
                        view.onLoading(false);
                        view.onError("Failed to post ride: " + e.getMessage());
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
