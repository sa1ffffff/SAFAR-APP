package com.safar.app.views.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.databinding.ActivityProfileBinding;
import com.safar.app.firebase.FirebaseManager;
import com.safar.app.models.User;
import com.safar.app.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        session = new SessionManager(this);
        loadProfile();

        binding.btnMyRides.setOnClickListener(v ->
                startActivity(new Intent(this, MyAdsActivity.class)));

        binding.btnLogout.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (d, w) -> {
                        session.logout(this);
                        Intent i = new Intent(this, PhoneAuthActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Fix GitHub button
        binding.btnGithub.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sa1ffffff"));
            startActivity(i);
        });
    }

    private void loadProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        binding.tvName.setText(session.getUserName());
        binding.tvPhone.setText(session.getUserPhone());

        FirebaseManager.getInstance().getUserRef(uid).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        binding.tvName.setText(user.getName());
                        binding.tvPhone.setText(user.getPhone());
                        binding.tvTotalRides.setText(user.getTotalRides() + " rides");
                        if (user.getRatingCount() > 0) {
                            binding.ratingBar.setRating((float) user.getRating());
                            binding.tvRating.setText(String.format("%.1f (%d reviews)", user.getRating(), user.getRatingCount()));
                        }
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
