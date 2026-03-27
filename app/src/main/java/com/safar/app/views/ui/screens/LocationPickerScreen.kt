package com.safar.app.views.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safar.app.views.activities.LocationSuggestion
import com.safar.app.views.activities.RecentLocation
import com.safar.app.views.ui.theme.*

/**
 * LocationPickerScreen - Enterprise-grade location selection screen.
 * Uses Google Maps Compose for robust map integration and lifecycle management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    title: String = "Select Location",
    placeName: String = "",
    placeNameHint: String = "Move map to detect, or type a name",
    searchQuery: String = "",
    isConfirmEnabled: Boolean = false,
    isSearching: Boolean = false,
    isGeocoding: Boolean = false,
    suggestions: List<LocationSuggestion> = emptyList(),
    recentLocations: List<RecentLocation> = emptyList(),
    showSuggestions: Boolean = false,
    showRecentLocations: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onSuggestionClick: (LocationSuggestion) -> Unit = {},
    onRecentLocationClick: (RecentLocation) -> Unit = {},
    onPlaceNameChange: (String) -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onMyLocation: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
    onMapReady: (com.google.android.gms.maps.GoogleMap) -> Unit = {},
    onMapDragStart: () -> Unit = {},
    onMapDragEnd: (Double, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Pakistan center as default
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(30.3753, 69.3451), 5f)
    }

    // Sync camera state with activity logic when idle
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            onMapDragEnd(center.latitude, center.longitude)
        } else {
            onMapDragStart()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SafarBlack
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern Maps Compose Integration
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false
                ),
                properties = MapProperties(
                    isMyLocationEnabled = false
                )
            ) {
                MapEffect(Unit) { map ->
                    onMapReady(map)
                }
            }

            // Center pin indicator (always visible)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Pin location",
                    tint = SafarGreen,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-24).dp)
                )
                // Shadow dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            SafarBlack.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }

            // Top Section: Back button + Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SafarDarkSurface,
                    shadowElevation = 8.dp
                ) {
                    Column {
                        // Title Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go back",
                                    tint = SafarTextPrimary
                                )
                            }

                            Text(
                                text = title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = SafarTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Search Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SafarTextTertiary,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = SafarTextPrimary,
                                    fontSize = 15.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(SafarGreen),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search for a location",
                                                fontSize = 15.sp,
                                                color = SafarTextTertiary
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = SafarTextSecondary
                                    )
                                }
                            }

                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = SafarGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Search Suggestions Dropdown
                AnimatedVisibility(
                    visible = showSuggestions,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SafarDarkSurface,
                        shadowElevation = 8.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                SuggestionItem(
                                    primaryText = suggestion.primaryText,
                                    secondaryText = suggestion.secondaryText,
                                    onClick = { onSuggestionClick(suggestion) }
                                )
                            }
                        }
                    }
                }

                // Recent Locations Dropdown
                AnimatedVisibility(
                    visible = showRecentLocations && !showSuggestions,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SafarDarkSurface,
                        shadowElevation = 8.dp
                    ) {
                        Column {
                            Text(
                                text = "Recent",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = SafarTextSecondary,
                                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                            )
                            recentLocations.take(5).forEach { recent ->
                                RecentLocationItem(
                                    name = recent.name,
                                    address = recent.address,
                                    onClick = { onRecentLocationClick(recent) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Zoom Controls + My Location FAB
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(44.dp),
                    containerColor = SafarDarkSurface,
                    contentColor = SafarTextPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Add, "Zoom in", modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(44.dp),
                    containerColor = SafarDarkSurface,
                    contentColor = SafarTextPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Remove, "Zoom out", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                FloatingActionButton(
                    onClick = onMyLocation,
                    modifier = Modifier.size(48.dp),
                    containerColor = SafarDarkSurface,
                    contentColor = SafarTextPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.MyLocation, "My location")
                }
            }

            // Bottom Sheet: Place name + Confirm
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = SafarDarkSurface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    // Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                SafarTextTertiary.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Place name field
                    Text(
                        text = "LOCATION NAME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarTextTertiary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = placeName,
                            onValueChange = onPlaceNameChange,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = SafarTextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(SafarGreen),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (placeName.isEmpty()) {
                                        Text(
                                            text = placeNameHint,
                                            fontSize = 14.sp,
                                            color = SafarTextTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (isGeocoding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SafarGreen,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Confirm Button
                    Button(
                        onClick = onConfirm,
                        enabled = isConfirmEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafarGreen,
                            contentColor = SafarBlack,
                            disabledContainerColor = SafarGreen.copy(alpha = 0.25f),
                            disabledContentColor = SafarBlack.copy(alpha = 0.25f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Confirm Location",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SafarDarkCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = SafarTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primaryText,
                fontSize = 15.sp,
                color = SafarTextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = secondaryText,
                fontSize = 13.sp,
                color = SafarTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentLocationItem(
    name: String,
    address: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = SafarTextSecondary,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                color = SafarTextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = address,
                fontSize = 13.sp,
                color = SafarTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
