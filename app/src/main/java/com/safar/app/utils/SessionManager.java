package com.safar.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SessionManager {

    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_EMAIL = "user_email";
    
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public boolean isSafetyShown() {
        return prefs.getBoolean(AppConstants.PREF_SAFETY_SHOWN, false);
    }

    public void setSafetyShown(boolean shown) {
        editor.putBoolean(AppConstants.PREF_SAFETY_SHOWN, shown).apply();
    }

    public String getUserName() {
        return prefs.getString(AppConstants.PREF_USER_NAME, "");
    }

    public void setUserName(String name) {
        editor.putString(AppConstants.PREF_USER_NAME, name).apply();
    }

    public String getUserPhone() {
        return prefs.getString(AppConstants.PREF_USER_PHONE, "");
    }

    public void setUserPhone(String phone) {
        editor.putString(AppConstants.PREF_USER_PHONE, phone).apply();
    }
    
    public String getUserEmail() {
        return prefs.getString(PREF_USER_EMAIL, "");
    }
    
    public void setUserEmail(String email) {
        editor.putString(PREF_USER_EMAIL, email).apply();
    }
    
    public String getUserId() {
        // First check SharedPreferences, then Firebase
        String storedId = prefs.getString(PREF_USER_ID, null);
        if (storedId != null && !storedId.isEmpty()) {
            return storedId;
        }
        // Fallback to Firebase user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    public void setUserId(String userId) {
        editor.putString(PREF_USER_ID, userId).apply();
    }

    public String getUserLanguage() {
        return prefs.getString(AppConstants.PREF_USER_LANGUAGE, "en");
    }

    public void setUserLanguage(String language) {
        editor.putString(AppConstants.PREF_USER_LANGUAGE, language).apply();
    }
    
    public String getUserPhotoUrl() {
        return prefs.getString(AppConstants.PREF_USER_PHOTO_URL, null);
    }
    
    public void setUserPhotoUrl(String photoUrl) {
        editor.putString(AppConstants.PREF_USER_PHOTO_URL, photoUrl).apply();
    }

    public boolean isLoggedIn() {
        // Check both Firebase auth and stored user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return true;
        }
        // Also check if we have a stored user ID (for phone-only auth)
        String storedId = prefs.getString(PREF_USER_ID, null);
        return storedId != null && !storedId.isEmpty();
    }

    public String getCurrentUserId() {
        return getUserId();
    }

    public void logout(Context context) {
        FirebaseAuth.getInstance().signOut();
        editor.clear().apply();
    }
}
