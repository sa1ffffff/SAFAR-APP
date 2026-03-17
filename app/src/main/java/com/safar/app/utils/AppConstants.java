package com.safar.app.utils;

public class AppConstants {

    // Firebase DB Nodes
    public static final String NODE_ADS = "ads";
    public static final String NODE_USERS = "users";
    public static final String NODE_CHATS = "chats";
    public static final String NODE_REQUESTS = "joinRequests";

    // SharedPreferences
    public static final String PREFS_NAME = "SafarPrefs";
    public static final String PREF_SAFETY_SHOWN = "safety_shown";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_PHONE = "user_phone";

    // Intent Keys
    public static final String KEY_AD_ID = "ad_id";
    public static final String KEY_CHAT_ROOM_ID = "chat_room_id";
    public static final String KEY_OTHER_USER_NAME = "other_user_name";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_REQUEST_ID = "request_id";

    // Ad Status
    public static final String AD_STATUS_OPEN = "OPEN";
    public static final String AD_STATUS_FILLED = "FILLED";
    public static final String AD_STATUS_COMPLETED = "COMPLETED";

    // Default Pakistan map center (Lahore)
    public static final double DEFAULT_LAT = 31.5204;
    public static final double DEFAULT_LNG = 74.3587;

    // Date formats
    public static final String DATE_FORMAT_DISPLAY = "EEE, dd MMM • hh:mm a";
    public static final String DATE_FORMAT_SHORT = "dd MMM, hh:mm a";

    private AppConstants() {}
}
