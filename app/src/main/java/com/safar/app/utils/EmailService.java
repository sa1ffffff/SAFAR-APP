package com.safar.app.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SAFAR Email Service
 * Integrates with Email.js for sending transactional emails.
 * 
 * NOTE: Password reset is handled by Firebase Auth directly.
 * This service is used for welcome emails only.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * SETUP INSTRUCTIONS:
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. Go to https://www.emailjs.com/ and create a FREE account
 * 
 * 2. CREATE EMAIL SERVICE:
 *    - Dashboard > Email Services > Add New Service
 *    - Select Gmail (or your email provider)
 *    - Connect your Gmail account
 *    - Note your SERVICE_ID (e.g., "service_abc123")
 * 
 * 3. CREATE EMAIL TEMPLATE:
 * 
 *    Template: Welcome Email
 *    - Name: template_welcome
 *    - Subject: Welcome to SAFAR - Your Carpool Journey Begins! 🚗
 *    - Variables used: {{user_name}}, {{user_email}}, {{app_name}}
 *    - Body: Copy from SAFAR_EMAIL_TEMPLATES.html or EMAIL_TEMPLATES.md
 * 
 * 4. GET API KEYS:
 *    - Dashboard > Account > API Keys
 *    - Copy your PUBLIC KEY
 * 
 * 5. UPDATE CONFIGURATION BELOW:
 *    - Replace YOUR_EMAILJS_PUBLIC_KEY with your public key
 *    - Replace YOUR_EMAILJS_SERVICE_ID with your service ID
 * 
 * Email.js REST API: https://www.emailjs.com/docs/rest-api/send/
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class EmailService {

    private static final String TAG = "EmailService";
    private static final String EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION - Replace these with your Email.js credentials
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Your Email.js Public Key (found in Account > API Keys)
     * TODO: Move to BuildConfig or secure storage in production
     */
    private static final String PUBLIC_KEY = "cPZ4ROW8XEu3zTF_W";
    
    /**
     * Your Email.js Service ID (found in Email Services)
     */
    private static final String SERVICE_ID = "service_ti82hjp";
    
    /**
     * Template ID for welcome email
     */
    private static final String TEMPLATE_WELCOME = "template_welcome";
    
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static volatile EmailService instance;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    private EmailService() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @NonNull
    public static EmailService getInstance() {
        if (instance == null) {
            synchronized (EmailService.class) {
                if (instance == null) {
                    instance = new EmailService();
                }
            }
        }
        return instance;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sends a welcome email to newly registered users.
     * 
     * @param userName    User's display name
     * @param userEmail   User's email address
     * @param callback    Callback for success/failure
     */
    public void sendWelcomeEmail(String userName, String userEmail, EmailCallback callback) {
        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("user_name", userName);
            templateParams.put("user_email", userEmail);
            templateParams.put("app_name", "SAFAR");
            
            sendEmail(TEMPLATE_WELCOME, templateParams, callback);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing welcome email", e);
            notifyFailure(callback, "Failed to prepare email: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE EMAIL SENDING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void sendEmail(String templateId, JSONObject templateParams, EmailCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Build request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("service_id", SERVICE_ID);
                requestBody.put("template_id", templateId);
                requestBody.put("user_id", PUBLIC_KEY);
                requestBody.put("template_params", templateParams);
                
                // Create connection
                URL url = new URL(EMAILJS_API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                
                // Write request body
                String jsonBody = requestBody.toString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Read response
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Email sent successfully via template: " + templateId);
                    notifySuccess(callback);
                } else {
                    // Read error response
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    String errorMsg = "Email failed: HTTP " + responseCode + " - " + response;
                    Log.e(TAG, errorMsg);
                    notifyFailure(callback, errorMsg);
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Network error sending email", e);
                notifyFailure(callback, "Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending email", e);
                notifyFailure(callback, "Error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACK HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void notifySuccess(EmailCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onSuccess);
        }
    }
    
    private void notifyFailure(EmailCallback callback, String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onFailure(error));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACK INTERFACE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Callback interface for email operations.
     */
    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION CHECK
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if Email.js is properly configured.
     * @return true if configuration looks valid
     */
    public static boolean isConfigured() {
        return !PUBLIC_KEY.equals("YOUR_EMAILJS_PUBLIC_KEY") 
                && !SERVICE_ID.equals("YOUR_EMAILJS_SERVICE_ID");
    }
    
    /**
     * Returns the current public key (for debugging).
     * In production, don't expose this.
     */
    public static String getPublicKeyPreview() {
        if (PUBLIC_KEY.length() > 8) {
            return PUBLIC_KEY.substring(0, 4) + "****" + PUBLIC_KEY.substring(PUBLIC_KEY.length() - 4);
        }
        return "NOT_CONFIGURED";
    }
}
