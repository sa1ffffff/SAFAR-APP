package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safar.app.views.ui.screens.MyAdsScreen
import com.safar.app.views.ui.screens.MyAdsUiState
import com.safar.app.views.ui.screens.RideAdUi
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MY ADS BRIDGE
// Connects Java MyAdsActivity to Jetpack Compose MyAdsScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Filter options for ads list.
 */
enum class AdsFilter(val displayName: String) {
    ALL("All"),
    ACTIVE("Active"),
    FILLED("Filled"),
    EXPIRED("Expired")
}

class MyAdsStateHolder {
    var state by mutableStateOf(MyAdsUiState())
        private set
    
    var currentFilter by mutableStateOf(AdsFilter.ALL)
        private set
    
    var isRefreshing by mutableStateOf(false)
    
    private var allAds = listOf<RideAdUi>()
    
    val filteredAds: List<RideAdUi>
        get() = when (currentFilter) {
            AdsFilter.ALL -> allAds
            AdsFilter.ACTIVE -> allAds.filter { it.status == "Active" }
            AdsFilter.FILLED -> allAds.filter { it.status == "Filled" }
            AdsFilter.EXPIRED -> allAds.filter { it.status == "Expired" }
        }
    
    fun updateLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }
    
    fun updateAds(ads: List<RideAdUi>) {
        allAds = ads
        state = state.copy(
            ads = filteredAds,
            isLoading = false,
            isEmpty = ads.isEmpty()
        )
        isRefreshing = false
    }
    
    fun setFilter(filter: AdsFilter) {
        currentFilter = filter
        state = state.copy(ads = filteredAds)
    }
    
    fun setError(message: String) {
        state = state.copy(
            errorMessage = message,
            isLoading = false
        )
        isRefreshing = false
    }
    
    fun removeAd(adId: String) {
        allAds = allAds.filter { it.id != adId }
        state = state.copy(
            ads = filteredAds,
            isEmpty = allAds.isEmpty(),
            showDeleteDialog = false,
            selectedAd = null
        )
    }
    
    fun startRefresh() {
        isRefreshing = true
    }

    fun showDeleteConfirmation(ad: RideAdUi) {
        state = state.copy(showDeleteDialog = true, selectedAd = ad)
    }

    fun dismissDeleteDialog() {
        state = state.copy(showDeleteDialog = false, selectedAd = null)
    }
}

fun MyAdsActivity.setupComposeUI(stateHolder: MyAdsStateHolder) {
    setContent {
        SafarTheme(darkTheme = true) {
            MyAdsScreen(
                state = stateHolder.state,
                currentFilter = stateHolder.currentFilter,
                filters = AdsFilter.entries,
                isRefreshing = stateHolder.isRefreshing,
                onFilterChange = { filter: Any ->
                    if (filter is AdsFilter) {
                        stateHolder.setFilter(filter)
                    }
                },
                onAdClick = { adId ->
                    navigateToAdDetail(adId)
                },
                onAdDelete = { ad ->
                    stateHolder.showDeleteConfirmation(ad)
                },
                onConfirmDelete = { adId ->
                    stateHolder.updateLoading(true)
                    handleDeleteAd(adId)
                },
                onDismissDeleteDialog = {
                    stateHolder.dismissDeleteDialog()
                },
                onAdEdit = { adId ->
                    // Logic for edit
                },
                onRefresh = {
                    stateHolder.startRefresh()
                    handleRefreshAds()
                },
                onPostNewAd = {
                    handlePostNewAd()
                },
                onBack = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }
}
