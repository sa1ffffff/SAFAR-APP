package com.safar.app.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.safar.app.utils.AppConstants;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() { return auth; }
    
    public FirebaseStorage getStorage() { return storage; }

    public DatabaseReference getAdsRef() {
        return database.getReference(AppConstants.NODE_ADS);
    }

    public DatabaseReference getUsersRef() {
        return database.getReference(AppConstants.NODE_USERS);
    }

    public DatabaseReference getChatsRef() {
        return database.getReference(AppConstants.NODE_CHATS);
    }

    public DatabaseReference getRequestsRef() {
        return database.getReference(AppConstants.NODE_REQUESTS);
    }
    
    public DatabaseReference getConversationsRef() {
        return database.getReference(AppConstants.NODE_CONVERSATIONS);
    }
    
    public DatabaseReference getMessagesRef() {
        return database.getReference(AppConstants.NODE_MESSAGES);
    }

    public DatabaseReference getAdRef(String adId) {
        return getAdsRef().child(adId);
    }

    public DatabaseReference getChatRef(String chatRoomId) {
        return getChatsRef().child(chatRoomId);
    }

    public DatabaseReference getUserRef(String uid) {
        return getUsersRef().child(uid);
    }
    
    public DatabaseReference getConversationRef(String conversationId) {
        return getConversationsRef().child(conversationId);
    }
    
    public DatabaseReference getConversationMessagesRef(String conversationId) {
        return getMessagesRef().child(conversationId);
    }
    
    // Storage references for profile pictures
    public StorageReference getProfilePicturesRef() {
        return storage.getReference().child("profile_pictures");
    }
    
    public StorageReference getUserProfilePictureRef(String uid) {
        return getProfilePicturesRef().child(uid + ".jpg");
    }

    public String generateChatRoomId(String adId, String uid1, String uid2) {
        // Deterministic chat room ID - same regardless of who opens first
        String sorted = uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
        return adId + "_" + sorted;
    }
    
    /**
     * Generate a user-to-user conversation ID (not linked to rides)
     * This creates a deterministic ID for any two users
     */
    public String generateConversationId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}
