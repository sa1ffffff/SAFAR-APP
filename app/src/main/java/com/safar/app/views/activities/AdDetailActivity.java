package com.safar.app.views.activities;

import android.content.Intent;
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
            requestPresenter.acceptRequest(request, chatRoomId);
        }
    }

    public void handleReject(String requestId) {
        JoinRequest request = findRequestById(requestId);
        if (request != null) {
            requestPresenter.rejectRequest(request);
        }
    }
    
    public void handleContactPoster() {
        if (currentAd != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + currentAd.getCreatorPhone()));
            startActivity(intent);
        }
    }
    
    public void handleCallPoster() {
        handleContactPoster();
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
