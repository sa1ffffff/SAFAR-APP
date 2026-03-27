package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.safar.app.R;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;

/**
 * SAFAR LOGIN ACTIVITY
 * Modern Compose-based login with Instagram-style animations
 * Supports phone, email, and Google authentication
 */
public class LoginActivity extends AppCompatActivity {

    private LoginStateHolder stateHolder;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private SessionManager session;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Google Sign-In failed: " + e.getMessage());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize dependencies
        auth = FirebaseManager.getInstance().getAuth();
        session = new SessionManager(this);
        stateHolder = new LoginStateHolder();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup Compose UI
        LoginBridgeKt.setupComposeUI(this, stateHolder);
    }

    /**
     * Handles phone authentication flow
     */
    public void handlePhoneAuth(String phoneNumber) {
        stateHolder.setLoading(true);
        stateHolder.clearError();
        
        Intent intent = new Intent(this, OtpVerifyActivity.class);
        intent.putExtra(AppConstants.KEY_PHONE_NUMBER, phoneNumber);
        startActivity(intent);
        stateHolder.setLoading(false);
    }

    /**
     * Handles email registration
     */
    public void handleEmailRegister(String email, String password, String name) {
        stateHolder.setLoading(true);
        stateHolder.clearError();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Registration failed");
                        return;
                    }

                    // Send verification email
                    firebaseUser.sendEmailVerification()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Verification email sent. Please verify then login.",
                                            Toast.LENGTH_LONG).show());

                    // Save to DB
                    User user = new User(firebaseUser.getUid(), name, email);
                    FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).setValue(user);
                    session.setUserName(name);
                    session.setUserPhone(email); // Using phone field for email

                    stateHolder.setLoading(false);
                    // Sign out - force them to verify email first
                    auth.signOut();
                    showEmailVerificationDialog(email);
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Registration failed: " + e.getMessage());
                });
    }

    /**
     * Handles email login
     */
    public void handleEmailLogin(String email, String password) {
        stateHolder.setLoading(true);
        stateHolder.clearError();

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Login failed");
                        return;
                    }

                    // Check if email is verified
                    if (!firebaseUser.isEmailVerified()) {
                        stateHolder.setLoading(false);
                        auth.signOut();
                        showResendVerificationDialog(email, password);
                        return;
                    }

                    // Load user data from DB
                    FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).get()
                            .addOnSuccessListener(snapshot -> {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    session.setUserName(user.getName());
                                    session.setUserPhone(user.getPhone());
                                }
                                stateHolder.setLoading(false);
                                navigateToMain();
                            });
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Login failed: " + e.getMessage());
                });
    }

    /**
     * Handles Google authentication
     */
    public void handleGoogleSignIn() {
        stateHolder.setLoading(true);
        stateHolder.clearError();
        googleLauncher.launch(googleSignInClient.getSignInIntent());
    }

    /**
     * Handles forgot password
     */
    public void handleForgotPassword(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                    stateHolder.clearError();
                })
                .addOnFailureListener(e ->
                        stateHolder.setError("Error: " + e.getMessage()));
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Google authentication failed");
                        return;
                    }

                    // Check if new user
                    boolean isNew = result.getAdditionalUserInfo() != null
                            && result.getAdditionalUserInfo().isNewUser();

                    if (isNew) {
                        // Save new user to DB
                        User user = new User(
                                firebaseUser.getUid(),
                                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User",
                                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : ""
                        );
                        FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).setValue(user);
                        session.setUserName(user.getName());
                        session.setUserPhone(user.getPhone());
                    } else {
                        // Existing user - load from DB
                        FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).get()
                                .addOnSuccessListener(snapshot -> {
                                    User user = snapshot.getValue(User.class);
                                    if (user != null) {
                                        session.setUserName(user.getName());
                                        session.setUserPhone(user.getPhone());
                                    }
                                });
                    }
                    stateHolder.setLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Google authentication failed: " + e.getMessage());
                });
    }

    private void navigateToMain() {
        SessionManager s = new SessionManager(this);
        Intent intent;
        if (!s.isSafetyShown()) {
            intent = new Intent(this, SafetyActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showEmailVerificationDialog(String email) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Verify Your Email")
                .setMessage("A verification link has been sent to " + email + ".\n\nPlease check your inbox and click the link, then come back to login.")
                .setPositiveButton("OK, I'll verify", null)
                .setNegativeButton("Resend Email", (d, w) -> {
                    // Note: Cannot resend without re-authentication
                    Toast.makeText(this, "Please try registering again to resend", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showResendVerificationDialog(String email, String password) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Please verify your email before logging in.\n\nCheck your inbox for the verification link.")
                .setPositiveButton("Resend Email", (d, w) -> {
                    // Sign in temporarily to resend
                    auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(r -> {
                                if (r.getUser() != null) {
                                    r.getUser().sendEmailVerification()
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(this, "Verification email resent!", Toast.LENGTH_SHORT).show());
                                    auth.signOut();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}