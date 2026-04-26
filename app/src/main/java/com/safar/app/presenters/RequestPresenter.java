package com.safar.app.presenters;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.JoinRequest;
import com.safar.app.models.Notification;

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
    private ValueEventListener requestsListener;

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
                .addOnSuccessListener(unused -> {
                    view.onRequestSent();
                    // Send notification to ad creator about new request
                    sendNewRequestNotification(request);
                })
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    /**
     * Sends a notification to the ad creator when a new join request is received
     */
    private void sendNewRequestNotification(JoinRequest request) {
        if (request.getAdCreatorUid() == null) return;
        
        Notification notification = Notification.newRequest(
                request.getRequestorUid(),
                request.getRequestorName(),
                null, // photo URL not available in request
                request.getAdId(),
                request.getRequestId()
        );
        
        firebaseManager.getUserNotificationsRef(request.getAdCreatorUid())
                .push()
                .setValue(notification);
    }

    public void loadRequestsForAd(String adId) {
        detach();
        requestsListener = new ValueEventListener() {
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
                if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                    view.onError(error.getMessage());
                }
            }
        };
        firebaseManager.getRequestsRef()
                .orderByChild("adId")
                .equalTo(adId)
                .addValueEventListener(requestsListener);
    }

    public void loadIncomingRequests(String currentUid) {
        detach();
        requestsListener = new ValueEventListener() {
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
                if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                    view.onError(error.getMessage());
                }
            }
        };
        firebaseManager.getRequestsRef()
                .orderByChild("adCreatorUid")
                .equalTo(currentUid)
                .addValueEventListener(requestsListener);
    }

    public void acceptRequest(JoinRequest request, String chatRoomId, String acceptorName) {
        request.setStatus(JoinRequest.STATUS_ACCEPTED);
        request.setChatRoomId(chatRoomId);
        firebaseManager.getRequestsRef().child(request.getRequestId()).setValue(request)
                .addOnSuccessListener(unused -> {
                    view.onRequestUpdated(request);
                    // Send notification to requester that their request was accepted
                    sendAcceptedNotification(request, acceptorName);
                })
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    /**
     * Legacy method for backward compatibility
     */
    public void acceptRequest(JoinRequest request, String chatRoomId) {
        acceptRequest(request, chatRoomId, "Driver");
    }

    /**
     * Sends a notification to the requester when their request is accepted
     */
    private void sendAcceptedNotification(JoinRequest request, String acceptorName) {
        if (request.getRequestorUid() == null) return;
        
        Notification notification = Notification.requestAccepted(
                request.getAdCreatorUid(),
                acceptorName,
                null, // photo URL
                request.getAdId(),
                request.getRequestId()
        );
        
        firebaseManager.getUserNotificationsRef(request.getRequestorUid())
                .push()
                .setValue(notification);
    }

    public void rejectRequest(JoinRequest request, String rejectorName) {
        request.setStatus(JoinRequest.STATUS_REJECTED);
        firebaseManager.getRequestsRef().child(request.getRequestId()).setValue(request)
                .addOnSuccessListener(unused -> {
                    view.onRequestUpdated(request);
                    // Send notification to requester that their request was declined
                    sendRejectedNotification(request, rejectorName);
                })
                .addOnFailureListener(e -> view.onError(e.getMessage()));
    }

    /**
     * Legacy method for backward compatibility
     */
    public void rejectRequest(JoinRequest request) {
        rejectRequest(request, "Driver");
    }

    /**
     * Sends a notification to the requester when their request is declined
     */
    private void sendRejectedNotification(JoinRequest request, String rejectorName) {
        if (request.getRequestorUid() == null) return;
        
        Notification notification = Notification.requestDeclined(
                request.getAdCreatorUid(),
                rejectorName,
                null, // photo URL
                request.getAdId(),
                request.getRequestId()
        );
        
        firebaseManager.getUserNotificationsRef(request.getRequestorUid())
                .push()
                .setValue(notification);
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

    public void detach() {
        if (requestsListener != null) {
            firebaseManager.getRequestsRef().removeEventListener(requestsListener);
            requestsListener = null;
        }
    }

    public interface OnCheckComplete {
        void onResult(JoinRequest existingRequest);
    }
}
