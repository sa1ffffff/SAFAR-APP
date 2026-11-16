package com.safar.app.ui.main

import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.JoinRequest
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.RequestStatus
import com.safar.app.data.model.Ride
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.components.Card
import com.safar.app.ui.components.EmptyState
import com.safar.app.ui.components.Field
import com.safar.app.ui.components.Glass
import com.safar.app.ui.components.IconCircle
import com.safar.app.ui.components.Pill
import com.safar.app.ui.components.RideCard
import com.safar.app.ui.components.RideCardSkeleton
import com.safar.app.ui.components.RouteStops
import com.safar.app.ui.components.SeatsBadge
import com.safar.app.ui.components.Segmented
import com.safar.app.ui.components.pressable
import com.safar.app.data.seatsLabel
import com.safar.app.ui.departureLabel
import com.safar.app.ui.shortPlace
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Booking(val request: JoinRequest, val ride: Ride?)

data class RidesState(
    val found: List<Ride>? = null,
    val mine: List<Ride>? = null,
    val pendingByRide: Map<String, Int> = emptyMap(),
    val bookings: List<Booking>? = null,
    val ratings: Map<String, RatingSummary> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RidesViewModel(private val app: AppContainer) : SafarViewModel() {
    val query = MutableStateFlow("")
    /** null shows every ride; otherwise only local or only city-to-city rides. */
    val kindFilter = MutableStateFlow<com.safar.app.data.model.RouteKind?>(null)
    private val uid = app.session.uid.orEmpty()

    private val open = app.rides.observeOpenRides().catch { emit(emptyList()) }

    private val found = combine(open, query, kindFilter) { list, q, k ->
        val term = q.trim()
        list.filter { it.creatorUid != uid }.filter { k == null || it.routeKind == k }.filter {
            term.isEmpty() || it.destination.name.contains(term, true) || it.origin.name.contains(term, true) ||
                it.originCity.contains(term, true) || it.destinationCity.contains(term, true)
        }
    }

    /** Upcoming rides soonest first, then past ones, most recent first. */
    private val mine = app.rides.observeRidesBy(uid).catch { emit(emptyList()) }.map { list ->
        val (upcoming, past) = list.partition { !it.isExpired() && it.status != com.safar.app.data.model.RideStatus.CANCELLED }
        upcoming.sortedBy { it.departureTime } + past.sortedByDescending { it.departureTime }
    }

    private val pending = app.requests.observeIncoming(uid)
        .map { list -> list.filter { it.status == RequestStatus.PENDING }.groupingBy { it.adId }.eachCount() }
        .catch { emit(emptyMap()) }

    private val bookings: Flow<List<Booking>> = app.requests.observeOutgoing(uid)
        .catch { emit(emptyList()) }
        .flatMapLatest { requests ->
            val live = requests.filter { it.status != RequestStatus.CANCELLED }
            if (live.isEmpty()) flowOf(emptyList())
            else combine(live.map { r -> app.rides.observe(r.adId).catch { emit(null) }.map { Booking(r, it) } }) { all ->
                // A booking whose ride the driver deleted is gone entirely, not shown as "removed".
                all.filter { it.ride != null }.sortedBy { it.ride?.departureTime ?: Long.MAX_VALUE }
            }
        }

    private val ratings = open.map { list -> list.map { it.creatorUid }.toSet() }.distinctUntilChanged()
        .flatMapLatest { app.users.observeRatings(it).catch { emit(emptyMap()) } }

    val state: StateFlow<RidesState> = combine(found, mine, pending, bookings, ratings) { f, m, p, b, r ->
        RidesState(f, m, p, b, r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RidesState())
}

@Composable
fun RidesTab(nav: NavHostController, focusSearch: Boolean, onSearchFocused: () -> Unit) {
    val vm = appViewModel { RidesViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) { segment = 0; focus.requestFocus(); onSearchFocused() }
    }
    val bottom = TabBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 72.dp

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 20.dp)) {
                    Text("Rides", style = Safar.type.display, color = c.content)
                    Spacer(Modifier.height(16.dp))
                    Segmented(listOf("Find", "My Rides", "Booked"), segment, { segment = it })
                    if (segment == 0) {
                        Spacer(Modifier.height(12.dp))
                        Field(
                            value = query, onValueChange = { vm.query.value = it }, label = "Search",
                            placeholder = "Destination or pickup area",
                            modifier = Modifier.focusRequester(focus),
                            leading = { Icon(Icons.Rounded.Search, null, tint = c.contentSecondary) },
                            trailing = if (query.isNotEmpty()) ({
                                IconCircle(Icons.Rounded.Close, "Clear", { vm.query.value = "" }, size = 32.dp)
                            }) else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                        Spacer(Modifier.height(14.dp))
                        val kind by vm.kindFilter.collectAsStateWithLifecycle()
                        KindFilter(kind) { vm.kindFilter.value = it }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            when (segment) {
                0 -> rideList(
                    rides = state.found,
                    empty = {
                        val kind = vm.kindFilter.collectAsStateWithLifecycle().value
                        if (query.isBlank() && kind != null) EmptyState(
                            Icons.Rounded.DirectionsCar,
                            if (kind == com.safar.app.data.model.RouteKind.LOCAL) "No local rides right now" else "No city-to-city rides right now",
                            "Check back soon, or offer one yourself.",
                        )
                        else if (query.isBlank()) EmptyState(Icons.Rounded.DirectionsCar, "No open rides", "Check back soon, or offer a ride of your own.")
                        else EmptyState(
                            Icons.Rounded.SearchOff, "No rides match “${query.trim()}”",
                            "No one has posted a ride from or to there yet. Try a nearby area, or offer the ride yourself.",
                        )
                    },
                ) { ride ->
                    RideCard(ride, { nav.navigate(Routes.ride(ride.id)) }, rating = state.ratings[ride.creatorUid])
                }
                1 -> rideList(
                    rides = state.mine,
                    empty = {
                        EmptyState(
                            Icons.Rounded.DirectionsCar, "You haven't offered a ride",
                            "Driving somewhere? Post it and riders heading the same way can request a seat.",
                            action = "Offer a ride", onAction = { nav.navigate(Routes.post()) },
                        )
                    },
                ) { ride ->
                    val pending = state.pendingByRide[ride.id] ?: 0
                    RideCard(ride, { nav.navigate(Routes.ride(ride.id)) }, showDriver = false, footer = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (pending > 0) Pill(if (pending == 1) "1 new request" else "$pending new requests", background = c.primary, color = c.onPrimary)
                            else Text("No pending requests", style = Safar.type.label, color = c.contentSecondary)
                            Spacer(Modifier.weight(1f))
                            SeatsBadge(ride)
                        }
                    })
                }
                else -> {
                    val bookings = state.bookings
                    if (bookings == null) items(2) { RideCardSkeleton() }
                    else if (bookings.isEmpty()) item {
                        EmptyState(Icons.Rounded.EventSeat, "No seats booked", "Requests you send to drivers show up here with their status.", action = "Find a ride", onAction = { segment = 0 })
                    } else items(bookings, key = { it.request.id }) { b -> BookingCard(b) { nav.navigate(Routes.ride(b.request.adId)) } }
                }
            }
        }

        Glass(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = TabBarSpace + 4.dp),
            shape = CircleShape,
        ) {
            Row(
                Modifier.pressable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = { nav.navigate(Routes.post()) })
                    .padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = c.content)
                Text("Offer a ride", style = Safar.type.bodyStrong, color = c.content)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rideList(
    rides: List<Ride>?,
    empty: @Composable () -> Unit,
    row: @Composable (Ride) -> Unit,
) {
    when {
        rides == null -> items(3) { RideCardSkeleton() }
        rides.isEmpty() -> item { empty() }
        else -> items(rides, key = { it.id }) { row(it) }
    }
}

@Composable
private fun BookingCard(b: Booking, onClick: () -> Unit) {
    val c = Safar.colors
    val (label, bg, fg) = if (b.ride == null) Triple("Unavailable", c.surfaceMuted, c.contentSecondary) else when (b.request.status) {
        RequestStatus.PENDING -> Triple("Waiting for driver", c.warningMuted, c.warning)
        RequestStatus.ACCEPTED -> if (b.request.isCompleted) Triple("Completed", c.surfaceMuted, c.contentSecondary) else Triple("Confirmed", c.positiveMuted, c.positive)
        RequestStatus.REJECTED -> Triple("Declined", c.negativeMuted, c.negative)
        RequestStatus.CANCELLED -> Triple("Cancelled", c.surfaceMuted, c.contentSecondary)
    }
    Card(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (b.ride?.let { departureLabel(it.departureTime) } ?: "Ride removed").uppercase(),
                    style = Safar.type.overline, color = c.contentSecondary, modifier = Modifier.weight(1f),
                )
                Pill(label, background = bg, color = fg)
            }
            if (b.ride != null) RouteStops(shortPlace(b.ride.origin.name), shortPlace(b.ride.destination.name))
            else Text(com.safar.app.ui.shortRoute(b.request.routeLabel).ifBlank { "This ride is no longer available" }, style = Safar.type.bodyStrong, color = c.content)
            Text(
                "${seatsLabel(b.request.seats)} with ${b.ride?.creatorName?.substringBefore(' ') ?: "driver"}",
                style = Safar.type.label, color = c.contentSecondary,
            )
        }
    }
}

/** All · Local · City to city, as quiet text tabs with the route marks and an underline. */
@Composable
private fun KindFilter(selected: com.safar.app.data.model.RouteKind?, onSelect: (com.safar.app.data.model.RouteKind?) -> Unit) {
    val c = Safar.colors
    val haptics = com.safar.app.ui.haptics()
    val options = listOf<com.safar.app.data.model.RouteKind?>(null, com.safar.app.data.model.RouteKind.LOCAL, com.safar.app.data.model.RouteKind.CITY_TO_CITY)
    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        options.forEach { option ->
            val on = option == selected
            val ink by androidx.compose.animation.animateColorAsState(if (on) c.content else c.contentTertiary, androidx.compose.animation.core.tween(160), label = "kind")
            Column(
                Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                        if (!on) { haptics.tick(); onSelect(option) }
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (option != null) com.safar.app.ui.components.RouteKindMark(option, Modifier.size(13.dp).graphicsLayerAlpha(if (on) 1f else 0.55f))
                    Text(option?.label ?: "All", style = Safar.type.label, color = ink)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.height(2.dp).fillMaxWidth().clip(CircleShape).background(if (on) c.content else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}

private fun Modifier.graphicsLayerAlpha(a: Float) = this.then(Modifier.graphicsLayer { alpha = a })
