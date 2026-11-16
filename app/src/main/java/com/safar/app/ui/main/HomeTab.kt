package com.safar.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.safar.app.ui.components.rememberRoadRoute
import com.safar.app.ui.components.routePoints
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.safar.app.AppContainer
import com.safar.app.R
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.Ride
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.EmptyState
import com.safar.app.ui.components.Glass
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.RideCard
import com.safar.app.ui.components.RideCardSkeleton
import com.safar.app.ui.components.pressable
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope

private val DefaultCenter = LatLng(33.6844, 73.0479) // Islamabad

data class HomeState(
    val rides: List<Ride>? = null,
    val ratings: Map<String, RatingSummary> = emptyMap(),
    val location: LatLng? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val app: AppContainer) : SafarViewModel() {
    private val location = MutableStateFlow<LatLng?>(null)

    private val rides = app.rides.observeOpenRides()
        .map { list -> list.filter { it.creatorUid != app.session.uid } }
        .catch { say("Couldn't load rides. Pull down to retry."); emit(emptyList()) }

    private val ratings = rides
        .map { list -> list.map { it.creatorUid }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { app.users.observeRatings(it).catch { emit(emptyMap()) } }

    val state: StateFlow<HomeState> = combine(rides, ratings, location) { list, r, here ->
        val sorted = if (here == null) list else list.sortedBy { distanceKm(here, it.origin.lat, it.origin.lng) }
        HomeState(sorted, r, here)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    @SuppressLint("MissingPermission")
    fun refreshLocation(context: Context, onFound: (LatLng) -> Unit = {}) {
        if (!hasLocationPermission(context)) return
        viewModelScope.launch {
            runCatching {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val loc: Location? = client.lastLocation.await()
                    ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                loc?.let { LatLng(it.latitude, it.longitude) }
            }.getOrNull()?.let {
                location.value = it
                onFound(it)
            }
        }
    }
}

fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun distanceKm(a: LatLng, lat: Double, lng: Double): Float {
    val out = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, lat, lng, out)
    return out[0] / 1000f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTab(nav: NavHostController, onSearch: () -> Unit, onOpenAccount: () -> Unit) {
    val vm = appViewModel { HomeViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val context = LocalContext.current
    val density = LocalDensity.current
    val state by vm.state.collectAsStateWithLifecycle()
    val session = context.container().session
    val me by session.user.collectAsStateWithLifecycle()
    val unread by session.unreadNotifications.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(DefaultCenter, 11f) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    // The sheet collapses to just the search bar, giving the map the screen. Drag or tap the handle.
    var expanded by rememberSaveable { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current
    val dragY = remember { Animatable(0f) }
    fun settle(open: Boolean) {
        if (open != expanded) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        expanded = open
        scope.launch { dragY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) }
    }
    var selected by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    val listState = rememberLazyListState()
    val rides = state.rides.orEmpty()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) vm.refreshLocation(context) { scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(it, 13f), 600) } }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) vm.refreshLocation(context) { camera.move(CameraUpdateFactory.newLatLngZoom(it, 12f)) }
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // First load: frame you together with the nearest rides, so rides elsewhere in the city
    // are on screen instead of hiding past the edge. Runs once; after that the camera is the user's.
    var framed by rememberSaveable { mutableStateOf(false) }
    // Bounds only fit correctly once the map has laid out with its padding.
    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(rides.isNotEmpty(), state.location, mapLoaded, sheetHeight > 0) {
        if (framed || state.rides == null || !mapLoaded || sheetHeight == 0) return@LaunchedEffect
        val nearest = rides.take(6)
        if (nearest.isEmpty()) return@LaunchedEffect
        val bounds = LatLngBounds.builder().apply {
            nearest.forEach { include(LatLng(it.origin.lat, it.origin.lng)) }
            state.location?.let { include(it) }
        }.build()
        runCatching { camera.animate(CameraUpdateFactory.newLatLngBounds(bounds, 180), 700) }
        framed = true
    }

    // The card that settles in the carousel drives the map: its route is drawn and framed.
    // Only after the user has swiped, so the first load keeps the camera on their location.
    LaunchedEffect(listState, rides) {
        var swiped = false
        snapshotFlow { listState.firstVisibleItemIndex to listState.isScrollInProgress }
            .collect { (index, scrolling) ->
                if (scrolling) swiped = true
                if (!scrolling && swiped && rides.isNotEmpty()) {
                    val ride = rides.getOrNull(index) ?: return@collect
                    if (selected != ride.id) {
                        selected = ride.id
                        frameRide(camera, ride)
                    }
                }
            }
    }

    val bottomPad = with(density) { sheetHeight.toDp() }
    val darkMap = c.isDark

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            contentPadding = PaddingValues(bottom = bottomPad, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp),
            properties = MapProperties(
                isMyLocationEnabled = hasPermission,
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context, if (darkMap) R.raw.map_style_dark else R.raw.map_style_light
                ),
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, myLocationButtonEnabled = false, compassEnabled = false,
                mapToolbarEnabled = false, indoorLevelPickerEnabled = false,
            ),
            onMapClick = { selected = null },
            onMapLoaded = { mapLoaded = true },
        ) {
            rides.forEach { ride ->
                val pos = LatLng(ride.origin.lat, ride.origin.lng)
                val isSelected = ride.id == selected
                MarkerComposable(
                    ride.id, isSelected, ride.availableSeats, c.isDark,
                    state = remember(ride.id, pos) { MarkerState(pos) },
                    zIndex = if (isSelected) 2f else 1f,
                    // One tap goes straight to the ride, where its route is shown and seats are requested.
                    // Swiping the cards below is the way to preview routes without leaving Home.
                    onClick = {
                        nav.navigate(Routes.ride(ride.id))
                        true
                    },
                ) {
                    SeatMarker(ride.availableSeats, isSelected)
                }
            }
            val active = rides.firstOrNull { it.id == selected }
            val road = rememberRoadRoute(active)
            if (active != null) {
                Polyline(
                    points = active.routePoints(road),
                    color = c.content, width = with(density) { 4.dp.toPx() }, geodesic = road == null,
                )
                MarkerComposable(
                    "dest-${active.id}", c.isDark,
                    state = remember(active.id) { MarkerState(LatLng(active.destination.lat, active.destination.lng)) },
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                ) {
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(c.content).border(2.dp, c.background, RoundedCornerShape(3.dp)))
                }
            }
        }

        // Top floating controls
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glass(shape = CircleShape) {
                Row(
                    Modifier.pressable(remember { MutableInteractionSource() }, onClick = onOpenAccount)
                        .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Avatar(me?.name.orEmpty(), me?.photoUrl, size = 32.dp)
                    Text("Salam, ${me?.firstName ?: ""}", style = Safar.type.bodyStrong, color = c.content)
                }
            }
            Spacer(Modifier.weight(1f))
            GlassIconButton(Icons.Outlined.Notifications, "Notifications", { nav.navigate(Routes.NOTIFICATIONS) }, badge = unread > 0)
        }

        // Bottom sheet
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { sheetHeight = it.height }
        ) {
            GlassIconButton(
                Icons.Rounded.MyLocation, "My location",
                onClick = {
                    if (hasPermission) vm.refreshLocation(context) { scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(it, 14f), 600) } }
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                modifier = Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 12.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    // Follows the finger (with resistance past the open position), then springs to a state.
                    .graphicsLayer { translationY = dragY.value }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                val next = dragY.value + delta
                                dragY.snapTo(if (next < 0f) next * 0.3f else next)
                            }
                        },
                        onDragStopped = { velocity ->
                            val threshold = with(density) { 56.dp.toPx() }
                            when {
                                velocity > 800f || dragY.value > threshold -> settle(false)
                                velocity < -800f || dragY.value < -threshold / 3 -> settle(true)
                                else -> settle(expanded)
                            }
                        },
                    )
                    .shadow(24.dp, RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl), ambientColor = Color.Black.copy(0.1f), spotColor = Color.Black.copy(0.12f))
                    .clip(RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl))
                    .background(c.surface)
                    .padding(bottom = TabBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                // The handle: a generous tap target around the visible bar.
                Box(
                    Modifier.fillMaxWidth().height(28.dp)
                        .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = if (expanded) "Collapse" else "Expand") { settle(!expanded) }
                        .semantics { contentDescription = if (expanded) "Collapse rides panel" else "Expand rides panel" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(c.border))
                }
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(Radii.md))
                        .background(c.surfaceMuted)
                        .pressable(remember { MutableInteractionSource() }, onClick = onSearch, pressedScale = 0.985f)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = c.content)
                    Text("Where are you heading?", style = Safar.type.bodyStrong, color = c.contentSecondary)
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), expandFrom = Alignment.Top) + fadeIn(tween(220, delayMillis = 40)),
                    exit = shrinkVertically(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
                ) {
                Column {
                Spacer(Modifier.height(18.dp))
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Leaving soon", style = Safar.type.headline, color = c.content)
                        Text(
                            when {
                                state.rides == null -> "Looking for rides…"
                                rides.isEmpty() -> "No open rides right now"
                                state.location != null -> "${rides.size} open · nearest first"
                                else -> "${rides.size} open"
                            },
                            style = Safar.type.caption, color = c.contentSecondary,
                        )
                    }
                    Row(
                        Modifier.clip(CircleShape).background(c.primary)
                            .pressable(remember { MutableInteractionSource() }, onClick = { nav.navigate(Routes.post()) })
                            .padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp), tint = c.onPrimary)
                        Text("Offer a ride", style = Safar.type.label, color = c.onPrimary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                when {
                    state.rides == null -> Row(Modifier.padding(horizontal = 16.dp)) { RideCardSkeleton(Modifier.width(300.dp)) }
                    rides.isEmpty() -> EmptyState(
                        Icons.Rounded.DirectionsCar, "Be the first today",
                        "Driving somewhere? Offer your empty seats and split the fuel.",
                        modifier = Modifier.padding(vertical = 0.dp),
                    )
                    else -> LazyRow(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(rides, key = { _, r -> r.id }) { _, ride ->
                            RideCard(
                                ride, onClick = { nav.navigate(Routes.ride(ride.id)) },
                                rating = state.ratings[ride.creatorUid],
                                modifier = Modifier.width(300.dp).then(
                                    if (ride.id == selected) Modifier.border(1.5.dp, c.content, RoundedCornerShape(Radii.lg)) else Modifier
                                ),
                            )
                        }
                    }
                }
                }
                }
                if (!expanded) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private suspend fun frameRide(camera: com.google.maps.android.compose.CameraPositionState, ride: Ride) {
    val bounds = LatLngBounds.builder()
        .include(LatLng(ride.origin.lat, ride.origin.lng))
        .include(LatLng(ride.destination.lat, ride.destination.lng))
        .build()
    runCatching { camera.animate(CameraUpdateFactory.newLatLngBounds(bounds, 140), 650) }
}

@Composable
private fun SeatMarker(seats: Int, selected: Boolean) {
    // Solid, inverted pill: the highest-contrast mark on either map style. The ring separates
    // it from roads and from neighbouring markers; the ride being previewed gets a thicker ring.
    val c = Safar.colors
    Row(
        Modifier.clip(CircleShape).background(c.content)
            .border(if (selected) 3.dp else 1.5.dp, c.background, CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(13.dp), tint = c.background)
        Text("$seats", style = Safar.type.caption, color = c.background)
    }
}

private fun Context.container() = (applicationContext as com.safar.app.SafarApp).container
