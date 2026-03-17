package com.safar.app.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.safar.app.utils.AppConstants;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    private FirebaseManager() {
        database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() { return auth; }

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

    public DatabaseReference getAdRef(String adId) {
        return getAdsRef().child(adId);
    }

    public DatabaseReference getChatRef(String chatRoomId) {
        return getChatsRef().child(chatRoomId);
    }

    public DatabaseReference getUserRef(String uid) {
        return getUsersRef().child(uid);
    }

    public String generateChatRoomId(String adId, String uid1, String uid2) {
        // Deterministic chat room ID - same regardless of who opens first
        String sorted = uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
        return adId + "_" + sorted;
    }
}
