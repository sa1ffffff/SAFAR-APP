package com.safar.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.safar.app.SafarApp
import com.safar.app.data.RoadRoute
import com.safar.app.data.model.Ride
import java.util.concurrent.ConcurrentHashMap

/** Routes already fetched this session, by ride endpoints; a route never changes. */
private val routeCache = ConcurrentHashMap<String, RoadRoute>()

/**
 * The driving route for [ride], fetched once and cached. Null until it arrives, or if there's no
 * route (offline, or the route service is down); callers draw a straight line until then.
 */
@Composable
fun rememberRoadRoute(ride: Ride?): RoadRoute? {
    if (ride == null) return null
    val key = "${ride.origin.lat},${ride.origin.lng}>${ride.destination.lat},${ride.destination.lng}"
    val api = (LocalContext.current.applicationContext as SafarApp).container.api
    var route by remember(key) { mutableStateOf(routeCache[key]) }
    LaunchedEffect(key) {
        if (route != null) return@LaunchedEffect
        runCatching { api.route(ride.origin.lat, ride.origin.lng, ride.destination.lat, ride.destination.lng) }
            .getOrNull()?.takeIf { it.points.size >= 2 }?.let { routeCache[key] = it; route = it }
    }
    return route
}

/** The route's points as map coordinates, or the straight pickup → drop-off line as a fallback. */
fun Ride.routePoints(route: RoadRoute?): List<LatLng> =
    route?.points?.map { (lat, lng) -> LatLng(lat, lng) }
        ?: listOf(LatLng(origin.lat, origin.lng), LatLng(destination.lat, destination.lng))

/** "12 km · 25 min" (or "800 m · 4 min"). */
fun RoadRoute.summary(): String {
    val distance = if (meters < 1000) "$meters m" else "%.0f km".format(meters / 1000.0).let { if (meters < 10_000) "%.1f km".format(meters / 1000.0) else it }
    val minutes = (seconds + 59) / 60
    val time = if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
    return "$distance · $time"
}
