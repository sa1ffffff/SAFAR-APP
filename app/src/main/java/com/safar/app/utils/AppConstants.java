package com.safar.app.utils;

public class AppConstants {

    // Firebase DB Nodes
    public static final String NODE_ADS = "ads";
    public static final String NODE_USERS = "users";
    public static final String NODE_CHATS = "chats";
    public static final String NODE_REQUESTS = "joinRequests";
    public static final String NODE_CONVERSATIONS = "conversations";
    public static final String NODE_MESSAGES = "messages";
    public static final String NODE_REVIEWS = "reviews";
    public static final String NODE_NOTIFICATIONS = "notifications";

    // SharedPreferences
    public static final String PREFS_NAME = "SafarPrefs";
    public static final String PREF_SAFETY_SHOWN = "safety_shown";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_PHONE = "user_phone";
    public static final String PREF_USER_LANGUAGE = "user_language";
    public static final String PREF_USER_PHOTO_URL = "user_photo_url";

    // Intent Keys
    public static final String KEY_AD_ID = "ad_id";
    public static final String KEY_CHAT_ROOM_ID = "chat_room_id";
    public static final String KEY_OTHER_USER_NAME = "other_user_name";
    public static final String KEY_OTHER_USER_ID = "other_user_id";
    public static final String KEY_OTHER_USER_PHOTO = "other_user_photo";
    public static final String KEY_CONVERSATION_ID = "conversation_id";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_REQUEST_ID = "request_id";

    // Ad Status
    public static final String AD_STATUS_OPEN = "OPEN";
    public static final String AD_STATUS_FILLED = "FILLED";
    public static final String AD_STATUS_COMPLETED = "COMPLETED";

    // Profile Picture
    public static final long MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024; // 5MB

    // Default Pakistan map center (Lahore)
    public static final double DEFAULT_LAT = 31.5204;
    public static final double DEFAULT_LNG = 74.3587;

    // Date formats
    public static final String DATE_FORMAT_DISPLAY = "EEE, dd MMM • hh:mm a";
    public static final String DATE_FORMAT_SHORT = "dd MMM, hh:mm a";

    private AppConstants() {}
}
