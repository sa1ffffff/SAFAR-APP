package com.safar.app.ui.components

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ride.isRepeating) Icon(Icons.Rounded.Repeat, "Repeats weekly", Modifier.padding(end = 6.dp).size(14.dp), tint = c.contentSecondary)
                Text(departureLabel(ride.departureTime).uppercase(), style = Safar.type.overline, color = c.contentSecondary, modifier = Modifier.weight(1f))
                Text(
                    buildAnnotatedString {
                        append(priceLabel(ride.pricePerSeat))
                        if (ride.pricePerSeat > 0) withStyle(SpanStyle(color = c.contentSecondary, fontSize = Safar.type.caption.fontSize)) { append(" /seat") }
                    },
                    style = Safar.type.headline, color = c.content,
                )
            }
            RouteStops(shortPlace(ride.origin.name), shortPlace(ride.destination.name))
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
