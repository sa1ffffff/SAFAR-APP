package com.safar.app.views.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.AppConstants;
import com.safar.app.utils.ProfileImageHelper;
import com.safar.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ProfileStateHolder stateHolder;
    private SessionManager session;
    
    // Image picker launcher
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session     = new SessionManager(this);
        stateHolder = new ProfileStateHolder();
        
        // Register image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleImageResult
        );
        
        ProfileBridgeKt.setupComposeUI(this, stateHolder);
        loadProfile();
    }

    private void loadProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        stateHolder.updateName(session.getUserName()  != null ? session.getUserName()  : "");
        stateHolder.updatePhone(session.getUserPhone() != null ? session.getUserPhone() : "");
        stateHolder.updateAvatar(session.getUserPhotoUrl());

        FirebaseManager.getInstance().getUserRef(uid).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        stateHolder.updateName(user.getName()   != null ? user.getName()  : "");
                        stateHolder.updatePhone(user.getPhone() != null ? user.getPhone() : "");
                        stateHolder.updateEmail(user.getEmail() != null ? user.getEmail() : "");
                        stateHolder.updateBio(user.getBio() != null ? user.getBio() : "");
                        stateHolder.updateAvatar(user.getPhotoUrl());
                        stateHolder.updateTotalRides(user.getTotalRides());
                        if (user.getRatingCount() > 0) {
                            stateHolder.updateRating((float) user.getRating(), user.getRatingCount());
                        }
                    }
                });
    }

    public void handleSaveProfile(String name, String bio) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        stateHolder.updateLoading(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("bio", bio);

        FirebaseManager.getInstance().getUserRef(uid).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    stateHolder.updateName(name);
                    stateHolder.updateBio(bio);
                    session.setUserName(name);
                    stateHolder.updateLoading(false);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    stateHolder.updateLoading(false);
                    stateHolder.setError(e.getMessage());
                });
    }

    public void handleChangePhoto() {
        imagePickerLauncher.launch("image/*");
    }
    
    public void handleAvatarClick() {
        // Could open full screen avatar view or photo options
        handleChangePhoto();
    }
    
    private void handleImageResult(Uri uri) {
        if (uri == null) return;
        
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        
        stateHolder.updateUploading(true);
        
        // Ensure only 3 arguments are passed: Context, Uri, Callback
        ProfileImageHelper.uploadProfilePicture(
                ProfileActivity.this, 
                uri, 
                new ProfileImageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        runOnUiThread(() -> {
                            stateHolder.updateAvatar(downloadUrl);
                            stateHolder.updateUploading(false);
                            session.setUserPhotoUrl(downloadUrl);
                            Toast.makeText(ProfileActivity.this, 
                                    "Profile picture updated", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            stateHolder.updateUploading(false);
                            Toast.makeText(ProfileActivity.this, 
                                    error, Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                        // Optional: Update progress UI if needed
                    }
                }
        );
    }

    public void navigateToMyRides() {
        startActivity(new Intent(this, MyAdsActivity.class));
    }

    public void navigateToPaymentMethods() {
        Toast.makeText(this, "Payments coming soon", Toast.LENGTH_SHORT).show();
    }

    public void navigateToSavedPlaces() {
        Toast.makeText(this, "Saved places coming soon", Toast.LENGTH_SHORT).show();
    }

    public void navigateToSafety() {
        startActivity(new Intent(this, SafetyActivity.class));
    }

    public void navigateToHelp() {
        startActivity(new Intent(this, HelpActivity.class));
    }

    public void navigateToSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void navigateToAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void handleLogout() {
        session.logout(this);
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
