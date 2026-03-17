package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.safar.app.databinding.ActivitySafetyBinding;
import com.safar.app.utils.SessionManager;

public class SafetyActivity extends AppCompatActivity {

    private ActivitySafetyBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySafetyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        binding.btnIUnderstand.setOnClickListener(v -> {
            if (!binding.cbAgree.isChecked()) {
                binding.cbAgree.setError("Please acknowledge the safety guidelines");
                return;
            }
            session.setSafetyShown(true);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
