package com.safar.app.ui.post

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.safar.app.R
import com.safar.app.ui.PICK_RESULT
import com.safar.app.ui.back
import com.safar.app.ui.components.BottomActionBar
import com.safar.app.ui.components.Glass
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.IconCircle
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.main.hasLocationPermission
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** On-device geocoding (no Places API key or billing needed). */
internal object Geo {
    @Suppress("DEPRECATION")
    suspend fun search(context: Context, query: String): List<Address> = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        withTimeoutOrNull(GEO_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(query, 6, 23.5, 60.8, 37.1, 77.9, listener(cont))
            } else runCatching { geocoder.getFromLocationName(query, 6, 23.5, 60.8, 37.1, 77.9) }.getOrNull().orEmpty()
        }.orEmpty()
    }

    @Suppress("DEPRECATION")
    suspend fun name(context: Context, at: LatLng): String? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val list: List<Address>? = withTimeoutOrNull(GEO_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(at.latitude, at.longitude, 1, listener(cont))
            } else runCatching { geocoder.getFromLocation(at.latitude, at.longitude, 1) }.getOrNull()
        }
        list?.firstOrNull()?.let(::label)
    }

    /** The city at a point ("Islamabad", "Lahore"), or null if the geocoder can't tell. */
    @Suppress("DEPRECATION")
    suspend fun city(context: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val list: List<Address>? = withTimeoutOrNull(GEO_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, listener(cont))
            } else runCatching { geocoder.getFromLocation(lat, lng, 1) }.getOrNull()
        }
        list?.firstOrNull()?.let { it.locality ?: it.subAdminArea }?.takeIf { it.isNotBlank() }
    }

    private const val GEO_TIMEOUT_MS = 6_000L

    /**
     * The API 33 geocoder reports failures through onError; without it the coroutine
     * would never resume and the picker would sit on "Finding address…" forever.
     */
    @androidx.annotation.RequiresApi(33)
    private fun listener(cont: kotlinx.coroutines.CancellableContinuation<List<Address>>) =
        object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) { if (cont.isActive) cont.resume(addresses) }
            override fun onError(errorMessage: String?) { if (cont.isActive) cont.resume(emptyList()) }
        }

    /** "Street/landmark, Area, City" without the country or postcode noise. */
    fun label(a: Address): String {
        val parts = listOfNotNull(
            // Skip Plus Codes like "P27F+JVM": machine-readable, meaningless to riders.
            a.featureName?.takeIf { f -> f.any(Char::isLetter) && '+' !in f && f != a.subLocality && f != a.locality },
            a.subLocality, a.locality ?: a.subAdminArea,
        ).distinct()
        return parts.joinToString(", ").ifBlank { a.getAddressLine(0) ?: "Dropped pin" }
    }
}

@Composable
fun LocationPickerScreen(field: String, nav: NavHostController) {
    val c = Safar.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(33.6844, 73.0479), 13f) }
    var label by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Address>>(emptyList()) }
    val moving = camera.isMoving
    // The pin lifts while the map moves and settles when it stops, like setting something down.
    val lift by animateDpAsState(if (moving) (-10).dp else 0.dp, tween(180), label = "pin-lift")

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) runCatching {
            @Suppress("MissingPermission")
            val loc = LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
            if (loc != null) camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
        }
    }
    LaunchedEffect(moving) {
        if (!moving) {
            label = null
            label = Geo.name(context, camera.position.target) ?: "Dropped pin"
        }
    }
    LaunchedEffect(query) {
        if (query.length < 3) { results = emptyList(); return@LaunchedEffect }
        delay(350)
        results = Geo.search(context, query)
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            contentPadding = PaddingValues(top = 120.dp, bottom = 120.dp),
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission(context),
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, if (c.isDark) R.raw.map_style_dark else R.raw.map_style_light),
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false, compassEnabled = false),
        )

        // Center pin (sits on the map's padded center)
        Column(
            Modifier.align(Alignment.Center).offset(y = lift - 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (field == "from") Box(Modifier.size(22.dp).clip(CircleShape).background(c.background).border(6.dp, c.content, CircleShape))
            else Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(c.content).border(3.dp, c.background, RoundedCornerShape(4.dp)))
            Box(Modifier.width(3.dp).height(18.dp).background(c.content))
        }

        Column(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() })
                Glass(Modifier.weight(1f), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.height(44.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = c.contentSecondary)
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search area or landmark", style = Safar.type.body, color = c.contentTertiary)
                            BasicTextField(
                                query, { query = it }, singleLine = true,
                                textStyle = Safar.type.body.copy(color = c.content), cursorBrush = SolidColor(c.content),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (query.isNotEmpty()) IconCircle(Icons.Rounded.Close, "Clear", { query = "" }, size = 28.dp)
                    }
                }
            }
            if (results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Glass(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radii.lg)) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        results.forEach { a ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    focus.clearFocus(); query = ""; results = emptyList()
                                    scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(a.latitude, a.longitude), 16f), 600) }
                                }.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Place, null, Modifier.size(20.dp), tint = c.contentSecondary)
                                Text(Geo.label(a), style = Safar.type.bodyStrong, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter)) {
            GlassIconButton(
                Icons.Rounded.MyLocation, "My location",
                {
                    scope.launch {
                        if (!hasLocationPermission(context)) return@launch
                        @Suppress("MissingPermission")
                        val loc = runCatching { LocationServices.getFusedLocationProviderClient(context).lastLocation.await() }.getOrNull()
                        if (loc != null) camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f), 600)
                    }
                },
                Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 8.dp),
            )
            BottomActionBar {
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(if (field == "from") "Pickup" else "Drop-off", style = Safar.type.caption, color = c.contentSecondary)
                    Text(
                        if (moving || label == null) "Finding address…" else label!!,
                        style = Safar.type.bodyStrong, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                SafarButton(
                    "Confirm",
                    onClick = {
                        val at = camera.position.target
                        nav.previousBackStackEntry?.savedStateHandle?.set(PICK_RESULT, "$field|${at.latitude}|${at.longitude}|${label ?: "Dropped pin"}")
                        nav.back()
                    },
                    enabled = !moving && label != null,
                )
            }
        }
    }
}

