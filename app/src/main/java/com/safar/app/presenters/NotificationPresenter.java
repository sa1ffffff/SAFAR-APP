package com.safar.app.presenters;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SAFAR Notification Presenter
 * 
 * Handles all notification-related operations:
 * - Loading user notifications
 * - Sending notifications
 * - Marking notifications as read
 * - Getting unread count
 */
public class NotificationPresenter {

    private static final String TAG = "NotificationPresenter";

    public interface NotificationView {
        void onNotificationsLoaded(List<Notification> notifications);
        void onUnreadCountUpdated(int count);
        void onNotificationSent();
        void onError(String message);
        void onLoading(boolean isLoading);
    }

    private final NotificationView view;
    private final FirebaseManager firebaseManager;
    private ValueEventListener notificationsListener;
    private ValueEventListener unreadCountListener;
    private String currentUserId;  // Track userId for proper listener cleanup

    public NotificationPresenter(NotificationView view) {
        this.view = view;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    /**
     * Loads all notifications for a user (sorted by timestamp, newest first)
     */
    public void loadNotifications(String userId) {
        // Validate userId before making Firebase call
        if (userId == null || userId.trim().isEmpty()) {
            Log.e(TAG, "Cannot load notifications: userId is null or empty");
            if (view != null) {
                view.onLoading(false);
                view.onError("Please log in to view notifications");
            }
            return;
        }

        // Store userId for cleanup
        this.currentUserId = userId;

        if (view != null) view.onLoading(true);

        try {
            Query query = firebaseManager.getUserNotificationsRef(userId)
                    .orderByChild("timestamp");

            notificationsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Notification> notifications = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Notification notification = child.getValue(Notification.class);
                            if (notification != null) {
                                notification.setNotificationId(child.getKey());
                                notifications.add(notification);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse notification: " + child.getKey(), e);
                            // Continue loading other notifications
                        }
                    }
                    // Sort by timestamp descending (newest first)
                    Collections.sort(notifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    
                    if (view != null) {
                        view.onLoading(false);
                        view.onNotificationsLoaded(notifications);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load notifications: " + error.getMessage(), error.toException());
                    if (view != null) {
                        view.onLoading(false);
                        // Provide more specific error messages based on error code
                        String errorMessage;
                        switch (error.getCode()) {
                            case DatabaseError.PERMISSION_DENIED:
                                errorMessage = "Permission denied. Please check Firebase rules.";
                                break;
                            case DatabaseError.DISCONNECTED:
                            case DatabaseError.NETWORK_ERROR:
                                errorMessage = "Network error. Please check your connection.";
                                break;
                            default:
                                errorMessage = "Failed to load notifications";
                        }
                        view.onError(errorMessage);
                    }
                }
            };

            query.addValueEventListener(notificationsListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception while setting up notifications listener", e);
            if (view != null) {
                view.onLoading(false);
                view.onError("Failed to load notifications");
            }
        }
    }

    /**
     * Listens for unread notification count
     */
    public void listenForUnreadCount(String userId) {
        Query query = firebaseManager.getUserNotificationsRef(userId)
                .orderByChild("read")
                .equalTo(false);

        unreadCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                if (view != null) {
                    view.onUnreadCountUpdated(count);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get unread count", error.toException());
            }
        };

        query.addValueEventListener(unreadCountListener);
    }

    /**
     * Marks a notification as read
     */
    public void markAsRead(String userId, String notificationId) {
        firebaseManager.getUserNotificationsRef(userId)
                .child(notificationId)
                .child("read")
                .setValue(true)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark as read", e));
    }

    /**
     * Marks all notifications as read
     */
    public void markAllAsRead(String userId) {
        firebaseManager.getUserNotificationsRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().child("read").setValue(true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark all as read", e));
    }

    /**
     * Sends a notification to a user
     */
    public void sendNotification(String toUserId, Notification notification) {
        DatabaseReference ref = firebaseManager.getUserNotificationsRef(toUserId).push();
        String notificationId = ref.getKey();
        notification.setNotificationId(notificationId);

        ref.setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification sent to " + toUserId);
                    if (view != null) view.onNotificationSent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);
                    if (view != null) view.onError("Failed to send notification");
                });
    }

    /**
     * Deletes a notification
     */
    public void deleteNotification(String userId, String notificationId) {
        firebaseManager.getUserNotificationsRef(userId)
                .child(notificationId)
                .removeValue()
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete notification", e));
    }

    /**
     * Clears all notifications for a user
     */
    public void clearAllNotifications(String userId) {
        firebaseManager.getUserNotificationsRef(userId)
                .removeValue()
                .addOnFailureListener(e -> Log.e(TAG, "Failed to clear notifications", e));
    }

    /**
     * Properly detaches all Firebase listeners to prevent memory leaks
     */
    public void detach() {
        if (currentUserId != null) {
            if (notificationsListener != null) {
                try {
                    firebaseManager.getUserNotificationsRef(currentUserId)
                            .removeEventListener(notificationsListener);
                } catch (Exception e) {
                    Log.w(TAG, "Error removing notifications listener", e);
                }
                notificationsListener = null;
            }
            if (unreadCountListener != null) {
                try {
                    firebaseManager.getUserNotificationsRef(currentUserId)
                            .removeEventListener(unreadCountListener);
                } catch (Exception e) {
                    Log.w(TAG, "Error removing unread count listener", e);
                }
                unreadCountListener = null;
            }
        }
        currentUserId = null;
    }
}
