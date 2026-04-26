package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;

import java.util.concurrent.TimeUnit;

/**
 * OTP Verification Activity
 * Used during Sign Up flow only - after verification, redirects to Password Setup
 */
public class OtpVerifyActivity extends AppCompatActivity {

    private OtpVerifyStateHolder                  stateHolder;
    private String                                verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth                          auth;
    private String                                phoneNumber;
    private SessionManager                        session;
    private FirebaseUser                          pendingUser;
    private boolean                               isSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth        = FirebaseManager.getInstance().getAuth();
        session     = new SessionManager(this);
        phoneNumber = normalizePhoneNumber(getIntent().getStringExtra(AppConstants.KEY_PHONE_NUMBER));
        isSignUp    = getIntent().getBooleanExtra("IS_SIGN_UP", false);

        stateHolder = new OtpVerifyStateHolder();
        stateHolder.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
        OtpVerifyBridgeKt.setupComposeUI(this, stateHolder);

        sendOtp(phoneNumber, false);
    }

    // Called by bridge when Verify button tapped
    public void handleVerify(String code) {
        if (code == null || code.length() != 6) return;
        verifyCode(code);
    }

    // Called by bridge when Resend tapped
    public void handleResend() {
        sendOtp(phoneNumber, true);
    }
    
    // Called by bridge when name is submitted (legacy flow)
    public void handleNameSubmit(String name) {
        if (pendingUser == null) {
            stateHolder.setLoading(false);
            Toast.makeText(this, "Error: User session lost. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For new sign-up flow, redirect to password setup
        if (isSignUp) {
            stateHolder.setShowNameDialog(false);
            navigateToPasswordSetup(pendingUser.getUid(), name);
            return;
        }
        
        // Legacy flow - save user directly
        User user = new User(pendingUser.getUid(), name, phoneNumber);
        FirebaseManager.getInstance().getUserRef(pendingUser.getUid()).setValue(user)
                .addOnSuccessListener(unused -> {
                    session.setUserName(name);
                    session.setUserPhone(phoneNumber);
                    stateHolder.setShowNameDialog(false);
                    navigateNext();
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    Toast.makeText(this, "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendOtp(String phone, boolean resend) {
        stateHolder.setLoading(true);
        
        // Validate phone number format
        if (phone == null || phone.isEmpty()) {
            stateHolder.setLoading(false);
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String normalizedPhone = normalizePhoneNumber(phone);
        if (!isPhoneNumberPlausible(normalizedPhone)) {
            stateHolder.setLoading(false);
            Toast.makeText(this, "Invalid phone number. Use format like +923001234567", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Log for debugging
        android.util.Log.d("OTP", "Sending OTP to: " + normalizedPhone + " (original: " + phone + ")");
        
        final String finalPhone = normalizedPhone;
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(finalPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        android.util.Log.d("OTP", "Auto-verification completed");
                        signInWithCredential(credential);
                    }
                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        stateHolder.setLoading(false);
                        String errorMsg = e.getMessage();
                        
                        // Provide user-friendly error messages
                        if (errorMsg != null) {
                            if (errorMsg.contains("missing valid app") || 
                                errorMsg.contains("invalid application") ||
                                errorMsg.contains("INVALID_APP_CREDENTIAL")) {
                                // SHA-1/SHA-256 fingerprint issue
                                Toast.makeText(OtpVerifyActivity.this,
                                    "Phone Auth not configured. Please contact support.", 
                                    Toast.LENGTH_LONG).show();
                                android.util.Log.e("OTP", 
                                    "Firebase Phone Auth Error: SHA fingerprints not configured in Firebase Console. " +
                                    "Run './gradlew signingReport' and add SHA-1/SHA-256 to Firebase Console > Project Settings > Your Apps");
                            } else if (errorMsg.contains("quota exceeded") || 
                                       errorMsg.contains("QUOTA_EXCEEDED")) {
                                Toast.makeText(OtpVerifyActivity.this,
                                    "Too many attempts. Please try again later.", 
                                    Toast.LENGTH_LONG).show();
                            } else if (errorMsg.contains("invalid phone") || 
                                       errorMsg.contains("INVALID_PHONE_NUMBER")) {
                                Toast.makeText(OtpVerifyActivity.this,
                                    "Invalid phone number format. Please include country code.", 
                                    Toast.LENGTH_LONG).show();
                            } else if (errorMsg.contains("not enabled") || 
                                       errorMsg.contains("OPERATION_NOT_ALLOWED")) {
                                Toast.makeText(OtpVerifyActivity.this,
                                    "Phone authentication not enabled. Please contact support.", 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(OtpVerifyActivity.this,
                                    "Verification failed: " + errorMsg, 
                                    Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(OtpVerifyActivity.this,
                                "Verification failed. Please try again.", 
                                Toast.LENGTH_LONG).show();
                        }
                        android.util.Log.e("OTP", "onVerificationFailed: " + errorMsg, e);
                    }
                    @Override
                    public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        android.util.Log.d("OTP", "Code sent successfully to: " + finalPhone);
                        verificationId = id;
                        resendToken    = token;
                        stateHolder.setLoading(false);
                        startResendTimer();
                        Toast.makeText(OtpVerifyActivity.this, 
                            "OTP sent to " + finalPhone, Toast.LENGTH_SHORT).show();
                    }
                });

        if (resend && resendToken != null) builder.setForceResendingToken(resendToken);
        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        String normalizedPhone = phone.trim();
        normalizedPhone = normalizedPhone.replaceAll("[\\s\\-()]", "");

        if (normalizedPhone.startsWith("00")) {
            normalizedPhone = "+" + normalizedPhone.substring(2);
        }
        if (!normalizedPhone.startsWith("+")) {
            normalizedPhone = "+" + normalizedPhone;
        }

        if (normalizedPhone.length() > 1) {
            normalizedPhone = "+" + normalizedPhone.substring(1).replaceAll("[^0-9]", "");
        }
        return normalizedPhone;
    }

    private boolean isPhoneNumberPlausible(String phone) {
        if (phone == null || !phone.startsWith("+")) {
            return false;
        }
        int length = phone.length();
        return length >= 8 && length <= 16;
    }

    private void verifyCode(String code) {
        if (verificationId == null) {
            Toast.makeText(this, "Please wait for OTP to arrive.", Toast.LENGTH_SHORT).show();
            return;
        }
        stateHolder.setLoading(true);
        signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code));
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    if (result.getUser() != null) checkOrCreateUser(result.getUser());
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    Toast.makeText(this, "Invalid OTP. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkOrCreateUser(FirebaseUser firebaseUser) {
        FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // New user - for sign up flow, go to password setup
                        stateHolder.setLoading(false);
                        pendingUser = firebaseUser;
                        
                        if (isSignUp) {
                            // New sign-up flow - redirect to password setup directly
                            navigateToPasswordSetup(firebaseUser.getUid(), null);
                        } else {
                            // Legacy flow - show name input dialog
                            stateHolder.showNameInput();
                        }
                    } else {
                        // Existing user
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            session.setUserId(firebaseUser.getUid());
                            session.setUserName(user.getName());
                            session.setUserPhone(user.getPhone());
                            if (user.getEmail() != null) {
                                session.setUserEmail(user.getEmail());
                            }
                            
                            // Check if user has password set
                            if (isSignUp && (user.getPasswordHash() == null || user.getPasswordHash().isEmpty())) {
                                // User exists but no password - redirect to password setup
                                navigateToPasswordSetup(firebaseUser.getUid(), user.getName());
                                return;
                            }
                        }
                        navigateNext();
                    }
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startResendTimer() {
        stateHolder.setResendSeconds(60);
        new CountDownTimer(60000, 1000) {
            public void onTick(long ms) {
                stateHolder.setResendSeconds((int) (ms / 1000));
            }
            public void onFinish() {
                stateHolder.setResendSeconds(0);
            }
        }.start();
    }

    /**
     * Navigate to Password Setup for new users during sign up
     */
    private void navigateToPasswordSetup(String userId, String name) {
        Intent intent = new Intent(this, PasswordSetupActivity.class);
        intent.putExtra(PasswordSetupActivity.EXTRA_UID, userId);
        intent.putExtra(PasswordSetupActivity.EXTRA_NAME, name);
        intent.putExtra(PasswordSetupActivity.EXTRA_PHONE, phoneNumber);
        intent.putExtra(PasswordSetupActivity.EXTRA_AUTH_PROVIDER, "phone");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateNext() {
        Intent intent = new SessionManager(this).isSafetyShown()
                ? new Intent(this, MainActivity.class)
                : new Intent(this, SafetyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
