package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.safar.app.databinding.ActivityOtpVerifyBinding;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;

import java.util.concurrent.TimeUnit;

public class OtpVerifyActivity extends AppCompatActivity {

    private ActivityOtpVerifyBinding binding;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth auth;
    private String phoneNumber;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseManager.getInstance().getAuth();
        session = new SessionManager(this);
        phoneNumber = getIntent().getStringExtra(AppConstants.KEY_PHONE_NUMBER);

        binding.tvPhoneHint.setText("Code sent to " + phoneNumber);
        sendOtp(phoneNumber, false);

        binding.btnVerify.setOnClickListener(v -> {
            String code = binding.etOtp.getText().toString().trim();
            if (code.length() != 6) {
                binding.etOtp.setError("Enter the 6-digit code");
                return;
            }
            verifyCode(code);
        });

        binding.tvResend.setOnClickListener(v -> {
            sendOtp(phoneNumber, true);
        });
    }

    private void sendOtp(String phone, boolean resend) {
        setLoading(true);
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
                        setLoading(false);
                        Toast.makeText(OtpVerifyActivity.this,
                                "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        resendToken = token;
                        setLoading(false);
                        startResendTimer();
                    }
                });

        if (resend && resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private void verifyCode(String code) {
        if (verificationId == null) {
            Toast.makeText(this, "Please wait for OTP to arrive.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        checkOrCreateUser(user);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Invalid OTP. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkOrCreateUser(FirebaseUser firebaseUser) {
        FirebaseManager.getInstance().getUserRef(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // New user - show name dialog
                        setLoading(false);
                        showNameDialog(firebaseUser);
                    } else {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            session.setUserName(user.getName());
                            session.setUserPhone(user.getPhone());
                        }
                        navigateNext();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showNameDialog(FirebaseUser firebaseUser) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Welcome to SAFAR!");
        builder.setMessage("What's your name?");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Your full name");
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);

        builder.setPositiveButton("Continue", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Name required");
                return;
            }
            User user = new User(firebaseUser.getUid(), name, phoneNumber);
            FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).setValue(user)
                    .addOnSuccessListener(unused -> {
                        session.setUserName(name);
                        session.setUserPhone(phoneNumber);
                        navigateNext();
                    });
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void navigateNext() {
        SessionManager session = new SessionManager(this);
        Intent intent;
        if (!session.isSafetyShown()) {
            intent = new Intent(this, SafetyActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startResendTimer() {
        binding.tvResend.setEnabled(false);
        new CountDownTimer(60000, 1000) {
            public void onTick(long ms) {
                binding.tvResend.setText("Resend in " + (ms / 1000) + "s");
            }
            public void onFinish() {
                binding.tvResend.setText("Resend OTP");
                binding.tvResend.setEnabled(true);
            }
        }.start();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnVerify.setEnabled(!loading);
    }
}
