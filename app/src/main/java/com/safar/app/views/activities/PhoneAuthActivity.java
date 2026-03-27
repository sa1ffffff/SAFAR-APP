package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.safar.app.utils.AppConstants;

public class PhoneAuthActivity extends AppCompatActivity {

    private PhoneAuthStateHolder stateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateHolder = new PhoneAuthStateHolder();
        PhoneAuthBridgeKt.setupComposeUI(this, stateHolder);
    }

    public void handleContinue(String phone) {
        if (stateHolder.getPhone().isEmpty()) {
            Toast.makeText(this, "Enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stateHolder.getPhone().length() < 10) {
            Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, OtpVerifyActivity.class);
        intent.putExtra(AppConstants.KEY_PHONE_NUMBER, phone);
        startActivity(intent);
    }
}
