package com.safar.app.views.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.databinding.ActivityPostAdBinding;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PostAdActivity extends AppCompatActivity implements AdsPresenter.AdsView {

    private ActivityPostAdBinding binding;
    private AdsPresenter presenter;
    private Calendar selectedDateTime = Calendar.getInstance();
    private boolean dateTimeSelected = false;
    private SessionManager session;

    // ── Real coordinates from LocationPickerActivity ─────────────────────────
    private double originLat = 0, originLng = 0;
    private double destLat   = 0, destLng   = 0;
    private boolean originPicked = false;
    private boolean destPicked   = false;

    // ── Location picker launchers ────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> originPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            originLat    = data.getDoubleExtra(LocationPickerActivity.RESULT_LAT, 0);
                            originLng    = data.getDoubleExtra(LocationPickerActivity.RESULT_LNG, 0);
                            String addr  = data.getStringExtra(LocationPickerActivity.RESULT_ADDRESS);
                            originPicked = true;
                            binding.etOrigin.setText(addr);
                        }
                    });

    private final ActivityResultLauncher<Intent> destPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            destLat    = data.getDoubleExtra(LocationPickerActivity.RESULT_LAT, 0);
                            destLng    = data.getDoubleExtra(LocationPickerActivity.RESULT_LNG, 0);
                            String addr = data.getStringExtra(LocationPickerActivity.RESULT_ADDRESS);
                            destPicked  = true;
                            binding.etDestination.setText(addr);
                        }
                    });

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // CRITICAL FIX: Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityPostAdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post a Ride");
        }

        presenter = new AdsPresenter(this);
        session   = new SessionManager(this);

        // Pre-fill phone if available in session
        String savedPhone = session.getUserPhone();
        if (savedPhone != null && !savedPhone.isEmpty()) {
            binding.etPhone.setText(savedPhone);
        }

        // ── Location pickers ──────────────────────────────────────────────
        // Both the display label AND the pin button (end icon) open the picker
        View.OnClickListener openOriginPicker = v -> {
            Intent i = new Intent(this, LocationPickerActivity.class);
            i.putExtra(LocationPickerActivity.EXTRA_TITLE, "Pick Origin");
            originPickerLauncher.launch(i);
        };
        View.OnClickListener openDestPicker = v -> {
            Intent i = new Intent(this, LocationPickerActivity.class);
            i.putExtra(LocationPickerActivity.EXTRA_TITLE, "Pick Destination");
            destPickerLauncher.launch(i);
        };

        binding.etOrigin.setOnClickListener(openOriginPicker);
        binding.tilOrigin.setEndIconOnClickListener(openOriginPicker);
        binding.etDestination.setOnClickListener(openDestPicker);
        binding.tilDestination.setEndIconOnClickListener(openDestPicker);

        binding.btnPickDateTime.setOnClickListener(v -> showDatePicker());
        binding.btnPost.setOnClickListener(v -> validateAndPost());
    }

    // ── Date / Time ──────────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                    showTimePicker();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMinDate(System.currentTimeMillis());
        dp.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
            selectedDateTime.set(Calendar.MINUTE, minute);
            selectedDateTime.set(Calendar.SECOND, 0);
            dateTimeSelected = true;
            updateDateTimeDisplay();
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
    }

    private void updateDateTimeDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault());
        binding.tvSelectedDateTime.setText("✓  " + sdf.format(selectedDateTime.getTime()));
        binding.tvSelectedDateTime.setVisibility(View.VISIBLE);
    }

    // ── Validation & post ────────────────────────────────────────────────────

    private void validateAndPost() {
        String origin      = binding.etOrigin.getText().toString().trim();
        String destination = binding.etDestination.getText().toString().trim();
        String phone       = binding.etPhone.getText().toString().trim();
        String seatsStr    = binding.etSeats.getText().toString().trim();
        String vehicle     = binding.etVehicle.getText().toString().trim();
        String notes       = binding.etNotes.getText().toString().trim();

        if (!originPicked || origin.isEmpty()) {
            Toast.makeText(this, "Please pick an origin location on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!destPicked || destination.isEmpty()) {
            Toast.makeText(this, "Please pick a destination location on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty()) {
            binding.etPhone.setError("Phone number is required");
            binding.etPhone.requestFocus();
            return;
        }
        if (phone.length() < 10) {
            binding.etPhone.setError("Enter a valid phone number");
            binding.etPhone.requestFocus();
            return;
        }
        if (!dateTimeSelected) {
            Toast.makeText(this, "Please select departure date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (seatsStr.isEmpty()) {
            binding.etSeats.setError("Number of seats required");
            binding.etSeats.requestFocus();
            return;
        }
        if (selectedDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future departure time", Toast.LENGTH_SHORT).show();
            return;
        }

        int seats;
        try {
            seats = Integer.parseInt(seatsStr);
            if (seats < 1 || seats > 8) {
                binding.etSeats.setError("Enter 1–8 seats");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etSeats.setError("Invalid number");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        String userName = session.getUserName();
        if (userName == null || userName.isEmpty()) userName = "User";

        // Real coordinates from the map picker — no more hardcoded city lookup
        RideAd ad = new RideAd(
                uid, userName, phone,
                origin, destination,
                originLat, originLng,
                destLat, destLng,
                selectedDateTime.getTimeInMillis(),
                seats, vehicle, notes
        );

        presenter.postAd(ad);
    }

    // ── AdsPresenter callbacks ───────────────────────────────────────────────

    @Override public void onAdsLoaded(List<RideAd> ads) {}

    @Override
    public void onAdPosted(String adId) {
        Toast.makeText(this, "Ride posted successfully! 🚗", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("openTab", "rides");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override public void onAdDeleted() {}

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLoading(boolean isLoading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnPost.setEnabled(!isLoading);
        binding.btnPost.setText(isLoading ? "Posting..." : "Post Ride");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) presenter.detach();
    }
}
