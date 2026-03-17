package com.safar.app.views.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.safar.app.R;
import com.safar.app.databinding.FragmentMapBinding;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.views.activities.AdDetailActivity;
import com.safar.app.views.activities.MyAdsActivity;
import com.safar.app.views.activities.PostAdActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback, AdsPresenter.AdsView {

    private FragmentMapBinding binding;
    private MapView mapView;
    private GoogleMap googleMap;
    private AdsPresenter presenter;

    private final Map<String, RideAd> markerAdMap = new HashMap<>();
    private final List<RideAd> recentAds = new ArrayList<>();
    private List<RideAd> pendingAds = null;
    private boolean mapReady = false;

    private final ActivityResultLauncher<String[]> locationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                        boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                        if ((fine || coarse) && googleMap != null) {
                            enableMyLocation();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter = new AdsPresenter(this);

        binding.tilePostRide.setOnClickListener(v -> startActivity(new Intent(requireContext(), PostAdActivity.class)));
        binding.tileExplore.setOnClickListener(v -> openListTab());
        binding.tileMyRides.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyAdsActivity.class)));
        binding.cardSearch.setOnClickListener(v -> openListTab());
        binding.etSearch.setOnClickListener(v -> openListTab());
        binding.btnSeeAllRides.setOnClickListener(v -> openListTab());
        
        binding.rideRow1.setOnClickListener(v -> openFirstAd());
        binding.rideRow2.setOnClickListener(v -> openSecondAd());
        binding.btnOpen1.setOnClickListener(v -> openFirstAd());
        binding.btnOpen2.setOnClickListener(v -> openSecondAd());

        presenter.loadActiveAds();
    }

    private void openListTab() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new ListFragment())
                .commit();
        try {
            com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            if (nav != null) nav.setSelectedItemId(R.id.nav_list);
        } catch (Exception ignored) {}
    }

    private void openFirstAd() {
        if (!recentAds.isEmpty()) openAdDetail(recentAds.get(0).getAdId());
        else openListTab();
    }

    private void openSecondAd() {
        if (recentAds.size() > 1) openAdDetail(recentAds.get(1).getAdId());
        else if (!recentAds.isEmpty()) openAdDetail(recentAds.get(0).getAdId());
        else openListTab();
    }

    private void openAdDetail(String adId) {
        if (adId == null || !isAdded()) return;
        Intent i = new Intent(requireContext(), AdDetailActivity.class);
        i.putExtra(AppConstants.KEY_AD_ID, adId);
        startActivity(i);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        mapReady = true;
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception ignored) {}

        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(AppConstants.DEFAULT_LAT, AppConstants.DEFAULT_LNG), 13f));

        applyMapPadding();

        googleMap.setOnMarkerClickListener(marker -> {
            String adId = marker.getTag() != null ? marker.getTag().toString() : null;
            if (adId != null && markerAdMap.containsKey(adId)) {
                AdBottomSheetFragment sheet = AdBottomSheetFragment.newInstance(adId);
                sheet.show(getChildFragmentManager(), "BottomSheet");
            }
            return true;
        });

        requestLocationPermission();
        if (pendingAds != null) {
            renderMarkers(pendingAds);
            pendingAds = null;
        }
    }

    private void applyMapPadding() {
        if (binding == null || googleMap == null || !isAdded()) return;
        binding.getRoot().post(() -> {
            if (binding == null || googleMap == null || !isAdded()) return;
            int topPx = binding.topBar.getHeight();
            int bottomPx = binding.bottomSheet.getHeight();
            googleMap.setPadding(0, topPx, 0, bottomPx);
        });
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, requireContext().getResources().getDisplayMetrics());
    }

    private void requestLocationPermission() {
        if (!isAdded()) return;
        boolean hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (hasFine) enableMyLocation();
        else locationLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocation() {
        if (googleMap == null || !isAdded()) return;
        googleMap.setMyLocationEnabled(true);
        binding.btnMyLocation.setOnClickListener(v -> {
             if(googleMap.getMyLocation() != null) {
                 googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                         new LatLng(googleMap.getMyLocation().getLatitude(), googleMap.getMyLocation().getLongitude()), 15f));
             }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RIDE MARKER
    // Layout: [car icon 48dp] [6dp gap] [dark pill with origin label]
    // ─────────────────────────────────────────────────────────────────────────
    private BitmapDescriptor makeRideMarker(String labelText) {
        float d = requireContext().getResources().getDisplayMetrics().density;

        // ── STEP 1: LOAD & SCALE car_marker.png (Used for Ride Ads) ──
        int iconSize = (int) (48 * d);
        Bitmap rawIcon = BitmapFactory.decodeResource(getResources(), R.drawable.car_marker);
        if (rawIcon == null) {
            // Fallback if car_marker is missing
            return BitmapDescriptorFactory.defaultMarker();
        }
        Bitmap carIcon = Bitmap.createScaledBitmap(rawIcon, iconSize, iconSize, true);
        rawIcon.recycle();

        // ── STEP 2: MEASURE the pill label ───────────────────────────────────
        int labelH = (int) (26 * d);
        int labelPad = (int) (12 * d);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(11 * d);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        float textW = textPaint.measureText(labelText);
        int labelW = (int) (textW + labelPad * 2);

        // ── STEP 3: TOTAL BITMAP SIZE ─────────────────────────────────────────
        int gap = (int) (6 * d);
        int totalW = iconSize + gap + labelW;
        int totalH = iconSize;

        Bitmap result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // ── STEP 4: DRAW icon (left side) ────────────────────────────────────
        canvas.drawBitmap(carIcon, 0f, 0f, null);
        carIcon.recycle();

        // ── STEP 5: DRAW pill background ─────────────────────────────────────
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#E61C1B1F")); // M3 Dark Surface with alpha
        bgPaint.setStyle(Paint.Style.FILL);

        float pillTop = (totalH - labelH) / 2f;
        RectF pill = new RectF(iconSize + gap, pillTop, totalW, pillTop + labelH);
        canvas.drawRoundRect(pill, labelH / 2f, labelH / 2f, bgPaint);

        // ── STEP 6: DRAW label text ──────────────────────────────────────────
        textPaint.setColor(Color.WHITE);
        float textX = iconSize + gap + labelPad;
        float textY = pillTop + (labelH / 2f) + (textPaint.getTextSize() / 3f);
        canvas.drawText(labelText, textX, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(result);
    }

    private void renderMarkers(List<RideAd> ads) {
        if (googleMap == null || !isAdded()) return;
        googleMap.clear();
        markerAdMap.clear();
        for (RideAd ad : ads) {
            if (ad.getOriginLat() == 0.0) continue;
            LatLng pos = new LatLng(ad.getOriginLat(), ad.getOriginLng());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .icon(makeRideMarker(ad.getOrigin()))
                    .anchor(0.5f, 0.5f));
            if (marker != null) {
                marker.setTag(ad.getAdId());
                markerAdMap.put(ad.getAdId(), ad);
            }
        }
    }

    @Override
    public void onAdsLoaded(List<RideAd> ads) {
        if (!isAdded()) return;
        recentAds.clear();
        recentAds.addAll(ads);
        if (mapReady) renderMarkers(ads);
        else pendingAds = new ArrayList<>(ads);

        if (binding == null) return;
        if (!ads.isEmpty()) {
            RideAd r = ads.get(0);
            binding.tvRide1Route.setText(r.getOrigin() + " → " + r.getDestination());
            binding.tvRide1Meta.setText(r.getAvailableSeats() + " seat(s) | " + com.safar.app.utils.DateUtils.formatShort(r.getDepartureTime()));
        }
        if (ads.size() > 1) {
            RideAd r = ads.get(1);
            binding.tvRide2Route.setText(r.getOrigin() + " → " + r.getDestination());
            binding.tvRide2Meta.setText(r.getAvailableSeats() + " seat(s) | " + com.safar.app.utils.DateUtils.formatShort(r.getDepartureTime()));
        }
    }

    @Override public void onAdPosted(String adId) {}
    @Override public void onAdDeleted() {}
    @Override public void onError(String msg) {}
    @Override public void onLoading(boolean loading) {
        if (binding != null) binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override public void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override public void onStop() { super.onStop(); if (mapView != null) mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override public void onDestroyView() {
        super.onDestroyView();
        if (presenter != null) presenter.detach();
        if (mapView != null) { mapView.onDestroy(); mapView = null; }
        binding = null;
        googleMap = null;
    }
}
