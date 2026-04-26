package com.safar.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    private String uid;
    private String name;
    private String phone;
    private String email;
    private String photoUrl;
    private String bio;
    private long joinedAt;
    private int totalRides;
    private double rating;
    private int ratingCount;
    private String fcmToken;
    private boolean pushNotifications;
    private boolean emailNotifications;
    
    // Authentication fields for the new sign up/sign in system
    private String passwordHash;          // Hashed password (salt:hash format)
    private boolean isVerified;           // Whether phone/email is verified
    private String authProvider;          // "phone", "google", or "email"
    private String resetCode;             // Temporary reset code for forgot password
    private long resetCodeExpiry;         // Expiry timestamp for reset code

    public User() {}

    public User(String uid, String name, String phone) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.joinedAt = System.currentTimeMillis();
        this.totalRides = 0;
        this.rating = 0.0;
        this.ratingCount = 0;
        this.pushNotifications = true;
        this.emailNotifications = true;
        this.isVerified = false;
        this.authProvider = "phone";
    }
    
    /**
     * Constructor for full registration with password
     */
    public User(String uid, String name, String phone, String email, String passwordHash, String authProvider) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authProvider = authProvider;
        this.joinedAt = System.currentTimeMillis();
        this.totalRides = 0;
        this.rating = 0.0;
        this.ratingCount = 0;
        this.pushNotifications = true;
        this.emailNotifications = true;
        this.isVerified = true; // Verified during signup via OTP/Google
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public int getTotalRides() { return totalRides; }
    public void setTotalRides(int totalRides) { this.totalRides = totalRides; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public boolean isPushNotifications() { return pushNotifications; }
    public void setPushNotifications(boolean pushNotifications) { this.pushNotifications = pushNotifications; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    
    // New authentication getters/setters
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
    
    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    
    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }
    
    public long getResetCodeExpiry() { return resetCodeExpiry; }
    public void setResetCodeExpiry(long resetCodeExpiry) { this.resetCodeExpiry = resetCodeExpiry; }
    
    /**
     * Checks if reset code is still valid (not expired).
     * Reset codes expire after 15 minutes.
     */
    public boolean isResetCodeValid() {
        return resetCode != null && 
               !resetCode.isEmpty() && 
               System.currentTimeMillis() < resetCodeExpiry;
    }
    
    /**
     * Clears the reset code after successful password reset.
     */
    public void clearResetCode() {
        this.resetCode = null;
        this.resetCodeExpiry = 0;
    }
}
