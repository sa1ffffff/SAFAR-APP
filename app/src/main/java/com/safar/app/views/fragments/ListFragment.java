package com.safar.app.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.safar.app.R;
import com.safar.app.databinding.FragmentListBinding;
import com.safar.app.models.RideAd;
import com.safar.app.presenters.AdsPresenter;
import com.safar.app.utils.AppConstants;
import com.safar.app.views.activities.AdDetailActivity;
import com.safar.app.views.adapters.RideAdAdapter;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends Fragment
        implements AdsPresenter.AdsView, RideAdAdapter.OnAdClickListener {

    private FragmentListBinding binding;
    private AdsPresenter presenter;
    private RideAdAdapter adapter;
    private List<RideAd> allAds = new ArrayList<>();

    // "all" | "city" | "long"
    private String currentTab = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── RecyclerView ────────────────────────────────────────────────────
        adapter = new RideAdAdapter(new ArrayList<>(), this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // ── TabLayout ───────────────────────────────────────────────────────
        // Add tabs programmatically — safer than XML string resources for i18n
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All Rides"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("My City"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Long Distance"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1:  currentTab = "city"; break;
                    case 2:  currentTab = "long"; break;
                    default: currentTab = "all";  break;
                }
                applyFilters();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // ── Search ──────────────────────────────────────────────────────────
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ── Pull-to-refresh ─────────────────────────────────────────────────
        binding.swipeRefresh.setColorSchemeColors(
                requireContext().getColor(R.color.safar_yellow));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            presenter.detach();
            presenter.loadActiveAds();
        });

        // ── Load data ───────────────────────────────────────────────────────
        presenter = new AdsPresenter(this);
        presenter.loadActiveAds();
    }

    // ─── Filtering ──────────────────────────────────────────────────────────

    private void applyFilters() {
        if (binding == null) return;

        String query = binding.etSearch.getText() != null
                ? binding.etSearch.getText().toString().toLowerCase().trim()
                : "";

        List<RideAd> filtered = new ArrayList<>();

        for (RideAd ad : allAds) {
            // ── Search filter ──
            boolean matchesSearch = query.isEmpty()
                    || contains(ad.getOrigin(), query)
                    || contains(ad.getDestination(), query);

            // ── Tab filter ──
            boolean matchesTab = true;
            if ("city".equals(currentTab)) {
                // Same-city rides: origin == destination (carpools within a city)
                matchesTab = ad.getOrigin() != null
                        && ad.getOrigin().equalsIgnoreCase(ad.getDestination());
            } else if ("long".equals(currentTab)) {
                // Intercity rides: different origin and destination
                matchesTab = ad.getOrigin() != null
                        && !ad.getOrigin().equalsIgnoreCase(ad.getDestination());
            }

            if (matchesSearch && matchesTab) filtered.add(ad);
        }

        adapter.updateList(filtered);

        int count = filtered.size();
        binding.tvResultCount.setText(
                count + " ride" + (count == 1 ? "" : "s") + " available");

        boolean empty = filtered.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    // ─── AdsPresenter callbacks ─────────────────────────────────────────────

    @Override
    public void onAdsLoaded(List<RideAd> ads) {
        if (!isAdded() || binding == null) return;
        binding.swipeRefresh.setRefreshing(false);
        allAds = new ArrayList<>(ads);
        applyFilters();
    }

    @Override public void onAdPosted(String adId) {}
    @Override public void onAdDeleted() {}

    @Override
    public void onError(String message) {
        if (!isAdded() || binding == null) return;
        binding.swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onLoading(boolean isLoading) {
        if (binding == null) return;
        if (!isLoading) binding.swipeRefresh.setRefreshing(false);
    }

    // ─── Adapter click ───────────────────────────────────────────────────────

    @Override
    public void onAdClick(RideAd ad) {
        if (!isAdded()) return;
        Intent i = new Intent(requireContext(), AdDetailActivity.class);
        i.putExtra(AppConstants.KEY_AD_ID, ad.getAdId());
        startActivity(i);
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (presenter != null) presenter.detach();
        binding = null;
    }
}
