package com.safar.app.ui.components

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

@Composable
fun SeatsBadge(ride: Ride) {
    val c = Safar.colors
    when {
        ride.status == RideStatus.CANCELLED -> Pill("Cancelled", background = c.negativeMuted, color = c.negative)
        ride.status == RideStatus.COMPLETED -> Pill("Completed")
        ride.status == RideStatus.FILLED || ride.availableSeats == 0 -> Pill("Full")
        ride.isExpired() -> Pill("Expired")
        else -> Pill(
            if (ride.availableSeats == 1) "1 seat left" else "${ride.availableSeats} seats left",
            background = c.positiveMuted, color = c.positive, icon = Icons.Rounded.AirlineSeatReclineNormal,
        )
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
