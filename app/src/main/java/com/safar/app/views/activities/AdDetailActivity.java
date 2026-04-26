package com.safar.app.views.activities;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.presenters.RequestPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.DateUtils;
import com.safar.app.utils.PhoneFormatUtils;
import com.safar.app.utils.SessionManager;
import com.safar.app.views.ui.screens.JoinRequestUiModel;

import java.util.ArrayList;
import java.util.List;

public class AdDetailActivity extends AppCompatActivity implements
        AdsPresenter.AdsView, RequestPresenter.RequestView {

    private AdsPresenter adsPresenter;
    private RequestPresenter requestPresenter;
    private RideAd currentAd;
    private String adId;
    private String currentUid;
    private SessionManager session;
    private AdDetailStateHolder stateHolder;
    private List<JoinRequest> rawRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        stateHolder = new AdDetailStateHolder();
        AdDetailBridgeKt.setupComposeUI(this, stateHolder);

        adId = getIntent().getStringExtra(AppConstants.KEY_AD_ID);
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        session = new SessionManager(this);

        adsPresenter = new AdsPresenter(this);
        requestPresenter = new RequestPresenter(this);

        loadAdDetail();
    }

    private void loadAdDetail() {
        if (adId == null) return;
        stateHolder.setLoading(true);
        FirebaseManager.getInstance().getAdRef(adId).get()
                .addOnSuccessListener(snapshot -> {
                    stateHolder.setLoading(false);
                    if (!snapshot.exists()) {
                        finish();
                        return;
                    }
                    currentAd = snapshot.getValue(RideAd.class);
                    if (currentAd == null) { finish(); return; }
                    currentAd.setAdId(adId);
                    if (currentAd.isExpired(System.currentTimeMillis())) {
                        FirebaseManager.getInstance().getAdRef(adId).removeValue();
                        Toast.makeText(this, "This ride has expired.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    boolean isOwner = currentAd.getCreatorUid().equals(currentUid);
                    stateHolder.setIsOwner(isOwner);
                    
                    populateUi();
                    if (isOwner) loadRequests();
                    else checkMyRequest();
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                });
    }

    private void populateUi() {
        stateHolder.setOrigin(currentAd.getOrigin());
        stateHolder.setDestination(currentAd.getDestination());
        stateHolder.setDepartureTime(DateUtils.formatDeparture(currentAd.getDepartureTime()));
        stateHolder.setSeats(currentAd.getAvailableSeats() + " seat(s) available");
        stateHolder.setPostedBy(currentAd.getCreatorName());
        stateHolder.setPosterPhone(currentAd.getCreatorPhone() != null ? currentAd.getCreatorPhone() : "");
        stateHolder.setStatus(currentAd.getStatus());
        stateHolder.setVehicle(currentAd.getVehicleDetails() != null ? currentAd.getVehicleDetails() : "");
        stateHolder.setNotes(currentAd.getNotes() != null ? currentAd.getNotes() : "");
    }

    private void loadRequests() {
        requestPresenter.loadRequestsForAd(adId);
    }

    private void checkMyRequest() {
        requestPresenter.checkExistingRequest(adId, currentUid, req -> {
            if (req == null) return;
            runOnUiThread(() -> {
                if (JoinRequest.STATUS_PENDING.equals(req.getStatus())) {
                    stateHolder.setUserRequestStatus("Pending");
                    stateHolder.setHasRequested(true);
                } else if (JoinRequest.STATUS_ACCEPTED.equals(req.getStatus())) {
                    stateHolder.setUserRequestStatus("Accepted");
                    stateHolder.setHasRequested(true);
                }
            });
        });
    }

    public void handleMarkFilled() {
        adsPresenter.updateAdStatus(adId, RideAd.STATUS_FILLED);
    }

    public void handleDeleteAd() {
        adsPresenter.deleteAd(adId);
    }
    
    public void handleEditAd() {
        // Implementation for edit ad
    }
    
    public void handleShareAd() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this ride from " + currentAd.getOrigin() + " to " + currentAd.getDestination());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Ride"));
    }

    public void handleSendRequest() {
        // If already accepted, open chat
        requestPresenter.checkExistingRequest(adId, currentUid, req -> {
            if (req != null && JoinRequest.STATUS_ACCEPTED.equals(req.getStatus())) {
                Intent i = new Intent(this, ChatActivity.class);
                i.putExtra(AppConstants.KEY_CHAT_ROOM_ID, req.getChatRoomId());
                i.putExtra(AppConstants.KEY_OTHER_USER_NAME, currentAd.getCreatorName());
                startActivity(i);
            } else {
                JoinRequest newReq = new JoinRequest(adId, currentUid, session.getUserName(),
                        session.getUserPhone(), currentAd.getCreatorUid());
                requestPresenter.sendJoinRequest(newReq);
            }
        });
    }
    
    public void handleCancelRequest() {
        // Implementation for cancel request
    }

    public void handleAccept(String requestId) {
        JoinRequest request = findRequestById(requestId);
        if (request != null) {
            String chatRoomId = FirebaseManager.getInstance()
                    .generateChatRoomId(adId, currentUid, request.getRequestorUid());
            String acceptorName = session.getUserName() != null ? session.getUserName() : "Driver";
            requestPresenter.acceptRequest(request, chatRoomId, acceptorName);
        }
    }

    public void handleReject(String requestId) {
        JoinRequest request = findRequestById(requestId);
        if (request != null) {
            String rejectorName = session.getUserName() != null ? session.getUserName() : "Driver";
            requestPresenter.rejectRequest(request, rejectorName);
        }
    }
    
    public void handleContactPoster() {
        if (currentAd == null) {
            Toast.makeText(this, "Ad data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String phone = currentAd.getCreatorPhone();
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone.trim()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    public void handleCallPoster() {
        handleContactPoster();
    }

    public void handleWhatsAppPoster() {
        if (currentAd == null) {
            Toast.makeText(this, "Ad data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String rawPhone = currentAd.getCreatorPhone();
        if (!PhoneFormatUtils.isValidWhatsappLocalNumber(rawPhone)) {
            Toast.makeText(this, "Ask the user to share phone in 03xx-xxxxxxx format", Toast.LENGTH_LONG).show();
            return;
        }

        String whatsappNumber = PhoneFormatUtils.toWhatsappInternationalNumber(rawPhone);
        if (whatsappNumber == null) {
            Toast.makeText(this, "Ask the user to share phone in 03xx-xxxxxxx format", Toast.LENGTH_LONG).show();
            return;
        }

        String message = "Hi " + currentAd.getCreatorName() + ", I saw your ride from "
                + currentAd.getOrigin() + " to " + currentAd.getDestination() + " on SAFAR.";
        Uri whatsappUri = Uri.parse("https://wa.me/" + whatsappNumber + "?text=" + Uri.encode(message));
        Intent intent = new Intent(Intent.ACTION_VIEW, whatsappUri);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Unable to open WhatsApp link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens in-app direct messaging with the ride poster.
     * Creates or opens an existing conversation between current user and poster.
     */
    public void handleChatWithPoster() {
        if (currentAd == null || currentUid == null) return;
        
        String posterUid = currentAd.getCreatorUid();
        if (posterUid == null || posterUid.equals(currentUid)) return;
        
        // Generate conversation ID for direct user-to-user messaging
        String conversationId = FirebaseManager.getInstance().generateConversationId(currentUid, posterUid);
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(AppConstants.KEY_CHAT_ROOM_ID, conversationId);
        intent.putExtra(AppConstants.KEY_OTHER_USER_NAME, currentAd.getCreatorName());
        intent.putExtra(AppConstants.KEY_OTHER_USER_ID, posterUid);
        // Pass context about where this chat was initiated from
        intent.putExtra(AppConstants.KEY_AD_ID, adId);
        startActivity(intent);
    }
    
    public void handleReportAd() {
        // Implementation for report ad
    }

    private JoinRequest findRequestById(String id) {
        for (JoinRequest r : rawRequests) {
            if (r.getRequestId().equals(id)) return r;
        }
        return null;
    }

    @Override public void onAdsLoaded(List<RideAd> ads) {}
    @Override public void onAdPosted(String adId) {}
    @Override public void onAdDeleted() { finish(); }
    @Override public void onError(String message) {
        stateHolder.setLoading(false);
        stateHolder.setError(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    @Override public void onLoading(boolean isLoading) {
        stateHolder.setLoading(isLoading);
    }

    @Override public void onRequestSent() {
        stateHolder.setLoading(false);
        Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
        loadAdDetail();
    }

    @Override
    public void onRequestsLoaded(List<JoinRequest> requests) {
        stateHolder.setLoading(false);
        this.rawRequests = requests;
        List<JoinRequestUiModel> uiModels = new ArrayList<>();
        for (JoinRequest r : requests) {
            uiModels.add(new JoinRequestUiModel(
                    r.getRequestId(),
                    r.getRequestorUid(),  // userId for profile navigation
                    r.getRequestorName(),
                    r.getRequestorPhone(),
                    null, // userAvatar
                    0f,   // userRating
                    1,    // seatsRequested
                    null, // message
                    r.getStatus(),
                    0L    // timestamp
            ));
        }
        stateHolder.setRequests(uiModels);
    }

    /**
     * Navigates to view another user's profile
     */
    public void navigateToUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(AppConstants.KEY_OTHER_USER_ID, userId);
        startActivity(intent);
    }

    @Override
    public void onRequestUpdated(JoinRequest request) {
        stateHolder.setLoading(false);
        stateHolder.setProcessingRequest(null);
        loadRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adsPresenter != null) adsPresenter.detach();
        if (requestPresenter != null) requestPresenter.detach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
