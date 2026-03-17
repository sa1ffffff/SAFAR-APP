package com.safar.app.views.activities;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.safar.app.databinding.ActivityLoginBinding;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
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
                    setLoading(false);
                    Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseManager.getInstance().getAuth();
        session = new SessionManager(this);

        // Configure Google Sign-In
        // R.string.default_web_client_id is auto-generated from google-services.json
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Tab switching
        binding.tabPhone.setOnClickListener(v -> {
            if (binding.layoutPhone.getVisibility() != View.VISIBLE)
                showTab("phone");
        });

        binding.tabEmail.setOnClickListener(v -> {
            if (binding.layoutEmail.getVisibility() != View.VISIBLE)
                showTab("email");
        });

        // Phone Auth
        binding.btnContinuePhone.setOnClickListener(v -> {
            String phone = binding.ccp.getSelectedCountryCodeWithPlus()
                    + binding.etPhone.getText().toString().trim();
            if (binding.etPhone.getText().toString().trim().isEmpty()) {
                binding.etPhone.setError("Enter phone number");
                return;
            }
            Intent intent = new Intent(this, OtpVerifyActivity.class);
            intent.putExtra(AppConstants.KEY_PHONE_NUMBER, phone);
            startActivity(intent);
        });

        // Toggle Email mode Login/Register
        binding.btnToggleEmailMode.setOnClickListener(v -> {
            boolean isLogin = binding.layoutRegisterFields.getVisibility() == View.GONE;
            if (isLogin) {
                binding.layoutRegisterFields.setVisibility(View.VISIBLE);
                binding.btnEmailAction.setText("Create Account");
                binding.btnToggleEmailMode.setText("Already have an account? Login");
            } else {
                binding.layoutRegisterFields.setVisibility(View.GONE);
                binding.btnEmailAction.setText("Login");
                binding.btnToggleEmailMode.setText("Don't have an account? Register");
            }
        });

        // Email Login / Register
        binding.btnEmailAction.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            if (email.isEmpty()) { binding.etEmail.setError("Required"); return; }
            if (password.isEmpty()) { binding.etPassword.setError("Required"); return; }
            if (password.length() < 6) { binding.etPassword.setError("Minimum 6 characters"); return; }

            boolean isRegister = binding.layoutRegisterFields.getVisibility() == View.VISIBLE;
            if (isRegister) {
                String name = binding.etName.getText().toString().trim();
                if (name.isEmpty()) { binding.etName.setError("Required"); return; }
                registerWithEmail(email, password, name);
            } else {
                loginWithEmail(email, password);
            }
        });

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty()) { binding.etEmail.setError("Enter your email first"); return; }
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Google Sign-In
        binding.btnGoogle.setOnClickListener(v -> {
            setLoading(true);
            googleLauncher.launch(googleSignInClient.getSignInIntent());
        });
    }

    // ─── EMAIL REGISTER ───
    private void registerWithEmail(String email, String password, String name) {
        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    // Send verification email
                    firebaseUser.sendEmailVerification()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Verification email sent to " + email + ". Please verify then login.",
                                            Toast.LENGTH_LONG).show());

                    // Save to DB
                    User user = new User(firebaseUser.getUid(), name, email);
                    FirebaseManager.getInstance().getUserRef(firebaseUser.getUid()).setValue(user);
                    session.setUserName(name);
                    session.setUserPhone(email);

                    setLoading(false);
                    // Sign out - force them to verify email first
                    auth.signOut();
                    showEmailVerificationDialog(email);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ─── EMAIL LOGIN ───
    private void loginWithEmail(String email, String password) {
        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    // Check if email is verified
                    if (!firebaseUser.isEmailVerified()) {
                        setLoading(false);
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
                                setLoading(false);
                                navigateToMain();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ─── GOOGLE AUTH ───
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

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
                    setLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Google Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ─── DIALOGS ───
    private void showEmailVerificationDialog(String email) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Verify Your Email")
                .setMessage("A verification link has been sent to " + email + ".\n\nPlease check your inbox and click the link, then come back to login.")
                .setPositiveButton("OK, I'll verify", null)
                .setNegativeButton("Resend Email", (d, w) -> {
                    auth.signInWithEmailAndPassword(email, "")
                            .addOnFailureListener(e -> {
                                // Re-send via current user if available
                                FirebaseUser u = auth.getCurrentUser();
                                if (u != null) u.sendEmailVerification();
                            });
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

    // ─── HELPERS ───
    private void showTab(String tab) {

        boolean phoneSelected = tab.equals("phone");

        View show = phoneSelected ? binding.layoutPhone : binding.layoutEmail;
        View hide = phoneSelected ? binding.layoutEmail : binding.layoutPhone;

        View activeTab = phoneSelected ? binding.tabPhone : binding.tabEmail;
        View inactiveTab = phoneSelected ? binding.tabEmail : binding.tabPhone;

        // Fade + slide out current
        hide.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> hide.setVisibility(View.GONE))
                .start();

        // Prepare new view
        show.setVisibility(View.VISIBLE);
        show.setAlpha(0f);
        show.setTranslationY(30f);

        // Fade + slide in
        show.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animate tab highlight
        activeTab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160)
                .start();

        inactiveTab.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(160)
                .start();

        // Change tab backgrounds
        activeTab.setBackgroundResource(R.drawable.bg_tab_active);
        inactiveTab.setBackgroundResource(android.R.color.transparent);

        // Update text colors
        if (phoneSelected) {
            binding.tabPhone.setTextColor(getColor(R.color.safar_black));
            binding.tabEmail.setTextColor(getColor(R.color.safar_text_secondary));
        } else {
            binding.tabEmail.setTextColor(getColor(R.color.safar_black));
            binding.tabPhone.setTextColor(getColor(R.color.safar_text_secondary));
        }
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

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGoogle.setEnabled(!loading);
        binding.btnEmailAction.setEnabled(!loading);
        binding.btnContinuePhone.setEnabled(!loading);
    }
}
