package com.safar.app.views.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PostAdActivity extends AppCompatActivity implements AdsPresenter.AdsView {

    private PostAdStateHolder stateHolder;
    private AdsPresenter      presenter;
    private Calendar          selectedDateTime = Calendar.getInstance();
    private boolean           dateTimeSelected = false;
    private SessionManager    session;

    private double  originLat = 0, originLng = 0;
    private double  destLat   = 0, destLng   = 0;
    private boolean originPicked = false;
    private boolean destPicked   = false;

    private final ActivityResultLauncher<Intent> originPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    originLat    = data.getDoubleExtra(LocationPickerActivity.RESULT_LAT, 0);
                    originLng    = data.getDoubleExtra(LocationPickerActivity.RESULT_LNG, 0);
                    String addr  = data.getStringExtra(LocationPickerActivity.RESULT_ADDRESS);
                    originPicked = true;
                    stateHolder.updateOrigin(addr != null ? addr : "", originLat, originLng);
                }
            });

    private final ActivityResultLauncher<Intent> destPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    destLat    = data.getDoubleExtra(LocationPickerActivity.RESULT_LAT, 0);
                    destLng    = data.getDoubleExtra(LocationPickerActivity.RESULT_LNG, 0);
                    String addr = data.getStringExtra(LocationPickerActivity.RESULT_ADDRESS);
                    destPicked  = true;
                    stateHolder.updateDestination(addr != null ? addr : "", destLat, destLng);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        presenter   = new AdsPresenter(this);
        session     = new SessionManager(this);
        stateHolder = new PostAdStateHolder();

        String savedPhone = session.getUserPhone();
        if (savedPhone != null && !savedPhone.isEmpty()) {
            stateHolder.updatePhone(savedPhone);
        }

        PostAdBridgeKt.setupComposeUI(this, stateHolder);
    }

    public void launchOriginPicker() {
        Intent i = new Intent(this, LocationPickerActivity.class);
        i.putExtra(LocationPickerActivity.EXTRA_TITLE, "Pick Origin");
        originPickerLauncher.launch(i);
    }

    public void launchDestPicker() {
        Intent i = new Intent(this, LocationPickerActivity.class);
        i.putExtra(LocationPickerActivity.EXTRA_TITLE, "Pick Destination");
        destPickerLauncher.launch(i);
    }

    public void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                    showTimePicker();
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
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
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault());
            stateHolder.updateDateTimeLabel(sdf.format(selectedDateTime.getTime()), selectedDateTime.getTimeInMillis());
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
    }

    public void validateAndPost() {
        String origin      = stateHolder.getState().getOriginAddress();
        String destination = stateHolder.getState().getDestAddress();
        String phone       = stateHolder.getState().getPhone().trim();
        String seatsStr    = stateHolder.getState().getSeats().trim();
        String vehicle     = stateHolder.getState().getVehicle().trim();
        String notes       = stateHolder.getState().getNotes().trim();

        if (!originPicked || origin.isEmpty()) {
            Toast.makeText(this, "Please pick an origin location on the map", Toast.LENGTH_SHORT).show(); return;
        }
        if (!destPicked || destination.isEmpty()) {
            Toast.makeText(this, "Please pick a destination location on the map", Toast.LENGTH_SHORT).show(); return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show(); return;
        }
        if (phone.length() < 10) {
            Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show(); return;
        }
        if (!dateTimeSelected) {
            Toast.makeText(this, "Please select departure date and time", Toast.LENGTH_SHORT).show(); return;
        }
        if (seatsStr.isEmpty()) {
            Toast.makeText(this, "Number of seats required", Toast.LENGTH_SHORT).show(); return;
        }
        if (selectedDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future departure time", Toast.LENGTH_SHORT).show(); return;
        }

        int seats;
        try {
            seats = Integer.parseInt(seatsStr);
            if (seats < 1 || seats > 8) {
                Toast.makeText(this, "Enter 1–8 seats", Toast.LENGTH_SHORT).show(); return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid seat number", Toast.LENGTH_SHORT).show(); return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show(); return;
        }

        String userName = session.getUserName();
        if (userName == null || userName.isEmpty()) userName = "User";

        RideAd ad = new RideAd(
                uid, userName, phone,
                origin, destination,
                originLat, originLng, destLat, destLng,
                selectedDateTime.getTimeInMillis(), seats, vehicle, notes
        );
        presenter.postAd(ad);
    }

    @Override public void onAdsLoaded(List<RideAd> ads) {}
    @Override public void onAdDeleted() {}

    @Override
    public void onAdPosted(String adId) {
        Toast.makeText(this, "Ride posted successfully! 🚗", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("openTab", "rides");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLoading(boolean isLoading) {
        stateHolder.updateLoading(isLoading);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) presenter.detach();
    }
}
