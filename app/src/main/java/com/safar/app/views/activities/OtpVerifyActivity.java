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

public class OtpVerifyActivity extends AppCompatActivity {

    private OtpVerifyStateHolder                  stateHolder;
    private String                                verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth                          auth;
    private String                                phoneNumber;
    private SessionManager                        session;
    private FirebaseUser                          pendingUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth        = FirebaseManager.getInstance().getAuth();
        session     = new SessionManager(this);
        phoneNumber = getIntent().getStringExtra(AppConstants.KEY_PHONE_NUMBER);

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
    
    // Called by bridge when name is submitted
    public void handleNameSubmit(String name) {
        if (pendingUser == null) {
            stateHolder.setLoading(false);
            Toast.makeText(this, "Error: User session lost. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
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
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }
                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        stateHolder.setLoading(false);
                        Toast.makeText(OtpVerifyActivity.this,
                                "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        resendToken    = token;
                        stateHolder.setLoading(false);
                        startResendTimer();
                    }
                });

        if (resend && resendToken != null) builder.setForceResendingToken(resendToken);
        PhoneAuthProvider.verifyPhoneNumber(builder.build());
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
                        stateHolder.setLoading(false);
                        pendingUser = firebaseUser;
                        stateHolder.showNameInput();
                    } else {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            session.setUserName(user.getName());
                            session.setUserPhone(user.getPhone());
                            if (user.getPhotoUrl() != null) {
                                // Store photo URL in session for quick access
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

    private void navigateNext() {
        Intent intent = new SessionManager(this).isSafetyShown()
                ? new Intent(this, MainActivity.class)
                : new Intent(this, SafetyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
