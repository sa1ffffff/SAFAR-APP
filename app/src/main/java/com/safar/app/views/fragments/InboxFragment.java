package com.safar.app.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.databinding.FragmentInboxBinding;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.presenters.RequestPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.views.activities.ChatActivity;
import com.safar.app.views.adapters.InboxAdapter;

import java.util.ArrayList;
import java.util.List;

public class InboxFragment extends Fragment implements RequestPresenter.RequestView,
        InboxAdapter.OnRequestActionListener {

    private FragmentInboxBinding binding;
    private RequestPresenter requestPresenter;
    private InboxAdapter adapter;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInboxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        requestPresenter = new RequestPresenter(this);
        adapter = new InboxAdapter(new ArrayList<>(), this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        loadInboxRequests();
    }

    private void loadInboxRequests() {
        // Load requests where currentUser is the ad creator (incoming)
        FirebaseManager.getInstance().getRequestsRef()
                .orderByChild("adCreatorUid")
                .equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<JoinRequest> requests = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            JoinRequest req = ds.getValue(JoinRequest.class);
                            if (req != null) {
                                req.setRequestId(ds.getKey());
                                requests.add(req);
                            }
                        }
                        if (!isAdded()) return;
                        adapter.updateList(requests);
                        binding.tvEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public void onAccept(JoinRequest request) {
        if (currentUid == null) return;
        String chatRoomId = FirebaseManager.getInstance()
                .generateChatRoomId(request.getAdId(), currentUid, request.getRequestorUid());
        requestPresenter.acceptRequest(request, chatRoomId);
    }

    @Override
    public void onReject(JoinRequest request) {
        requestPresenter.rejectRequest(request);
    }

    @Override
    public void onOpenChat(JoinRequest request) {
        Intent i = new Intent(requireContext(), ChatActivity.class);
        i.putExtra(AppConstants.KEY_CHAT_ROOM_ID, request.getChatRoomId());
        i.putExtra(AppConstants.KEY_OTHER_USER_NAME, request.getRequestorName());
        startActivity(i);
    }

    @Override public void onRequestSent() {}
    @Override public void onRequestsLoaded(List<JoinRequest> requests) {}

    @Override
    public void onRequestUpdated(JoinRequest request) {
        if (!isAdded()) return;
        if (JoinRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
            // Auto-open chat after accepting
            Intent i = new Intent(requireContext(), ChatActivity.class);
            i.putExtra(AppConstants.KEY_CHAT_ROOM_ID, request.getChatRoomId());
            i.putExtra(AppConstants.KEY_OTHER_USER_NAME, request.getRequestorName());
            startActivity(i);
        }
    }

    @Override
    public void onError(String message) {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
