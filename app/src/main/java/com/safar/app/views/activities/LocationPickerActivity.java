package com.safar.app.views.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.safar.app.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Intent keys ──────────────────────────────────────────────────────────
    public static final String EXTRA_TITLE    = "picker_title";
    public static final String RESULT_LAT     = "result_lat";
    public static final String RESULT_LNG     = "result_lng";
    public static final String RESULT_ADDRESS = "result_address";

    private static final LatLng DEFAULT_POSITION = new LatLng(30.3753, 69.3451);
    private static final float  DEFAULT_ZOOM     = 5f;
    private static final float  PICKED_ZOOM      = 14f;
    private static final float  ZOOM_STEP        = 1.5f;

    private MapView        mapView;
    private GoogleMap      googleMap;
    private EditText       etSearch;
    private EditText       etPlaceName;   // editable — user can override geocode result
    private MaterialButton btnConfirm;
    private FusedLocationProviderClient fusedLocation;

    private double pickedLat = DEFAULT_POSITION.latitude;
    private double pickedLng = DEFAULT_POSITION.longitude;

    private final ExecutorService executor   = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    // ── Location permission launcher ─────────────────────────────────────────
    private final ActivityResultLauncher<String[]> locationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean granted = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                                Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                        if (granted) jumpToMyLocation();
                    });

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title = getIntent().getStringExtra(EXTRA_TITLE);
            getSupportActionBar().setTitle(title != null ? title : "Pick Location");
        }

        etSearch     = findViewById(R.id.etSearch);
        etPlaceName  = findViewById(R.id.etPlaceName);
        btnConfirm   = findViewById(R.id.btnConfirm);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        // ── Search bar ────────────────────────────────────────────────────
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE) {
                searchLocation(etSearch.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            searchLocation(etSearch.getText().toString().trim());
            hideKeyboard();
        });

        // ── Custom map controls ───────────────────────────────────────────
        CardView btnZoomIn     = findViewById(R.id.btnZoomIn);
        CardView btnZoomOut    = findViewById(R.id.btnZoomOut);
        CardView btnMyLocation = findViewById(R.id.btn_my_location);

        btnZoomIn.setOnClickListener(v -> {
            if (googleMap != null)
                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        });
        btnZoomOut.setOnClickListener(v -> {
            if (googleMap != null)
                googleMap.animateCamera(CameraUpdateFactory.zoomOut());
        });
        btnMyLocation.setOnClickListener(v -> requestLocationAndJump());

        // ── Confirm ───────────────────────────────────────────────────────
        btnConfirm.setOnClickListener(v -> {
            // Use whatever the user has typed in etPlaceName (they may have edited it)
            String finalName = etPlaceName.getText().toString().trim();
            if (finalName.isEmpty()) {
                Toast.makeText(this, "Please enter or detect a place name", Toast.LENGTH_SHORT).show();
                etPlaceName.requestFocus();
                return;
            }
            Intent result = new Intent();
            result.putExtra(RESULT_LAT,     pickedLat);
            result.putExtra(RESULT_LNG,     pickedLng);
            result.putExtra(RESULT_ADDRESS, finalName);
            setResult(RESULT_OK, result);
            finish();
        });

        // ── Map ───────────────────────────────────────────────────────────
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // ── Map ready ────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM));

        // Disable ALL default SDK controls — we have our own
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        // When camera stops moving → update coordinates + reverse geocode
        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            pickedLat = center.latitude;
            pickedLng = center.longitude;
            reverseGeocode(center);
        });

        // On camera move start → clear place name so user sees it's updating
        googleMap.setOnCameraMoveStartedListener(reason -> {
            etPlaceName.setHint("Detecting…");
            btnConfirm.setEnabled(false);
        });
    }

    // ── Search (geocode name → move camera) ─────────────────────────────────

    private void searchLocation(String query) {
        if (query.isEmpty()) return;
        etPlaceName.setHint("Searching…");

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                mainHandler.post(() -> {
                    if (results != null && !results.isEmpty()) {
                        Address addr = results.get(0);
                        LatLng pos = new LatLng(addr.getLatitude(), addr.getLongitude());
                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(pos, PICKED_ZOOM));
                        // onCameraIdle fires after animation → fills etPlaceName
                    } else {
                        etPlaceName.setHint("Move map to detect, or type a name");
                        Toast.makeText(this, "Location not found. Try a different name.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                mainHandler.post(() ->
                        etPlaceName.setHint("Move map to detect, or type a name"));
            }
        });
    }

    // ── Reverse geocode (coordinates → readable place name) ─────────────────
    // Fills etPlaceName so user sees the detected name.
    // User can then edit it to something more specific like "SEECS Parking, NUST".

    private void reverseGeocode(LatLng pos) {
        btnConfirm.setEnabled(false);

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(pos.latitude, pos.longitude, 1);
                mainHandler.post(() -> {
                    String detected;
                    if (results != null && !results.isEmpty()) {
                        Address addr = results.get(0);
                        String locality = addr.getLocality();
                        String subLocality = addr.getSubLocality();
                        String admin = addr.getAdminArea();
                        String country = addr.getCountryName();

                        // Try to give the most specific name available
                        if (subLocality != null && locality != null) {
                            detected = subLocality + ", " + locality;
                        } else if (locality != null && admin != null) {
                            detected = locality + ", " + admin;
                        } else if (admin != null) {
                            detected = admin + (country != null ? ", " + country : "");
                        } else if (addr.getAddressLine(0) != null) {
                            detected = addr.getAddressLine(0);
                        } else {
                            detected = String.format(Locale.getDefault(),
                                    "%.4f, %.4f", pos.latitude, pos.longitude);
                        }
                    } else {
                        detected = String.format(Locale.getDefault(),
                                "%.4f, %.4f", pos.latitude, pos.longitude);
                    }

                    // Pre-fill the editable field — user can now override it
                    etPlaceName.setText(detected);
                    etPlaceName.setHint("Move map to detect, or type a name");

                    // Move cursor to end so user can edit easily
                    etPlaceName.setSelection(detected.length());

                    btnConfirm.setEnabled(true);
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    String coords = String.format(Locale.getDefault(),
                            "%.4f, %.4f", pos.latitude, pos.longitude);
                    etPlaceName.setText(coords);
                    btnConfirm.setEnabled(true);
                });
            }
        });
    }

    // ── Location Helpers ─────────────────────────────────────────────────────

    private void requestLocationAndJump() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            jumpToMyLocation();
        } else {
            locationLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void jumpToMyLocation() {
        fusedLocation.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && googleMap != null) {
                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, PICKED_ZOOM));
            } else {
                Toast.makeText(this, "Could not determine current location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
