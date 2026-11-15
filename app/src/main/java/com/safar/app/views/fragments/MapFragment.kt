package com.safar.app.views.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safar.app.R
import com.safar.app.models.RideAd
import com.safar.app.presenters.AdsPresenter
import com.safar.app.utils.AppConstants
import com.safar.app.utils.DateUtils
import com.safar.app.utils.SessionManager
import com.safar.app.views.activities.AdDetailActivity
import com.safar.app.views.activities.MainActivity
import com.safar.app.views.activities.MyAdsActivity
import com.safar.app.views.activities.NotificationsActivity
import com.safar.app.views.activities.PostAdActivity
import com.safar.app.views.activities.ProfileActivity
import com.safar.app.presenters.NotificationPresenter
import com.safar.app.models.Notification
import com.safar.app.views.ui.theme.SafarTheme

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Premium Map Screen
// Material 3 Design with Uber-style floating panels and micro-interactions
// ══════════════════════════════════════════════════════════════════════════════

// ── UI State ──────────────────────────────────────────────────────────────────
data class MapUiState(
    val isLoading: Boolean = true,
    val ride1Route: String = "",
    val ride1Meta: String = "",
    val showRide1: Boolean = false,
    val ride2Route: String = "",
    val ride2Meta: String = "",
    val showRide2: Boolean = false,
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val notificationCount: Int = 0
)

// ── State Holder ──────────────────────────────────────────────────────────────
class MapStateHolder {
    var state by mutableStateOf(MapUiState())
        private set

    fun setLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }

    fun setRide1(route: String, meta: String) {
        state = state.copy(ride1Route = route, ride1Meta = meta, showRide1 = true)
    }

    fun setRide2(route: String, meta: String) {
        state = state.copy(ride2Route = route, ride2Meta = meta, showRide2 = true)
    }

    fun clearRides() {
        state = state.copy(showRide1 = false, showRide2 = false)
    }

    fun setUserInfo(name: String, avatarUrl: String?) {
        state = state.copy(userName = name, userAvatarUrl = avatarUrl)
    }

    fun setNotificationCount(count: Int) {
        state = state.copy(notificationCount = count)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// FRAGMENT
// ══════════════════════════════════════════════════════════════════════════════

class MapFragment : Fragment(), OnMapReadyCallback, AdsPresenter.AdsView, NotificationPresenter.NotificationView {

    private val stateHolder = MapStateHolder()
    private var mapViewRef: MapView? = null
    private var googleMap: GoogleMap? = null
    private var presenter: AdsPresenter? = null
    private var notificationPresenter: NotificationPresenter? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val markerAdMap = HashMap<String, RideAd>()
    private val recentAds = mutableListOf<RideAd>()
    private var pendingAds: List<RideAd>? = null
    private var mapReady = false
    private var centerAfterPermissionGrant = false

    // ── Location Permission Launcher ──────────────────────────────────────────
    private val locationLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fine || coarse) {
                if (googleMap != null) enableMyLocation()
                if (centerAfterPermissionGrant) {
                    centerAfterPermissionGrant = false
                    centerOnMyLocation()
                }
            } else {
                centerAfterPermissionGrant = false
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Location permission is required to center on your location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SafarTheme(darkTheme = true) {
                MapScreen(
                    state = stateHolder.state,
                    onMapReady = { mv ->
                        mapViewRef = mv
                        mv.onCreate(savedInstanceState)
                        mv.getMapAsync(this@MapFragment)
                    },
                    onPostRide = {
                        startActivity(Intent(requireContext(), PostAdActivity::class.java))
                    },
                    onExplore = { openListTab() },
                    onMyLocation = { centerOnMyLocation() },
                    onSearchTap = { openListTab() },
                    onSeeAllRides = { openListTab() },
                    onRide1Click = { openFirstAd() },
                    onRide2Click = { openSecondAd() },
                    onProfileClick = { openProfile() },
                    onNotificationClick = { openNotifications() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        presenter = AdsPresenter(this)
        presenter?.loadActiveAds()
        loadUserInfo()
        loadNotificationCount()
    }

    private fun loadUserInfo() {
        if (!isAdded) return
        val session = SessionManager(requireContext())
        val userName = session.userName ?: ""
        val userAvatarUrl = session.userPhotoUrl
        stateHolder.setUserInfo(userName, userAvatarUrl)
    }

    private fun loadNotificationCount() {
        if (!isAdded) return
        val session = SessionManager(requireContext())
        val userId = session.currentUserId ?: return
        notificationPresenter = NotificationPresenter(this)
        notificationPresenter?.listenForUnreadCount(userId)
    }

    // ── Shared Presenter Callbacks (Satisfies both AdsPresenter.AdsView and NotificationPresenter.NotificationView) ──
    override fun onError(message: String?) {
        // Shared error handler for notification count and ads loading
    }

    override fun onLoading(isLoading: Boolean) {
        stateHolder.setLoading(isLoading)
    }

    // ── NotificationPresenter.NotificationView Implementation ────────────────
    override fun onNotificationsLoaded(notifications: MutableList<Notification>?) {
        // Not needed in this fragment
    }

    override fun onUnreadCountUpdated(count: Int) {
        if (!isAdded) return
        requireActivity().runOnUiThread {
            stateHolder.setNotificationCount(count)
        }
    }

    override fun onNotificationSent() {
        // Not needed in this fragment
    }

    // ── Navigation Helpers ────────────────────────────────────────────────────
    private fun openListTab() {
        if (!isAdded) return
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.onTabSelected(1)
        } else {
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ListFragment())
                .commit()
        }
    }

    private fun openProfile() {
        if (!isAdded) return
        startActivity(Intent(requireContext(), ProfileActivity::class.java))
    }

    private fun openNotifications() {
        if (!isAdded) return
        startActivity(Intent(requireContext(), NotificationsActivity::class.java))
    }

    private fun openFirstAd() {
        if (recentAds.isNotEmpty()) openAdDetail(recentAds[0].adId)
        else openListTab()
    }

    private fun openSecondAd() {
        if (recentAds.size > 1) openAdDetail(recentAds[1].adId)
        else if (recentAds.isNotEmpty()) openAdDetail(recentAds[0].adId)
        else openListTab()
    }

    private fun openAdDetail(adId: String?) {
        if (adId == null || !isAdded) return
        val intent = Intent(requireContext(), AdDetailActivity::class.java)
        intent.putExtra(AppConstants.KEY_AD_ID, adId)
        startActivity(intent)
    }

    // ── Map Ready ─────────────────────────────────────────────────────────────
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapReady = true

        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        } catch (ignored: Exception) {
        }

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(AppConstants.DEFAULT_LAT, AppConstants.DEFAULT_LNG), 13f
            )
        )

        map.setOnMarkerClickListener { marker ->
            val adId = marker.tag?.toString()
            if (adId != null && markerAdMap.containsKey(adId)) {
                AdBottomSheetFragment.newInstance(adId)
                    .show(childFragmentManager, "BottomSheet")
            }
            true
        }

        requestLocationPermission()
        pendingAds?.let { renderMarkers(it); pendingAds = null }
    }

    // ── Location ──────────────────────────────────────────────────────────────
    private fun requestLocationPermission(centerOnGrant: Boolean = false) {
        if (!isAdded) return
        if (hasLocationPermission()) {
            enableMyLocation()
            if (centerOnGrant) centerOnMyLocation()
            return
        }
        centerAfterPermissionGrant = centerOnGrant
        locationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        if (!isAdded) return false
        val context = requireContext()
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @Suppress("MissingPermission")
    private fun enableMyLocation() {
        googleMap?.isMyLocationEnabled = true
    }

    @Suppress("MissingPermission")
    private fun centerOnMyLocation() {
        if (!isAdded) return
        if (!hasLocationPermission()) {
            requestLocationPermission(centerOnGrant = true)
            return
        }
        enableMyLocation()
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            15f
                        )
                    )
                } else {
                    fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { currentLocation ->
                            if (currentLocation != null) {
                                googleMap?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(currentLocation.latitude, currentLocation.longitude),
                                        15f
                                    )
                                )
                            } else if (isAdded) {
                                Toast.makeText(
                                    requireContext(),
                                    "Could not determine current location.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener {
                            if (isAdded) {
                                Toast.makeText(
                                    requireContext(),
                                    "Could not determine current location.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Could not determine current location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // ── Ride Marker Creation ──────────────────────────────────────────────────
    private fun makeRideMarker(labelText: String): BitmapDescriptor {
        val d = requireContext().resources.displayMetrics.density
        val iconSize = (48 * d).toInt()

        val rawIcon = BitmapFactory.decodeResource(resources, R.drawable.car_marker)
            ?: return BitmapDescriptorFactory.defaultMarker()
        val carIcon = Bitmap.createScaledBitmap(rawIcon, iconSize, iconSize, true)
        rawIcon.recycle()

        val labelH = (26 * d).toInt()
        val labelPad = (12 * d).toInt()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11 * d
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val textW = textPaint.measureText(labelText)
        val labelW = (textW + labelPad * 2).toInt()

        val gap = (6 * d).toInt()
        val totalW = iconSize + gap + labelW
        val totalH = iconSize

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(carIcon, 0f, 0f, null)
        carIcon.recycle()

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E61C1B1F")
            style = Paint.Style.FILL
        }
        val pillTop = (totalH - labelH) / 2f
        val pill = RectF((iconSize + gap).toFloat(), pillTop, totalW.toFloat(), pillTop + labelH)
        canvas.drawRoundRect(pill, labelH / 2f, labelH / 2f, bgPaint)

        textPaint.color = android.graphics.Color.WHITE
        val textX = iconSize + gap + labelPad.toFloat()
        val textY = pillTop + (labelH / 2f) + (textPaint.textSize / 3f)
        canvas.drawText(labelText, textX, textY, textPaint)

        return BitmapDescriptorFactory.fromBitmap(result)
    }

    private fun renderMarkers(ads: List<RideAd>) {
        if (googleMap == null || !isAdded) return
        googleMap?.clear()
        markerAdMap.clear()
        for (ad in ads) {
            if (ad.originLat == 0.0) continue
            val pos = LatLng(ad.originLat, ad.originLng)
            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(pos)
                    .icon(makeRideMarker(ad.origin ?: ""))
                    .anchor(0.5f, 0.5f)
            )
            marker?.apply {
                tag = ad.adId
                markerAdMap[ad.adId ?: ""] = ad
            }
        }
    }

    // ── AdsPresenter Callbacks ────────────────────────────────────────────────
    override fun onAdsLoaded(ads: List<RideAd>) {
        if (!isAdded) return
        recentAds.clear()
        recentAds.addAll(ads)

        if (mapReady) renderMarkers(ads)
        else pendingAds = ArrayList(ads)

        if (ads.isNotEmpty()) {
            val r = ads[0]
            stateHolder.setRide1(
                "${r.origin} -> ${r.destination}",
                "${r.availableSeats} seat(s) | ${DateUtils.formatShort(r.departureTime)}"
            )
        }
        if (ads.size > 1) {
            val r = ads[1]
            stateHolder.setRide2(
                "${r.origin} -> ${r.destination}",
                "${r.availableSeats} seat(s) | ${DateUtils.formatShort(r.departureTime)}"
            )
        }
    }

    override fun onAdPosted(adId: String) {}
    override fun onAdDeleted() {}

    // ── MapView Lifecycle Forwarding ──────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        mapViewRef?.onResume()
    }

    override fun onPause() {
        mapViewRef?.onPause()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapViewRef?.onStart()
    }

    override fun onStop() {
        mapViewRef?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewRef?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewRef?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.detach()
        notificationPresenter?.detach()
        mapViewRef?.onDestroy()
        mapViewRef = null
        googleMap = null
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPOSABLE UI - Premium Uber-Style Map Screen
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: MapUiState,
    onMapReady: (MapView) -> Unit,
    onPostRide: () -> Unit,
    onExplore: () -> Unit,
    onMyLocation: () -> Unit,
    onSearchTap: () -> Unit,
    onSeeAllRides: () -> Unit,
    onRide1Click: () -> Unit,
    onRide2Click: () -> Unit,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { context ->
                MapView(context).also { onMapReady(it) }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Top Bar with Search, Profile Avatar, and Notification Bell
        TopSearchBar(
            userAvatarUrl = state.userAvatarUrl,
            userName = state.userName,
            notificationCount = state.notificationCount,
            onSearchTap = onSearchTap,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        )

        // Floating Action Buttons (Right Side)
        FloatingActionColumn(
            onMyLocation = onMyLocation,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // Bottom Panel
        BottomRidePanel(
            state = state,
            onPostRide = onPostRide,
            onExplore = onExplore,
            onSeeAllRides = onSeeAllRides,
            onRide1Click = onRide1Click,
            onRide2Click = onRide2Click,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopSearchBar(
    userAvatarUrl: String?,
    userName: String,
    notificationCount: Int = 0,
    onSearchTap: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .shadow(4.dp, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false)
                ) { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (!userAvatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(userAvatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                // Fallback to initials
                val initials = userName.take(1).uppercase().ifEmpty { "U" }
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search Bar
        Surface(
            modifier = Modifier
                .weight(1f)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    ) { onSearchTap() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Where are you going?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Notification Bell Icon with subtle unread indicator
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .shadow(4.dp, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false)
                ) { onNotificationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (notificationCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            // Small unread dot
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = (-1).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun FloatingActionColumn(
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // My Location Button
        FloatingActionButton(
            onClick = onMyLocation,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "My Location",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun BottomRidePanel(
    state: MapUiState,
    onPostRide: () -> Unit,
    onExplore: () -> Unit,
    onSeeAllRides: () -> Unit,
    onRide1Click: () -> Unit,
    onRide2Click: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Filled.AddCircle,
                    label = "Post Ride",
                    isPrimary = true,
                    onClick = onPostRide,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Filled.Explore,
                    label = "Explore",
                    isPrimary = false,
                    onClick = onExplore,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Rides Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Rides",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSeeAllRides) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Loading State
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Ride Cards
            AnimatedVisibility(
                visible = !state.isLoading && (state.showRide1 || state.showRide2),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.showRide1) {
                        RidePreviewCard(
                            route = state.ride1Route,
                            meta = state.ride1Meta,
                            onClick = onRide1Click
                        )
                    }
                    if (state.showRide2) {
                        RidePreviewCard(
                            route = state.ride2Route,
                            meta = state.ride2Meta,
                            onClick = onRide2Click
                        )
                    }
                }
            }

            // Empty State
            AnimatedVisibility(
                visible = !state.isLoading && !state.showRide1 && !state.showRide2,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EmptyRidesState()
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPrimary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = if (isPrimary) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun RidePreviewCard(
    route: String,
    meta: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Car Icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyRidesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No rides nearby",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Be the first to post a ride!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
