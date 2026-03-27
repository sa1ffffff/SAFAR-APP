package com.safar.app.views.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_TITLE    = "picker_title";
    public static final String RESULT_LAT     = "result_lat";
    public static final String RESULT_LNG     = "result_lng";
    public static final String RESULT_ADDRESS = "result_address";

    private static final LatLng DEFAULT_POSITION = new LatLng(30.3753, 69.3451);
    private static final float  DEFAULT_ZOOM     = 5f;
    private static final float  PICKED_ZOOM      = 14f;

    private MapView                     mapView;
    private GoogleMap                   googleMap;
    private LocationPickerStateHolder   stateHolder;
    private FusedLocationProviderClient fusedLocation;

    private double pickedLat = DEFAULT_POSITION.latitude;
    private double pickedLng = DEFAULT_POSITION.longitude;

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> locationLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted =
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) jumpToMyLocation();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);
        stateHolder   = new LocationPickerStateHolder();

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        LocationPickerBridgeKt.setupComposeUI(
                this, stateHolder, title != null ? title : "Pick Location");
    }

    // ── Public bridge methods ─────────────────────────────────────────────────

    public void bindMapView(MapView mv) {
        this.mapView = mv;
        mv.getMapAsync(this);
    }

    public void handleZoomIn() {
        if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomIn());
    }

    public void handleZoomOut() {
        if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomOut());
    }

    public void handleMyLocation() { requestLocationAndJump(); }

    public void handleSearch(String query) { searchLocation(query); hideKeyboard(); }

    public void handleSuggestionSelected(String placeId) {
        // TODO: Implement detailed place lookup using Google Places API if needed
        // For now, search by name if the placeId is just a name or handled by searchLocation
        searchLocation(stateHolder.getSearchQuery());
    }

    public void handleRecentLocationSelected(String id) {
        // Find recent location and move camera
        for (RecentLocation loc : stateHolder.getRecentLocations()) {
            if (loc.getId().equals(id)) {
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(loc.getLatitude(), loc.getLongitude()), PICKED_ZOOM));
                }
                break;
            }
        }
    }

    public void handleMapCenterChanged(double lat, double lng) {
        pickedLat = lat;
        pickedLng = lng;
        reverseGeocode(new LatLng(lat, lng));
    }

    public void handleConfirm(String finalName) {
        if (finalName == null || finalName.trim().isEmpty()) {
            Toast.makeText(this, "Please enter or detect a place name", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(RESULT_LAT,     pickedLat);
        result.putExtra(RESULT_LNG,     pickedLng);
        result.putExtra(RESULT_ADDRESS, finalName.trim());
        setResult(RESULT_OK, result);
        finish();
    }

    // ── Map ready ─────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM));
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            handleMapCenterChanged(center.latitude, center.longitude);
        });

        googleMap.setOnCameraMoveStartedListener(reason -> {
            stateHolder.setPlaceNameHint("Detecting…");
            stateHolder.setConfirmEnabled(false);
        });
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void searchLocation(String query) {
        if (query == null || query.isEmpty()) return;
        stateHolder.setPlaceNameHint("Searching…");

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                mainHandler.post(() -> {
                    if (results != null && !results.isEmpty()) {
                        Address addr = results.get(0);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(addr.getLatitude(), addr.getLongitude()), PICKED_ZOOM));
                    } else {
                        stateHolder.setPlaceNameHint("Move map to detect, or type a name");
                        Toast.makeText(this, "Location not found. Try a different name.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                mainHandler.post(() ->
                        stateHolder.setPlaceNameHint("Move map to detect, or type a name"));
            }
        });
    }

    // ── Reverse geocode ───────────────────────────────────────────────────────

    private void reverseGeocode(LatLng pos) {
        stateHolder.setConfirmEnabled(false);

        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(pos.latitude, pos.longitude, 1);
                mainHandler.post(() -> {
                    String detected;
                    if (results != null && !results.isEmpty()) {
                        Address addr       = results.get(0);
                        String locality    = addr.getLocality();
                        String subLocality = addr.getSubLocality();
                        String admin       = addr.getAdminArea();
                        String country     = addr.getCountryName();

                        if (subLocality != null && locality != null)
                            detected = subLocality + ", " + locality;
                        else if (locality != null && admin != null)
                            detected = locality + ", " + admin;
                        else if (admin != null)
                            detected = admin + (country != null ? ", " + country : "");
                        else if (addr.getAddressLine(0) != null)
                            detected = addr.getAddressLine(0);
                        else
                            detected = String.format(Locale.getDefault(),
                                    "%.4f, %.4f", pos.latitude, pos.longitude);
                    } else {
                        detected = String.format(Locale.getDefault(),
                                "%.4f, %.4f", pos.latitude, pos.longitude);
                    }

                    stateHolder.setPlaceName(detected);
                    stateHolder.setPlaceNameHint("Move map to detect, or type a name");
                    stateHolder.setConfirmEnabled(true);
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    stateHolder.setPlaceName(String.format(Locale.getDefault(),
                            "%.4f, %.4f", pos.latitude, pos.longitude));
                    stateHolder.setConfirmEnabled(true);
                });
            }
        });
    }

    // ── Location helpers ──────────────────────────────────────────────────────

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
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), PICKED_ZOOM));
            } else {
                Toast.makeText(this, "Could not determine current location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onResume()  { super.onResume();  if (mapView != null) mapView.onResume(); }
    @Override protected void onPause()   { if (mapView != null) mapView.onPause();  super.onPause(); }
    @Override public    void onLowMemory(){ super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        executor.shutdown();
        super.onDestroy();
    }
}
