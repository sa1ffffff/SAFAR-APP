package com.safar.app.presenters;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;

import java.util.ArrayList;
import java.util.List;

public class RequestPresenter {

    public interface RequestView {
        void onRequestSent();
        void onRequestsLoaded(List<JoinRequest> requests);
        void onRequestUpdated(JoinRequest request);
        void onError(String message);
    }

    private final RequestView view;
    private final FirebaseManager firebaseManager;

    public RequestPresenter(RequestView view) {
        this.view = view;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    public void sendJoinRequest(JoinRequest request) {
        String requestId = firebaseManager.getRequestsRef().push().getKey();
        if (requestId == null) {
            view.onError("Failed to send request.");
            return;
        }
        request.setRequestId(requestId);
        firebaseManager.getRequestsRef().child(requestId).setValue(request)
                .addOnSuccessListener(unused -> view.onRequestSent())
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    public void loadRequestsForAd(String adId) {
        firebaseManager.getRequestsRef()
                .orderByChild("adId")
                .equalTo(adId)
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
                        view.onRequestsLoaded(requests);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        view.onError(error.getMessage());
                    }
                });
    }

    public void acceptRequest(JoinRequest request, String chatRoomId) {
        request.setStatus(JoinRequest.STATUS_ACCEPTED);
        request.setChatRoomId(chatRoomId);
        firebaseManager.getRequestsRef().child(request.getRequestId()).setValue(request)
                .addOnSuccessListener(unused -> view.onRequestUpdated(request))
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    public void rejectRequest(JoinRequest request) {
        request.setStatus(JoinRequest.STATUS_REJECTED);
        firebaseManager.getRequestsRef().child(request.getRequestId()).setValue(request)
                .addOnSuccessListener(unused -> view.onRequestUpdated(request))
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    public void checkExistingRequest(String adId, String requestorUid,
                                      OnCheckComplete callback) {
        firebaseManager.getRequestsRef()
                .orderByChild("adId")
                .equalTo(adId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            JoinRequest req = ds.getValue(JoinRequest.class);
                            if (req != null && requestorUid.equals(req.getRequestorUid())) {
                                found = true;
                                callback.onResult(req);
                                break;
                            }
                        }
                        if (!found) callback.onResult(null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(null);
                    }
                });
    }

    public interface OnCheckComplete {
        void onResult(JoinRequest existingRequest);
    }
}
