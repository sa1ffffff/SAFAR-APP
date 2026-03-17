package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.safar.app.databinding.ActivityPhoneAuthBinding;
import com.safar.app.utils.AppConstants;

public class PhoneAuthActivity extends AppCompatActivity {

    private ActivityPhoneAuthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnContinue.setOnClickListener(v -> {
            String phone = binding.ccp.getSelectedCountryCodeWithPlus()
                    + binding.etPhone.getText().toString().trim();

            if (binding.etPhone.getText().toString().trim().isEmpty()) {
                binding.etPhone.setError("Enter your phone number");
                return;
            }
            if (binding.etPhone.getText().toString().trim().length() < 10) {
                binding.etPhone.setError("Enter a valid phone number");
                return;
            }

            Intent intent = new Intent(this, OtpVerifyActivity.class);
            intent.putExtra(AppConstants.KEY_PHONE_NUMBER, phone);
            startActivity(intent);
        });
    }
}
