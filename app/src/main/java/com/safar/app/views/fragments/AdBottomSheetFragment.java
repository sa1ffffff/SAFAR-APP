package com.safar.app.views.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.databinding.FragmentAdBottomSheetBinding;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.RequestPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.DateUtils;
import com.safar.app.utils.SessionManager;
import com.safar.app.views.activities.AdDetailActivity;
import com.safar.app.views.activities.ChatActivity;

import java.util.List;

public class AdBottomSheetFragment extends BottomSheetDialogFragment
        implements RequestPresenter.RequestView {

    private static final String ARG_AD_ID = "arg_ad_id";
    private FragmentAdBottomSheetBinding binding;
    private RequestPresenter requestPresenter;
    private RideAd currentAd;
    private String adId;
    private SessionManager session;

    public static AdBottomSheetFragment newInstance(String adId) {
        AdBottomSheetFragment f = new AdBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AD_ID, adId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adId = getArguments() != null ? getArguments().getString(ARG_AD_ID) : null;
        session = new SessionManager(requireContext());
        requestPresenter = new RequestPresenter(this);

        if (adId == null) { dismiss(); return; }

        loadAdDetails();
    }

    private void loadAdDetails() {
        FirebaseManager.getInstance().getAdRef(adId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    currentAd = snapshot.getValue(RideAd.class);
                    if (currentAd == null) { dismiss(); return; }
                    currentAd.setAdId(adId);
                    populateUi();
                    checkExistingRequest();
                });
    }

    private void populateUi() {
        binding.tvRoute.setText(currentAd.getOrigin() + "  →  " + currentAd.getDestination());
        binding.tvTime.setText(DateUtils.formatDeparture(currentAd.getDepartureTime()));
        binding.tvSeats.setText(currentAd.getAvailableSeats() + " seat(s) available");
        binding.tvPostedBy.setText("Posted by " + currentAd.getCreatorName());
        if (currentAd.getVehicleDetails() != null && !currentAd.getVehicleDetails().isEmpty()) {
            binding.tvVehicle.setText(currentAd.getVehicleDetails());
            binding.tvVehicle.setVisibility(View.VISIBLE);
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        // Don't show request button for own ad
        if (currentAd.getCreatorUid().equals(currentUid)) {
            binding.btnRequestJoin.setVisibility(View.GONE);
            binding.btnViewDetail.setText("Manage Ride");
        }

        binding.btnRequestJoin.setOnClickListener(v -> sendJoinRequest());
        binding.btnViewDetail.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), AdDetailActivity.class);
            i.putExtra(AppConstants.KEY_AD_ID, adId);
            startActivity(i);
            dismiss();
        });

        // Setup Call button
        binding.btnCall.setOnClickListener(v -> {
            String phone = currentAd.getCreatorPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkExistingRequest() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUid == null) return;

        requestPresenter.checkExistingRequest(adId, currentUid, existingRequest -> {
            if (!isAdded()) return;
            if (existingRequest != null) {
                if (JoinRequest.STATUS_PENDING.equals(existingRequest.getStatus())) {
                    binding.btnRequestJoin.setText("Request Sent ✓");
                    binding.btnRequestJoin.setEnabled(false);
                } else if (JoinRequest.STATUS_ACCEPTED.equals(existingRequest.getStatus())) {
                    binding.btnRequestJoin.setText("Open Chat");
                    binding.btnRequestJoin.setEnabled(true);
                    binding.btnRequestJoin.setOnClickListener(v -> {
                        Intent i = new Intent(requireContext(), ChatActivity.class);
                        i.putExtra(AppConstants.KEY_CHAT_ROOM_ID, existingRequest.getChatRoomId());
                        i.putExtra(AppConstants.KEY_OTHER_USER_NAME, currentAd.getCreatorName());
                        startActivity(i);
                        dismiss();
                    });
                }
            }
        });
    }

    private void sendJoinRequest() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUid == null) return;

        JoinRequest request = new JoinRequest(
                adId, currentUid,
                session.getUserName(),
                session.getUserPhone(),
                currentAd.getCreatorUid()
        );
        binding.btnRequestJoin.setEnabled(false);
        requestPresenter.sendJoinRequest(request);
    }

    @Override
    public void onRequestSent() {
        if (!isAdded()) return;
        binding.btnRequestJoin.setText("Request Sent ✓");
        Toast.makeText(requireContext(), "Request sent! Waiting for approval.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestsLoaded(List<JoinRequest> requests) {}

    @Override
    public void onRequestUpdated(JoinRequest request) {}

    @Override
    public void onError(String message) {
        if (!isAdded()) return;
        binding.btnRequestJoin.setEnabled(true);
        Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
