package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.safar.app.utils.SessionManager;

public class SafetyActivity extends AppCompatActivity {

    private SafetyStateHolder stateHolder;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        stateHolder = new SafetyStateHolder();
        SafetyBridgeKt.setupComposeUI(this, stateHolder);
    }

    public void handleIUnderstand() {
        session.setSafetyShown(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}