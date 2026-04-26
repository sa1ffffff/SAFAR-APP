package com.safar.app.views.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.safar.app.models.Notification;
import com.safar.app.presenters.NotificationPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.DateUtils;
import com.safar.app.utils.SessionManager;
import com.safar.app.views.ui.screens.NotificationUiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * SAFAR Notifications Activity
 * 
 * Displays Instagram-style notifications for ride updates.
 * Shows accepted/declined requests, new requests, completed rides, and reviews.
 */
public class NotificationsActivity extends AppCompatActivity implements NotificationPresenter.NotificationView {

    private NotificationsStateHolder stateHolder;
    private NotificationPresenter presenter;
    private SessionManager session;
    private String currentUserId;

    // Permission request launcher
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications disabled. You can enable them in settings.", 
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        currentUserId = session.getCurrentUserId();

        if (currentUserId == null) {
            finish();
            return;
        }

        stateHolder = new NotificationsStateHolder();
        presenter = new NotificationPresenter(this);

        // Setup Compose UI
        NotificationsBridgeKt.setupComposeUI(this, stateHolder);

        // Check and request notification permission (Android 13+)
        checkNotificationPermission();

        // Load notifications
        presenter.loadNotifications(currentUserId);
    }

    /**
     * Checks and requests notification permission for Android 13+
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Handles notification click - navigates to relevant screen
     */
    public void handleNotificationClick(NotificationUiModel notification) {
        // Mark as read
        presenter.markAsRead(currentUserId, notification.getId());

        // Navigate based on notification type
        switch (notification.getType()) {
            case Notification.TYPE_REQUEST_ACCEPTED:
            case Notification.TYPE_REQUEST_DECLINED:
            case Notification.TYPE_NEW_REQUEST:
                // Open ad detail
                if (notification.getRelatedAdId() != null) {
                    Intent intent = new Intent(this, AdDetailActivity.class);
                    intent.putExtra(AppConstants.KEY_AD_ID, notification.getRelatedAdId());
                    startActivity(intent);
                }
                break;
                
            case Notification.TYPE_RIDE_COMPLETED:
            case Notification.TYPE_NEW_REVIEW:
                // Open profile
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                startActivity(profileIntent);
                break;
                
            case Notification.TYPE_NEW_MESSAGE:
                // Open chat
                if (notification.getRelatedAdId() != null) {
                    Intent chatIntent = new Intent(this, ChatActivity.class);
                    chatIntent.putExtra(AppConstants.KEY_CHAT_ROOM_ID, notification.getRelatedAdId());
                    chatIntent.putExtra(AppConstants.KEY_OTHER_USER_NAME, notification.getFromUserName());
                    startActivity(chatIntent);
                }
                break;
        }
    }

    /**
     * Marks all notifications as read
     */
    public void handleMarkAllRead() {
        presenter.markAllAsRead(currentUserId);
    }

    @Override
    public void onNotificationsLoaded(List<Notification> notifications) {
        runOnUiThread(() -> {
            List<NotificationUiModel> uiModels = new ArrayList<>();
            int unreadCount = 0;
            
            for (Notification n : notifications) {
                uiModels.add(new NotificationUiModel(
                        n.getNotificationId(),
                        n.getType(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getFromUserId(),  // fromUserId - for profile navigation
                        n.getFromUserName() != null ? n.getFromUserName() : "User",
                        n.getFromUserPhoto(),
                        n.getRelatedAdId(),
                        n.getRelatedRequestId(),
                        n.getTimestamp(),
                        n.isRead(),
                        DateUtils.timeAgo(n.getTimestamp())
                ));
                
                if (!n.isRead()) {
                    unreadCount++;
                }
            }
            
            stateHolder.setNotifications(uiModels);
            stateHolder.setUnreadCount(unreadCount);
            stateHolder.setLoading(false);
        });
    }

    /**
     * Navigates to another user's profile
     */
    public void navigateToUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(AppConstants.KEY_OTHER_USER_ID, userId);
        startActivity(intent);
    }

    @Override
    public void onUnreadCountUpdated(int count) {
        runOnUiThread(() -> stateHolder.setUnreadCount(count));
    }

    @Override
    public void onNotificationSent() {
        // Not used in this activity
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            stateHolder.setLoading(false);
            stateHolder.setError(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onLoading(boolean isLoading) {
        runOnUiThread(() -> stateHolder.setLoading(isLoading));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.detach();
        }
    }
}
