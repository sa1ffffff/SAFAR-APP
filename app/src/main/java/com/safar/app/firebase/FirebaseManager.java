package com.safar.app.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.safar.app.utils.AppConstants;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static volatile FirebaseManager instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance();
        if (database == null) {
            throw new RuntimeException("Failed to initialize FirebaseDatabase");
        }
        try {
            database.setPersistenceEnabled(true);
        } catch (DatabaseException e) {
            Log.w(TAG, "Persistence already enabled or failed to enable: " + e.getMessage());
        }
        auth = FirebaseAuth.getInstance();
        if (auth == null) {
            throw new RuntimeException("Failed to initialize FirebaseAuth");
        }
        storage = FirebaseStorage.getInstance();
        if (storage == null) {
            throw new RuntimeException("Failed to initialize FirebaseStorage");
        }
    }

    @NonNull
    public static FirebaseManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseManager.class) {
                if (instance == null) {
                    instance = new FirebaseManager();
                }
            }
        }
        return instance;
    }

    @NonNull
    public FirebaseAuth getAuth() {
        return auth;
    }

    @NonNull
    public FirebaseStorage getStorage() {
        return storage;
    }

    @NonNull
    public DatabaseReference getAdsRef() {
        return database.getReference(AppConstants.NODE_ADS);
    }

    @NonNull
    public DatabaseReference getUsersRef() {
        return database.getReference(AppConstants.NODE_USERS);
    }

    @NonNull
    public DatabaseReference getChatsRef() {
        return database.getReference(AppConstants.NODE_CHATS);
    }

    @NonNull
    public DatabaseReference getRequestsRef() {
        return database.getReference(AppConstants.NODE_REQUESTS);
    }

    @NonNull
    public DatabaseReference getConversationsRef() {
        return database.getReference(AppConstants.NODE_CONVERSATIONS);
    }

    @NonNull
    public DatabaseReference getMessagesRef() {
        return database.getReference(AppConstants.NODE_MESSAGES);
    }

    @NonNull
    public DatabaseReference getReviewsRef() {
        return database.getReference(AppConstants.NODE_REVIEWS);
    }

    @NonNull
    public DatabaseReference getNotificationsRef() {
        return database.getReference(AppConstants.NODE_NOTIFICATIONS);
    }

    @NonNull
    public DatabaseReference getUserNotificationsRef(@NonNull String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return getNotificationsRef().child(uid);
    }

    @NonNull
    public DatabaseReference getAdRef(@NonNull String adId) {
        if (adId == null || adId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ad ID cannot be null or empty");
        }
        return getAdsRef().child(adId);
    }

    @NonNull
    public DatabaseReference getChatRef(@NonNull String chatRoomId) {
        if (chatRoomId == null || chatRoomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat room ID cannot be null or empty");
        }
        return getChatsRef().child(chatRoomId);
    }

    @NonNull
    public DatabaseReference getUserRef(@NonNull String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return getUsersRef().child(uid);
    }

    @NonNull
    public DatabaseReference getConversationRef(@NonNull String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }
        return getConversationsRef().child(conversationId);
    }

    @NonNull
    public DatabaseReference getConversationMessagesRef(@NonNull String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }
        return getMessagesRef().child(conversationId);
    }

    @NonNull
    public StorageReference getProfilePicturesRef() {
        return storage.getReference().child("profile_pictures");
    }

    @NonNull
    public StorageReference getUserProfilePictureRef(@NonNull String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return getProfilePicturesRef().child(uid + ".jpg");
    }

    @NonNull
    public String generateChatRoomId(@NonNull String adId, @NonNull String uid1, @NonNull String uid2) {
        if (adId == null || adId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ad ID cannot be null or empty");
        }
        if (uid1 == null || uid1.trim().isEmpty() || uid2 == null || uid2.trim().isEmpty()) {
            throw new IllegalArgumentException("User IDs cannot be null or empty");
        }
        String sorted = uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
        return adId + "_" + sorted;
    }

    @NonNull
    public String generateConversationId(@NonNull String uid1, @NonNull String uid2) {
        if (uid1 == null || uid1.trim().isEmpty() || uid2 == null || uid2.trim().isEmpty()) {
            throw new IllegalArgumentException("User IDs cannot be null or empty");
        }
        if (uid1.equals(uid2)) {
            throw new IllegalArgumentException("Cannot create conversation with the same user");
        }
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}
