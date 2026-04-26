package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;

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
import com.safar.app.utils.PasswordUtils;
import com.safar.app.utils.SessionManager;

// Kotlin bridge imports
import static com.safar.app.views.activities.LoginBridgeKt.setupComposeUI;

/**
 * SAFAR LOGIN ACTIVITY
 * Redesigned authentication with Sign In / Sign Up flow
 * 
 * Sign In: Phone/Email + Password (no OTP)
 * Sign Up: Phone → OTP → Password Setup
 *          Google → Direct sign in/sign up
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
        setupComposeUI(this, stateHolder);
    }

    /**
     * Handles Sign In with phone/email and password
     * No OTP required - direct password authentication
     */
    public void handleSignIn(String phone, String email, String password) {
        stateHolder.setLoading(true);
        stateHolder.clearError();

        // Determine if signing in with phone or email
        String identifier = phone.isEmpty() ? email : phone;
        
        // First, find user in database by phone or email
        if (!phone.isEmpty()) {
            // Sign in with phone number + password
            String fullPhone = stateHolder.getFullPhone();
            signInWithPhonePassword(fullPhone, password);
        } else {
            // Sign in with email + password using Firebase Auth
            signInWithEmailPassword(email, password);
        }
    }

    /**
     * Sign in with phone number and password
     * Looks up user in Firebase Database and verifies password
     */
    private void signInWithPhonePassword(String phoneNumber, String password) {
        // Query Firebase for user with this phone number
        FirebaseManager.getInstance().getUsersRef()
                .orderByChild("phone")
                .equalTo(phoneNumber)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Account not found. Please sign up first.");
                        return;
                    }

                    // Get the first matching user
                    User user = null;
                    String userId = null;
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        user = child.getValue(User.class);
                        userId = child.getKey();
                        break;
                    }

                    if (user == null || userId == null) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("User data corrupted");
                        return;
                    }

                    // Check if user has a password set
                    String passwordHash = user.getPasswordHash();
                    if (passwordHash == null || passwordHash.isEmpty()) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("No password set. Please sign up again.");
                        return;
                    }

                    // Verify password
                    if (!PasswordUtils.verifyPassword(password, passwordHash)) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Incorrect password");
                        return;
                    }

                    // Password correct - login successful
                    session.setUserId(userId);
                    session.setUserName(user.getName());
                    session.setUserPhone(user.getPhone());
                    if (user.getEmail() != null) {
                        session.setUserEmail(user.getEmail());
                    }

                    stateHolder.setLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Sign in failed: " + e.getMessage());
                });
    }

    /**
     * Sign in with email and password using Firebase Auth
     */
    private void signInWithEmailPassword(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        stateHolder.setLoading(false);
                        stateHolder.setError("Sign in failed");
                        return;
                    }

                    // Load user data from DB
                    FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).get()
                            .addOnSuccessListener(snapshot -> {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    session.setUserId(firebaseUser.getUid());
                                    session.setUserName(user.getName());
                                    session.setUserPhone(user.getPhone());
                                    if (user.getEmail() != null) {
                                        session.setUserEmail(user.getEmail());
                                    }
                                }
                                stateHolder.setLoading(false);
                                navigateToMain();
                            });
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    String message = e.getMessage();
                    if (message != null && message.contains("no user record")) {
                        stateHolder.setError("Account not found. Please sign up first.");
                    } else if (message != null && message.contains("password is invalid")) {
                        stateHolder.setError("Incorrect password");
                    } else {
                        stateHolder.setError("Sign in failed: " + message);
                    }
                });
    }

    /**
     * Handles phone sign up - navigates to OTP verification
     * After OTP verification, user will set password
     */
    public void handlePhoneAuth(String phoneNumber) {
        stateHolder.setLoading(true);
        stateHolder.clearError();
        
        Intent intent = new Intent(this, OtpVerifyActivity.class);
        intent.putExtra(AppConstants.KEY_PHONE_NUMBER, phoneNumber);
        intent.putExtra("IS_SIGN_UP", true); // Flag to indicate sign up flow
        startActivity(intent);
        stateHolder.setLoading(false);
    }

    /**
     * Handles Google Sign-In/Sign-Up
     * For both new and existing users: signs in directly
     */
    public void handleGoogleSignIn() {
        stateHolder.setLoading(true);
        stateHolder.clearError();
        // Sign out first to force account picker to show every time
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            googleLauncher.launch(googleSignInClient.getSignInIntent());
        });
    }

    /**
     * Navigate to Forgot Password screen
     */
    public void navigateToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
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

                    FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).get()
                            .addOnSuccessListener(snapshot -> {
                                User user = snapshot.getValue(User.class);
                                if (snapshot.exists() && user != null) {
                                    signInWithGoogleUser(firebaseUser, user);
                                    return;
                                }

                                User newGoogleUser = buildGoogleUser(firebaseUser);
                                FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).setValue(newGoogleUser)
                                        .addOnSuccessListener(unused -> signInWithGoogleUser(firebaseUser, newGoogleUser))
                                        .addOnFailureListener(e -> {
                                            stateHolder.setLoading(false);
                                            stateHolder.setError("Failed to create account: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                stateHolder.setLoading(false);
                                stateHolder.setError("Failed to load user data: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    stateHolder.setLoading(false);
                    stateHolder.setError("Google authentication failed: " + e.getMessage());
                });
    }

    private void signInWithGoogleUser(FirebaseUser firebaseUser, User user) {
        String resolvedName = firstNonBlank(user.getName(), firebaseUser.getDisplayName(), "User");
        String resolvedPhone = firstNonBlank(user.getPhone(), "");
        String resolvedEmail = firstNonBlank(user.getEmail(), firebaseUser.getEmail(), "");

        String photoUrl = user.getPhotoUrl();
        if ((photoUrl == null || photoUrl.trim().isEmpty()) && firebaseUser.getPhotoUrl() != null) {
            photoUrl = firebaseUser.getPhotoUrl().toString();
        }

        session.setUserId(firebaseUser.getUid());
        session.setUserName(resolvedName);
        session.setUserPhone(resolvedPhone);
        session.setUserEmail(resolvedEmail);
        session.setUserPhotoUrl(photoUrl);

        stateHolder.setLoading(false);
        navigateToMain();
    }

    private User buildGoogleUser(FirebaseUser firebaseUser) {
        User user = new User(
                firebaseUser.getUid(),
                firstNonBlank(firebaseUser.getDisplayName(), "User"),
                "",
                firstNonBlank(firebaseUser.getEmail(), ""),
                null,
                "google"
        );

        if (firebaseUser.getPhotoUrl() != null) {
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
        }
        return user;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
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
}
