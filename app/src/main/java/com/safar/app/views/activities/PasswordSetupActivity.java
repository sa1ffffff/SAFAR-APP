package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.EmailService;
import com.safar.app.utils.PasswordUtils;
import com.safar.app.utils.SessionManager;

/**
 * SAFAR Password Setup Activity
 * Shown after phone OTP verification during registration.
 * Collects user's password and name, then creates the user account.
 */
public class PasswordSetupActivity extends AppCompatActivity {

    public static final String EXTRA_PHONE = "extra_phone";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_AUTH_PROVIDER = "extra_auth_provider";
    public static final String EXTRA_UID = "extra_uid";

    private PasswordSetupStateHolder stateHolder;
    private SessionManager session;
    
    private String phone;
    private String email;
    private String name;
    private String authProvider;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        session = new SessionManager(this);
        stateHolder = new PasswordSetupStateHolder();
        
        // Get data from intent
        phone = getIntent().getStringExtra(EXTRA_PHONE);
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        name = getIntent().getStringExtra(EXTRA_NAME);
        authProvider = getIntent().getStringExtra(EXTRA_AUTH_PROVIDER);
        uid = getIntent().getStringExtra(EXTRA_UID);
        
        // Pre-fill name if available (from Google)
        if (name != null && !name.isEmpty()) {
            stateHolder.setName(name);
        }
        
        // Pre-fill email if available
        if (email != null && !email.isEmpty()) {
            stateHolder.setEmail(email);
        }
        
        // Setup Compose UI
        PasswordSetupBridgeKt.setupComposeUI(this, stateHolder);
    }
    
    /**
     * Called by bridge when user submits the password setup form.
     */
    public void handleSubmit(String userName, String userEmail, String password, String confirmPassword) {
        // Validate inputs
        if (userName == null || userName.trim().isEmpty()) {
            stateHolder.setError("Please enter your name");
            return;
        }
        
        if (userEmail == null || userEmail.trim().isEmpty() || !userEmail.contains("@")) {
            stateHolder.setError("Please enter a valid email address");
            return;
        }
        
        String passwordError = PasswordUtils.validatePasswordStrength(password);
        if (passwordError != null) {
            stateHolder.setError(passwordError);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            stateHolder.setError("Passwords do not match");
            return;
        }
        
        stateHolder.setLoading(true);
        stateHolder.clearError();
        
        // Hash the password
        String passwordHash = PasswordUtils.hashPassword(password);
        if (passwordHash == null) {
            stateHolder.setLoading(false);
            stateHolder.setError("Failed to secure password. Please try again.");
            return;
        }
        
        // Get current Firebase user (should be signed in via OTP or Google)
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String finalUid = uid != null ? uid : (firebaseUser != null ? firebaseUser.getUid() : null);
        
        if (finalUid == null) {
            stateHolder.setLoading(false);
            stateHolder.setError("Session expired. Please start over.");
            return;
        }
        
        // Create user object
        User user = new User(
                finalUid,
                userName.trim(),
                phone != null ? phone : "",
                userEmail.trim(),
                passwordHash,
                authProvider != null ? authProvider : "phone"
        );
        
        // Save to Firebase
        FirebaseManager.getInstance().getUserRef(finalUid).setValue(user)
                .addOnSuccessListener(unused -> {
                    // Save to session
                    session.setUserId(finalUid);
                    session.setUserName(userName.trim());
                    session.setUserPhone(phone != null ? phone : "");
                    session.setUserEmail(userEmail.trim());
                    
                    // Send welcome email
                    sendWelcomeEmail(userName.trim(), userEmail.trim());
                    
                    stateHolder.setLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Failed to create account: " + e.getMessage());
                });
    }
    
    /**
     * Sends welcome email to the new user.
     */
    private void sendWelcomeEmail(String userName, String userEmail) {
        if (!EmailService.isConfigured()) {
            // Email.js not configured yet, skip silently
            return;
        }
        
        EmailService.getInstance().sendWelcomeEmail(userName, userEmail, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                // Welcome email sent successfully
            }
            
            @Override
            public void onFailure(String error) {
                // Log but don't show error to user - welcome email is nice-to-have
            }
        });
    }
    
    /**
     * Navigates to the main app after successful registration.
     */
    private void navigateToMain() {
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        
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
    
    /**
     * Called by bridge when back is pressed.
     */
    public void handleBack() {
        // Sign out since registration is incomplete
        FirebaseAuth.getInstance().signOut();
        finish();
    }
}
