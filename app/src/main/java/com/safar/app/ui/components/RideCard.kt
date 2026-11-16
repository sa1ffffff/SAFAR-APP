package com.safar.app.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.contentDescription
import com.safar.app.data.model.RouteKind
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirlineSeatReclineNormal
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.Ride
import com.safar.app.data.model.RideStatus
import com.safar.app.ui.departureLabel
import com.safar.app.ui.priceLabel
import com.safar.app.ui.shortPlace
import com.safar.app.ui.theme.Safar

@Composable
fun RideCard(
    ride: Ride,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: RatingSummary? = null,
    showDriver: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
) {
    val c = Safar.colors
    Card(modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val kind = ride.routeKind
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteKindMark(kind, Modifier.padding(end = 8.dp).size(14.dp))
                Text(
                    "${kind.label} · ${departureLabel(ride.departureTime)}".uppercase(),
                    style = Safar.type.overline, color = c.contentSecondary, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (ride.isRepeating) Icon(Icons.Rounded.Repeat, "Repeats weekly", Modifier.padding(end = 8.dp).size(14.dp), tint = c.contentSecondary)
                Text(
                    buildAnnotatedString {
                        append(priceLabel(ride.pricePerSeat))
                        if (ride.pricePerSeat > 0) withStyle(SpanStyle(color = c.contentSecondary, fontSize = Safar.type.caption.fontSize)) { append(" /seat") }
                    },
                    style = Safar.type.headline, color = c.content,
                )
            }
            if (kind == RouteKind.CITY_TO_CITY) CityToCityRoute(ride)
            else RouteStops(shortPlace(ride.origin.name), shortPlace(ride.destination.name))
            if (showDriver || footer != null) {
                Divider()
                if (footer != null) footer() else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(ride.creatorName, ride.creatorPhotoUrl, size = 30.dp)
                    Text(ride.creatorName.substringBefore(' '), style = Safar.type.label, color = c.content)
                    if (rating != null) RatingLabel(rating.average, rating.count)
                    Spacer(Modifier.weight(1f))
                    SeatsBadge(ride)
                }
            }
        }
    }
}

/**
 * A ride's availability, said quietly: a small seat meter and plain text, no pill.
 * [long] reads "2 of 4 seats left" (ride page); otherwise "2 left" (cards).
 */
@Composable
fun SeatsBadge(ride: Ride, long: Boolean = false) {
    val c = Safar.colors
    val total = maxOf(ride.totalSeats, ride.availableSeats)
    val (text, color, meter) = when {
        ride.status == RideStatus.CANCELLED -> Triple("Cancelled", c.negative, false)
        ride.status == RideStatus.COMPLETED -> Triple("Completed", c.contentSecondary, false)
        ride.status == RideStatus.STARTED -> Triple("On the way", c.content, false)
        ride.isExpired() -> Triple("Expired", c.contentTertiary, false)
        ride.status == RideStatus.FILLED || ride.availableSeats == 0 -> Triple("Full", c.contentSecondary, true)
        long -> Triple("${ride.availableSeats} of $total ${if (total == 1) "seat" else "seats"} left", c.contentSecondary, true)
        else -> Triple("${ride.availableSeats} left", c.contentSecondary, true)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (meter && total in 1..8) SeatMeter(free = ride.availableSeats, total = total)
        if (ride.status == RideStatus.STARTED) Box(Modifier.size(6.dp).clip(CircleShape).background(c.content))
        Text(text, style = Safar.type.caption, color = color)
    }
}

/** One short bar per seat; filled bars are the seats still free. */
@Composable
fun SeatMeter(free: Int, total: Int, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Row(modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            Box(Modifier.size(width = 10.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < free) c.content else c.border))
        }
    }
}

@Composable
fun RideCardSkeleton(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Skeleton(Modifier.fillMaxWidth(0.4f).height(12.dp))
            Skeleton(Modifier.fillMaxWidth(0.75f).height(16.dp))
            Skeleton(Modifier.fillMaxWidth(0.6f).height(16.dp))
            Skeleton(Modifier.fillMaxWidth().height(28.dp).padding(top = 4.dp))
        }
    }
}

/**
 * Route-type mark, drawn rather than taken from an icon set.
 * Local: a 3×3 sector grid with the centre block filled, like Islamabad's sectors.
 * City to city: two cities joined by a dashed arc, like a route on a road atlas.
 */
@Composable
fun RouteKindMark(kind: RouteKind, modifier: Modifier = Modifier) {
    val ink = Safar.colors.content
    val faint = Safar.colors.contentTertiary
    androidx.compose.foundation.Canvas(modifier.semantics { contentDescription = kind.label }) {
        val s = size.minDimension
        if (kind == RouteKind.LOCAL) {
            val gap = s * 0.14f
            val cell = (s - gap * 2) / 3
            for (row in 0..2) for (col in 0..2) {
                val center = row == 1 && col == 1
                drawRoundRect(
                    color = if (center) ink else faint,
                    topLeft = androidx.compose.ui.geometry.Offset(col * (cell + gap), row * (cell + gap)),
                    size = androidx.compose.ui.geometry.Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.28f),
                    style = if (center) androidx.compose.ui.graphics.drawscope.Fill
                    else androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.07f),
                )
            }
        } else {
            val r = s * 0.15f
            val a = androidx.compose.ui.geometry.Offset(r, s - r)
            val b = androidx.compose.ui.geometry.Offset(s - r, r)
            val arc = androidx.compose.ui.graphics.Path().apply {
                moveTo(a.x, a.y)
                quadraticTo(r * 0.6f, r * 0.6f, b.x, b.y)
            }
            drawPath(
                arc, color = ink,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = s * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(s * 0.13f, s * 0.11f)),
                ),
            )
            drawCircle(color = ink, radius = r, center = a)
            drawCircle(color = ink, radius = r, center = b, style = androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.09f))
        }
    }
}

/**
 * City to city at a glance: the two cities large, joined by a quiet dashed line, with the
 * pickup and drop-off areas beneath each.
 */
@Composable
fun CityToCityRoute(ride: Ride, modifier: Modifier = Modifier) {
    val c = Safar.colors
    val from = ride.originCity.ifBlank { com.safar.app.data.model.cityFromName(ride.origin.name) }.ifBlank { shortPlace(ride.origin.name) }
    val to = ride.destinationCity.ifBlank { com.safar.app.data.model.cityFromName(ride.destination.name) }.ifBlank { shortPlace(ride.destination.name) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(from, style = Safar.type.title, color = c.content, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(shortPlace(ride.origin.name), style = Safar.type.caption, color = c.contentSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        androidx.compose.foundation.Canvas(Modifier.weight(1f).widthIn(min = 20.dp).height(28.dp)) {
            val y = size.height * 0.5f
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
            drawLine(c.border, androidx.compose.ui.geometry.Offset(4.dp.toPx(), y), androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), y),
                strokeWidth = 1.5.dp.toPx(), pathEffect = dash, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawCircle(c.contentTertiary, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(2.5.dp.toPx(), y))
            drawCircle(c.content, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width - 2.5.dp.toPx(), y))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(to, style = Safar.type.title, color = c.content, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(shortPlace(ride.destination.name), style = Safar.type.caption, color = c.contentSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}
