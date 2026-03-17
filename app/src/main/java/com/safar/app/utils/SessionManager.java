package com.safar.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SessionManager {

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

    public boolean isLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void logout(Context context) {
        FirebaseAuth.getInstance().signOut();
        editor.clear().apply();
    }
}
