package com.safar.app.views.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.databinding.ActivityAdDetailBinding;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.presenters.RequestPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.DateUtils;
import com.safar.app.utils.SessionManager;
import com.safar.app.views.adapters.RequestsAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdDetailActivity extends AppCompatActivity implements
        AdsPresenter.AdsView, RequestPresenter.RequestView,
        RequestsAdapter.OnRequestActionListener {

    private ActivityAdDetailBinding binding;
    private AdsPresenter adsPresenter;
    private RequestPresenter requestPresenter;
    private RideAd currentAd;
    private String adId;
    private String currentUid;
    private boolean isOwner = false;
    private SessionManager session;
    private RequestsAdapter requestsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // CRITICAL FIX: Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adId = getIntent().getStringExtra(AppConstants.KEY_AD_ID);
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        session = new SessionManager(this);

        adsPresenter = new AdsPresenter(this);
        requestPresenter = new RequestPresenter(this);

        requestsAdapter = new RequestsAdapter(new ArrayList<>(), this);
        binding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRequests.setAdapter(requestsAdapter);

        loadAdDetail();
    }

    private void loadAdDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseManager.getInstance().getAdRef(adId).get()
                .addOnSuccessListener(snapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    currentAd = snapshot.getValue(RideAd.class);
                    if (currentAd == null) { finish(); return; }
                    currentAd.setAdId(adId);
                    isOwner = currentAd.getCreatorUid().equals(currentUid);
                    populateUi();
                    if (isOwner) loadRequests();
                    else checkMyRequest();
                });
    }

    private void populateUi() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(currentAd.getOrigin() + " → " + currentAd.getDestination());

        binding.tvOrigin.setText(currentAd.getOrigin());
        binding.tvDestination.setText(currentAd.getDestination());
        binding.tvDepartureTime.setText(DateUtils.formatDeparture(currentAd.getDepartureTime()));
        binding.tvSeats.setText(currentAd.getAvailableSeats() + " seat(s) available");
        binding.tvPostedBy.setText(currentAd.getCreatorName());
        binding.tvStatus.setText(currentAd.getStatus());

        if (currentAd.getVehicleDetails() != null && !currentAd.getVehicleDetails().isEmpty()) {
            binding.tvVehicle.setText(currentAd.getVehicleDetails());
        }
        if (currentAd.getNotes() != null && !currentAd.getNotes().isEmpty()) {
            binding.tvNotes.setText(currentAd.getNotes());
            binding.cardNotes.setVisibility(View.VISIBLE);
        }

        // Setup Call button (available for both owner and requester viewing the ride)
        View.OnClickListener callListener = v -> {
            String phone = currentAd.getCreatorPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        };
        binding.btnCallDetail.setOnClickListener(callListener);

        if (isOwner) {
            binding.layoutOwnerActions.setVisibility(View.VISIBLE);
            binding.layoutRequesterActions.setVisibility(View.GONE);
            binding.tvRequestsHeader.setVisibility(View.VISIBLE);
            binding.rvRequests.setVisibility(View.VISIBLE);

            binding.btnMarkFilled.setOnClickListener(v ->
                    adsPresenter.updateAdStatus(adId, RideAd.STATUS_FILLED));
            binding.btnDeleteAd.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Delete Ride?")
                        .setMessage("This will remove the ride ad. This cannot be undone.")
                        .setPositiveButton("Delete", (d, w) -> adsPresenter.deleteAd(adId))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            binding.layoutOwnerActions.setVisibility(View.GONE);
            binding.layoutRequesterActions.setVisibility(View.VISIBLE);
            binding.tvRequestsHeader.setVisibility(View.GONE);
            binding.rvRequests.setVisibility(View.GONE);

            binding.btnSendRequest.setOnClickListener(v -> {
                JoinRequest req = new JoinRequest(adId, currentUid, session.getUserName(),
                        session.getUserPhone(), currentAd.getCreatorUid());
                requestPresenter.sendJoinRequest(req);
                binding.btnSendRequest.setEnabled(false);
            });
        }
    }

    private void loadRequests() {
        requestPresenter.loadRequestsForAd(adId);
    }

    private void checkMyRequest() {
        requestPresenter.checkExistingRequest(adId, currentUid, req -> {
            if (req == null) return;
            runOnUiThread(() -> {
                if (JoinRequest.STATUS_PENDING.equals(req.getStatus())) {
                    binding.btnSendRequest.setText("Request Pending...");
                    binding.btnSendRequest.setEnabled(false);
                } else if (JoinRequest.STATUS_ACCEPTED.equals(req.getStatus())) {
                    binding.btnSendRequest.setText("Open Chat 💬");
                    binding.btnSendRequest.setEnabled(true);
                    binding.btnSendRequest.setOnClickListener(v -> {
                        Intent i = new Intent(this, ChatActivity.class);
                        i.putExtra(AppConstants.KEY_CHAT_ROOM_ID, req.getChatRoomId());
                        i.putExtra(AppConstants.KEY_OTHER_USER_NAME, currentAd.getCreatorName());
                        startActivity(i);
                    });
                }
            });
        });
    }

    // RequestsAdapter callbacks (owner actions)
    @Override
    public void onAccept(JoinRequest request) {
        String chatRoomId = FirebaseManager.getInstance()
                .generateChatRoomId(adId, currentUid, request.getRequestorUid());
        requestPresenter.acceptRequest(request, chatRoomId);
    }

    @Override
    public void onReject(JoinRequest request) {
        requestPresenter.rejectRequest(request);
    }

    @Override
    public void onChat(JoinRequest request) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra(AppConstants.KEY_CHAT_ROOM_ID, request.getChatRoomId());
        i.putExtra(AppConstants.KEY_OTHER_USER_NAME, request.getRequestorName());
        startActivity(i);
    }

    // AdsPresenter callbacks
    @Override public void onAdsLoaded(List<RideAd> ads) {}
    @Override public void onAdPosted(String adId) {}
    @Override public void onAdDeleted() { finish(); }
    @Override public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    @Override public void onLoading(boolean isLoading) {}

    // RequestPresenter callbacks
    @Override public void onRequestSent() {
        binding.btnSendRequest.setText("Request Sent ✓");
        Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestsLoaded(List<JoinRequest> requests) {
        requestsAdapter.updateList(requests);
        binding.tvNoRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestUpdated(JoinRequest request) {
        loadRequests();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
