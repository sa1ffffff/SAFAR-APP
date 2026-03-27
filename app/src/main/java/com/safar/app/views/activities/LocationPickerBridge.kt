package com.safar.app.views.activities

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.GoogleMap
import com.safar.app.views.ui.screens.LocationPickerScreen
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LOCATION PICKER BRIDGE
// Connects Java LocationPickerActivity to Jetpack Compose LocationPickerScreen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class LocationSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val distance: String? = null
)

data class RecentLocation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LocationPickerType {
    PICKUP,
    DESTINATION,
    GENERIC
}

class LocationPickerStateHolder {

    var placeName by mutableStateOf("")
    var placeNameHint by mutableStateOf("Move map to detect, or type a name")
    var isConfirmEnabled by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var isSearching by mutableStateOf(false)
    var isMapDragging by mutableStateOf(false)
        internal set
    var isGeocoding by mutableStateOf(false)
    val suggestions = mutableStateListOf<LocationSuggestion>()
    val recentLocations = mutableStateListOf<RecentLocation>()
    var selectedLatitude by mutableStateOf(0.0)
        internal set
    var selectedLongitude by mutableStateOf(0.0)
        internal set

    val showSuggestions: Boolean
        get() = searchQuery.isNotEmpty() && suggestions.isNotEmpty()

    val showRecentLocations: Boolean
        get() = searchQuery.isEmpty() && recentLocations.isNotEmpty()

    fun updateSuggestions(newSuggestions: List<LocationSuggestion>) {
        suggestions.clear()
        suggestions.addAll(newSuggestions)
        isSearching = false
    }

    fun updateRecentLocations(locations: List<RecentLocation>) {
        recentLocations.clear()
        recentLocations.addAll(locations)
    }

    fun clearSuggestions() {
        suggestions.clear()
    }

    fun updateCoordinates(lat: Double, lng: Double) {
        selectedLatitude = lat
        selectedLongitude = lng
    }

    internal fun updateMapDragging(dragging: Boolean) {
        isMapDragging = dragging
        if (dragging) {
            isConfirmEnabled = false
            placeName = ""
            placeNameHint = "Detecting location..."
        }
    }

    fun setDetectedPlace(name: String) {
        placeName = name
        placeNameHint = "Move map to detect, or type a name"
        isConfirmEnabled = name.isNotBlank()
        isGeocoding = false
    }

    fun reset() {
        placeName = ""
        searchQuery = ""
        isConfirmEnabled = false
        isSearching = false
        suggestions.clear()
    }
}

@JvmOverloads
fun LocationPickerActivity.setupComposeUI(
    stateHolder: LocationPickerStateHolder,
    title: String,
    pickerType: LocationPickerType = LocationPickerType.GENERIC
) {
    setContent {
        SafarTheme(darkTheme = true) {
            LocationPickerScreen(
                title = title,
                placeName = stateHolder.placeName,
                placeNameHint = stateHolder.placeNameHint,
                searchQuery = stateHolder.searchQuery,
                isConfirmEnabled = stateHolder.isConfirmEnabled,
                isSearching = stateHolder.isSearching,
                isGeocoding = stateHolder.isGeocoding,
                suggestions = stateHolder.suggestions,
                recentLocations = stateHolder.recentLocations,
                showSuggestions = stateHolder.showSuggestions,
                showRecentLocations = stateHolder.showRecentLocations,
                onSearchQueryChange = { query ->
                    stateHolder.searchQuery = query
                    if (query.length >= 2) {
                        stateHolder.isSearching = true
                        handleSearch(query)
                    } else {
                        stateHolder.clearSuggestions()
                    }
                },
                onSuggestionClick = { suggestion ->
                    stateHolder.searchQuery = suggestion.primaryText
                    stateHolder.placeName = suggestion.primaryText
                    stateHolder.isConfirmEnabled = true
                    stateHolder.clearSuggestions()
                    handleSuggestionSelected(suggestion.placeId)
                },
                onRecentLocationClick = { recent ->
                    stateHolder.placeName = recent.name
                    stateHolder.isConfirmEnabled = true
                    handleRecentLocationSelected(recent.id)
                },
                onPlaceNameChange = { name ->
                    stateHolder.placeName = name
                    stateHolder.isConfirmEnabled = name.isNotBlank()
                },
                onZoomIn = { handleZoomIn() },
                onZoomOut = { handleZoomOut() },
                onMyLocation = { handleMyLocation() },
                onConfirm = {
                    if (stateHolder.isConfirmEnabled) {
                        handleConfirm(stateHolder.placeName)
                    }
                },
                onBack = { onBackPressedDispatcher.onBackPressed() },
                onMapReady = { map: GoogleMap ->          // ✅ FIX: explicit type
                    this@setupComposeUI.onMapReady(map)
                },
                onMapDragStart = { stateHolder.updateMapDragging(true) },
                onMapDragEnd = { lat, lng ->
                    stateHolder.updateMapDragging(false)
                    stateHolder.updateCoordinates(lat, lng)
                    stateHolder.isGeocoding = true
                    handleMapCenterChanged(lat, lng)
                }
            )
        }
    }
}