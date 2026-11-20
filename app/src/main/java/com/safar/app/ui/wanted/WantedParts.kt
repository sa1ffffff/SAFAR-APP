package com.safar.app.ui.wanted

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.safar.app.R
import com.safar.app.data.model.distanceLabel
import com.safar.app.ui.components.initials
import com.safar.app.ui.theme.Safar
import com.safar.app.ui.timeLabel

/**
 * A wanted ride's time window drawn as a band on a clock, with matching rides as ticks.
 * The scale runs a little past the window on both sides so the band reads as a span of
 * the day, and the strongest tick is drawn tallest.
 */
@Composable
fun TimeRibbon(
    windowStart: Long,
    windowEnd: Long,
    modifier: Modifier = Modifier,
    preferred: Long? = null,
    ticks: List<Long> = emptyList(),
    labels: Boolean = true,
) {
    val c = Safar.colors
    val span = (windowEnd - windowStart).coerceAtLeast(1)
    val pad = if (span >= 20 * 60 * 60_000L) 0L else (span * 0.4).toLong().coerceAtLeast(20 * 60_000L)
    val from = windowStart - pad
    val to = windowEnd + pad
    fun x(t: Long) = ((t - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    val ink = c.content
    val band = c.content.copy(alpha = 0.12f)
    val axis = c.border
    val faint = c.contentTertiary

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val mid = size.height / 2
            drawLine(axis, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2.dp.toPx())
            val b0 = x(windowStart) * size.width
            val b1 = x(windowEnd) * size.width
            val bh = 16.dp.toPx()
            drawRoundRect(band, Offset(b0, mid - bh / 2), Size((b1 - b0).coerceAtLeast(bh), bh), CornerRadius(bh / 2))
            ticks.forEachIndexed { i, t ->
                val tx = x(t) * size.width
                val h = if (i == 0) 26.dp.toPx() else 16.dp.toPx()
                drawRoundRect(if (i == 0) ink else faint, Offset(tx - 1.5.dp.toPx(), mid - h / 2), Size(3.dp.toPx(), h), CornerRadius(2.dp.toPx()))
            }
            if (preferred != null && ticks.isEmpty()) {
                val px = x(preferred) * size.width
                val h = 26.dp.toPx()
                drawRoundRect(ink, Offset(px - 1.5.dp.toPx(), mid - h / 2), Size(3.dp.toPx(), h), CornerRadius(2.dp.toPx()))
            }
        }
        if (labels) BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)) {
            val w = maxWidth
            val start = if (span >= 20 * 60 * 60_000L) "Any time" else timeLabel(windowStart)
            Text(start, style = Safar.type.caption, color = c.contentTertiary, modifier = Modifier.offset(x = (w * x(windowStart) - 18.dp).coerceAtLeast(0.dp)))
            if (span < 20 * 60 * 60_000L) Text(
                timeLabel(windowEnd), style = Safar.type.caption, color = c.contentTertiary,
                modifier = Modifier.offset(x = (w * x(windowEnd) - 30.dp).coerceIn(0.dp, w - 56.dp)),
            )
        }
    }
}

/** A distance set large, unit small: "200 m". The pickup leads every match. */
@Composable
fun BigDistance(meters: Int, modifier: Modifier = Modifier, size: Int = 34, color: Color = Safar.colors.content) {
    val label = distanceLabel(meters)
    val number = label.substringBefore(' ')
    val unit = label.substringAfter(' ')
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = size.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)) { append(number) }
            withStyle(SpanStyle(fontSize = (size * 0.45).sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
        },
        style = Safar.type.title.copy(fontFeatureSettings = "tnum"), color = color, modifier = modifier,
    )
}

/** The pickup ring used across these screens: a fat ring with a soft halo. */
@Composable
fun PickupRing(size: Dp = 22.dp) {
    val c = Safar.colors
    Box(Modifier.size(size * 2.6f).clip(CircleShape).background(c.content.copy(alpha = 0.09f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).clip(CircleShape).background(c.background).border(size * 0.32f, c.content, CircleShape))
    }
}

/**
 * The map these screens share: muted style, lite mode, framed on everything shown. Draws
 * an optional route, the rider's pickup with its matching zone, and small markers.
 */
@Composable
fun WantedMap(
    modifier: Modifier = Modifier,
    pickup: LatLng? = null,
    zoneMeters: Double? = null,
    route: List<LatLng> = emptyList(),
    dots: List<LatLng> = emptyList(),
    people: List<Pair<LatLng, String>> = emptyList(),
    end: LatLng? = null,
    padding: PaddingValues = PaddingValues(top = 72.dp, bottom = 40.dp, start = 32.dp, end = 32.dp),
) {
    val c = Safar.colors
    val context = LocalContext.current
    val density = LocalDensity.current
    val fallback = LatLng(33.6844, 73.0479) // Islamabad
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(pickup ?: route.firstOrNull() ?: fallback, 13f) }
    val all = remember(pickup, route, dots, people, end) {
        buildList { pickup?.let(::add); addAll(route); addAll(dots); addAll(people.map { it.first }); end?.let(::add) }
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        contentPadding = padding,
        properties = MapProperties(mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, if (c.isDark) R.raw.map_style_dark else R.raw.map_style_light)),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false, myLocationButtonEnabled = false),
    ) {
        MapEffect(all.size, all.firstOrNull(), zoneMeters) {
            if (all.size >= 2) {
                val bounds = LatLngBounds.builder().apply { all.forEach(::include) }.build()
                runCatching { camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 60)) }
            } else if (all.size == 1) {
                val zoom = when {
                    zoneMeters == null -> 14f
                    zoneMeters > 5000 -> 10f
                    else -> 13.4f
                }
                camera.move(CameraUpdateFactory.newLatLngZoom(all.first(), zoom))
            }
        }
        if (route.size >= 2) Polyline(route, color = c.content, width = with(density) { 4.dp.toPx() })
        if (pickup != null && zoneMeters != null) Circle(
            center = pickup, radius = zoneMeters,
            fillColor = c.content.copy(alpha = 0.07f), strokeColor = c.content.copy(alpha = 0.28f),
            strokeWidth = with(density) { 1.5.dp.toPx() },
        )
        dots.forEachIndexed { i, p ->
            MarkerComposable("dot$i", c.isDark, i, state = remember(p) { MarkerState(p) }, anchor = Offset(0.5f, 0.5f)) {
                Box(Modifier.size(if (i == 0) 12.dp else 9.dp).clip(CircleShape).background(if (i == 0) c.content else c.contentTertiary))
            }
        }
        end?.let {
            MarkerComposable("end", c.isDark, state = remember(it) { MarkerState(it) }, anchor = Offset(0.5f, 0.5f)) {
                Box(Modifier.size(15.dp).clip(RoundedCornerShape(4.dp)).background(c.content).border(2.dp, c.background, RoundedCornerShape(4.dp)))
            }
        }
        people.forEachIndexed { i, (p, name) ->
            MarkerComposable("person$i", c.isDark, name, state = remember(p) { MarkerState(p) }, anchor = Offset(0.5f, 0.5f)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.surfaceMuted).border(3.dp, c.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(initials(name), style = Safar.type.label.copy(fontWeight = FontWeight.SemiBold), color = c.content) }
            }
        }
        pickup?.let {
            MarkerComposable("pickup", c.isDark, state = remember(it) { MarkerState(it) }, anchor = Offset(0.5f, 0.5f)) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(c.background).border(7.dp, c.content, CircleShape))
            }
        }
    }
}

/** Status line with a small coloured dot: the only colour these screens use. */
@Composable
fun StatusLine(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text("  ${text.uppercase()}", style = Safar.type.overline, color = Safar.colors.content)
    }
}

@Composable
fun FullBleed(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) { content() }
}
