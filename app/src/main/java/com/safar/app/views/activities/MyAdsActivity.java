package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.views.ui.screens.RideAdUi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyAdsActivity extends AppCompatActivity implements AdsPresenter.AdsView {

    private MyAdsStateHolder stateHolder;
    private AdsPresenter     presenter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DATE_FORMAT_DISPLAY, Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stateHolder = new MyAdsStateHolder();
        MyAdsBridgeKt.setupComposeUI(this, stateHolder);

        presenter = new AdsPresenter(this);
        handleRefreshAds();
    }

    public void navigateToAdDetail(String adId) {
        Intent i = new Intent(this, AdDetailActivity.class);
        i.putExtra(AppConstants.KEY_AD_ID, adId);
        startActivity(i);
    }

    public void handleDeleteAd(String adId) {
        presenter.deleteAd(adId);
    }

    public void handleRefreshAds() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) presenter.loadMyAds(uid);
        else onLoading(false);
    }

    public void handlePostNewAd() {
        Intent i = new Intent(this, PostAdActivity.class);
        startActivity(i);
    }

    @Override
    public void onAdsLoaded(List<RideAd> ads) {
        List<RideAdUi> uiList = new ArrayList<>();
        for (RideAd ad : ads) {
            String timeStr = ad.getDepartureTime() > 0 
                ? dateFormat.format(new Date(ad.getDepartureTime())) 
                : "Not set";
                
            uiList.add(new RideAdUi(
                    ad.getAdId()           != null ? ad.getAdId()           : "",
                    ad.getOrigin()         != null ? ad.getOrigin()         : "",
                    ad.getDestination()    != null ? ad.getDestination()    : "",
                    timeStr,
                    String.valueOf(ad.getAvailableSeats()),
                    ad.getVehicleDetails() != null ? ad.getVehicleDetails() : "",
                    ad.getStatus()         != null ? ad.getStatus()         : "Open",
                    "#06C167",
                    0
            ));
        }
        stateHolder.updateAds(uiList);
    }

    @Override public void onAdPosted(String adId) {}
    
    @Override 
    public void onAdDeleted() {
        Toast.makeText(this, "Ride deleted successfully", Toast.LENGTH_SHORT).show();
        handleRefreshAds();
    }
    
    @Override 
    public void onError(String message) {
        stateHolder.setError(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
