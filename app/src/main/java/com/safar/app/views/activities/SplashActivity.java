package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.safar.app.R;
import com.safar.app.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashBridgeKt.setupComposeUI(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);

            if (!session.isLoggedIn()) {
                // Go to unified Login screen (Phone + Email + Google)
                startActivity(new Intent(this, LoginActivity.class));
            } else if (!session.isSafetyShown()) {
                startActivity(new Intent(this, SafetyActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 3000); // Netflix-style timing for letter animation
    }
}
