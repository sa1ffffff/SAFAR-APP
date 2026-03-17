package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.safar.app.databinding.ActivityMyAdsBinding;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.views.adapters.RideAdAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyAdsActivity extends AppCompatActivity implements AdsPresenter.AdsView,
        RideAdAdapter.OnAdClickListener {

    private ActivityMyAdsBinding binding;
    private AdsPresenter presenter;
    private RideAdAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyAdsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Rides");
        }

        adapter = new RideAdAdapter(new ArrayList<>(), this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        presenter = new AdsPresenter(this);
        if (uid != null) presenter.loadMyAds(uid);
    }

    @Override public void onAdsLoaded(List<RideAd> ads) {
        adapter.updateList(ads);
        binding.tvEmpty.setVisibility(ads.isEmpty() ? View.VISIBLE : View.GONE);
    }
    @Override public void onAdPosted(String adId) {}
    @Override public void onAdDeleted() {}
    @Override public void onError(String message) {}
    @Override public void onLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    @Override public void onAdClick(RideAd ad) {
        Intent i = new Intent(this, AdDetailActivity.class);
        i.putExtra(AppConstants.KEY_AD_ID, ad.getAdId());
        startActivity(i);
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
