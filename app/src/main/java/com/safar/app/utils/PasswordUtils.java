package com.safar.app.utils;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * SAFAR Password Utilities
 * Handles password hashing and verification for the authentication system.
 * 
 * Uses SHA-256 with salt for password hashing.
 * This is stored in Firebase Realtime Database.
 * 
 * NOTE: For production apps, consider using Firebase Authentication's
 * built-in password handling. This utility is for the custom password
 * flow where we store hashed passwords ourselves.
 */
public class PasswordUtils {

    private static final String TAG = "PasswordUtils";
    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    
    /**
     * Hashes a password using SHA-256 with a random salt.
     * 
     * @param password The plain text password
     * @return A string in format "salt:hash" (Base64 encoded) or null if hashing fails
     */
    @Nullable
    public static String hashPassword(@NonNull String password) {
        if (password == null || password.isEmpty()) {
            Log.e(TAG, "Password cannot be null or empty");
            return null;
        }
        
        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash password with salt
            byte[] hash = hashWithSalt(password, salt);
            if (hash == null) return null;
            
            // Encode both salt and hash as Base64 and combine
            String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
            String hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP);
            
            return saltBase64 + ":" + hashBase64;
            
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }
    
    /**
     * Verifies a password against a stored hash.
     * 
     * @param password   The plain text password to verify
     * @param storedHash The stored hash in format "salt:hash"
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(@NonNull String password, @NonNull String storedHash) {
        if (password == null || password.isEmpty() || storedHash == null || storedHash.isEmpty()) {
            Log.e(TAG, "Password or stored hash cannot be null or empty");
            return false;
        }
        
        try {
            // Split stored hash into salt and hash
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                Log.e(TAG, "Invalid stored hash format");
                return false;
            }
            
            // Decode salt and hash from Base64
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);
            
            // Hash the input password with the same salt
            byte[] actualHash = hashWithSalt(password, salt);
            if (actualHash == null) return false;
            
            // Compare hashes using constant-time comparison
            return constantTimeEquals(expectedHash, actualHash);
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password", e);
            return false;
        }
    }
    
    /**
     * Validates password strength.
     * 
     * @param password The password to validate
     * @return null if valid, error message string if invalid
     */
    @Nullable
    public static String validatePasswordStrength(@NonNull String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        
        if (password.length() > 128) {
            return "Password is too long";
        }
        
        // Optional: Add more strength requirements
        // boolean hasUppercase = password.matches(".*[A-Z].*");
        // boolean hasLowercase = password.matches(".*[a-z].*");
        // boolean hasDigit = password.matches(".*\\d.*");
        // boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        
        return null; // Valid
    }
    
    /**
     * Generates a random reset code for password reset.
     * 
     * @return A 6-digit numeric code
     */
    @NonNull
    public static String generateResetCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }
    
    /**
     * Generates a secure random token for password reset links.
     * 
     * @return A 32-character hex token
     */
    @NonNull
    public static String generateResetToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Hashes a password with the given salt using SHA-256.
     */
    @Nullable
    private static byte[] hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Add salt first
            digest.update(salt);
            
            // Then add password
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] hash = digest.digest(passwordBytes);
            
            return hash;
            
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
