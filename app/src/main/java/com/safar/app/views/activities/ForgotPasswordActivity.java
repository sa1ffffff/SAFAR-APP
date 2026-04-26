package com.safar.app.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.firebase.FirebaseManager;

/**
 * SAFAR Forgot Password Activity
 * Uses Firebase Authentication's built-in password reset via email link.
 * 
 * SIMPLIFIED FLOW:
 * 1. User enters email
 * 2. Firebase sends password reset link to email
 * 3. User clicks link in email → Firebase handles password reset
 * 4. User returns to app and signs in with new password
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";
    
    private ForgotPasswordStateHolder stateHolder;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        auth = FirebaseManager.getInstance().getAuth();
        stateHolder = new ForgotPasswordStateHolder();
        ForgotPasswordBridgeKt.setupComposeUI(this, stateHolder);
    }
    
    /**
     * Send password reset email via Firebase Auth
     */
    public void handleRequestReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            stateHolder.setError("Please enter your email address");
            return;
        }
        
        String trimmedEmail = email.trim();
        
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            stateHolder.setError("Please enter a valid email address");
            return;
        }
        
        stateHolder.setLoading(true);
        stateHolder.clearError();
        
        Log.d(TAG, "Sending password reset email to: " + trimmedEmail);
        
        auth.sendPasswordResetEmail(trimmedEmail)
                .addOnSuccessListener(unused -> {
                    stateHolder.setLoading(false);
                    stateHolder.setEmailSent(true);
                    Toast.makeText(this, 
                            "Password reset link sent to " + trimmedEmail, 
                            Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Password reset email sent successfully");
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    String errorMsg = e.getMessage();
                    
                    if (errorMsg != null) {
                        if (errorMsg.contains("no user record") || 
                            errorMsg.contains("USER_NOT_FOUND")) {
                            stateHolder.setError("No account found with this email address");
                        } else if (errorMsg.contains("invalid email") || 
                                   errorMsg.contains("INVALID_EMAIL")) {
                            stateHolder.setError("Invalid email address format");
                        } else if (errorMsg.contains("too many requests") ||
                                   errorMsg.contains("TOO_MANY_ATTEMPTS")) {
                            stateHolder.setError("Too many attempts. Please try again later.");
                        } else {
                            stateHolder.setError("Failed to send reset email. Please try again.");
                        }
                    } else {
                        stateHolder.setError("Failed to send reset email. Please try again.");
                    }
                    
                    Log.e(TAG, "Password reset failed: " + errorMsg, e);
                });
    }
    
    /**
     * Navigation back handler
     */
    public void handleBack() {
        finish();
    }
    
    // Legacy methods - no longer needed but kept for bridge compatibility
    public void handleVerifyCode(String code) {
        // Not used with Firebase Auth reset link flow
    }
    
    public void handleResetPassword(String newPassword, String confirmPassword) {
        // Not used with Firebase Auth reset link flow
    }
    
    public void handleResendCode() {
        // Resend is just requesting reset again
        String email = stateHolder.getEmail();
        if (email != null && !email.isEmpty()) {
            stateHolder.setEmailSent(false);
            handleRequestReset(email);
        }
    }
}
