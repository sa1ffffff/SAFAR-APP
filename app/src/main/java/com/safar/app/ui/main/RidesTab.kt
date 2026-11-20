package com.safar.app.ui.main

import androidx.compose.material.icons.rounded.EmojiPeople
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.theme.Radii
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
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
    /** Open rides of each kind matching the search, for the Local / City to city tiles. */
    val kindCounts: Map<com.safar.app.data.model.RouteKind, Int> = emptyMap(),
    val mine: List<Ride>? = null,
    val pendingByRide: Map<String, Int> = emptyMap(),
    val bookings: List<Booking>? = null,
    val ratings: Map<String, RatingSummary> = emptyMap(),
    /** The rider's active wanted rides, with their matches and pending offers. */
    val wanted: List<com.safar.app.data.model.WantedRide> = emptyList(),
    val wantedMatches: Map<String, List<com.safar.app.data.model.WantedMatch>> = emptyMap(),
    val offers: List<com.safar.app.data.model.SeatOffer> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RidesViewModel(private val app: AppContainer) : SafarViewModel() {
    val query = MutableStateFlow("")
    /** null shows every ride; otherwise only local or only city-to-city rides. */
    val kindFilter = MutableStateFlow<com.safar.app.data.model.RouteKind?>(null)
    private val uid = app.session.uid.orEmpty()

    private val open = app.rides.observeOpenRides().catch { emit(emptyList()) }

    private val searched = combine(open, query) { list, q ->
        val term = q.trim()
        list.filter { it.creatorUid != uid }.filter {
            term.isEmpty() || it.destination.name.contains(term, true) || it.origin.name.contains(term, true) ||
                it.originCity.contains(term, true) || it.destinationCity.contains(term, true)
        }
    }

    /** The visible rides, plus how many of each kind the search matches (the tiles show these). */
    private val found = combine(searched, kindFilter) { list, k ->
        list.filter { k == null || it.routeKind == k } to list.groupingBy { it.routeKind }.eachCount()
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

    private val wanted = combine(
        app.wanted.observeMine(uid).catch { emit(emptyList()) },
        app.wanted.observeAllMatches(uid).catch { emit(emptyMap()) },
        app.wanted.observeOffers(uid).catch { emit(emptyList()) },
    ) { w, m, o -> Triple(w.filter { it.isActive }, m, o.filter { it.status == com.safar.app.data.model.OfferStatus.PENDING }) }

    private val core = combine(found, mine, pending, bookings, ratings) { f, m, p, b, r ->
        RidesState(found = f.first, kindCounts = f.second, mine = m, pendingByRide = p, bookings = b, ratings = r)
    }

    val state: StateFlow<RidesState> = combine(core, wanted) { s, (w, m, o) -> s.copy(wanted = w, wantedMatches = m, offers = o) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RidesState())
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
                        KindTiles(kind, state.kindCounts, loading = state.found == null) { vm.kindFilter.value = it }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            when (segment) {
                0 -> { rideList(
                    rides = state.found,
                    empty = {
                        val kind = vm.kindFilter.collectAsStateWithLifecycle().value
                        NoRideYet(
                            when {
                                query.isNotBlank() -> "No ride to ${query.trim()} yet."
                                kind == com.safar.app.data.model.RouteKind.LOCAL -> "No local ride yet."
                                kind != null -> "No city-to-city ride yet."
                                else -> "No ride yet."
                            },
                        ) { nav.navigate(Routes.WANT_NEW) }
                    },
                ) { ride ->
                    RideCard(ride, { nav.navigate(Routes.ride(ride.id)) }, rating = state.ratings[ride.creatorUid])
                }
                }
                1 -> {
                    item(key = "looking") {
                        LookingFor(state, onOpen = { nav.navigate(Routes.want(it)) }, onNew = { nav.navigate(Routes.WANT_NEW) })
                    }
                    if (state.wanted.isNotEmpty() && !state.mine.isNullOrEmpty()) item(key = "offering") {
                        Text("OFFERING", style = Safar.type.overline, color = c.contentSecondary, modifier = Modifier.padding(top = 14.dp))
                    }
                    rideList(
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

        // The two things people come here to do, side by side: ask for a seat, or offer one.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = TabBarSpace + 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.weight(1f).height(52.dp).clip(CircleShape).background(c.primary)
                    .pressable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = { nav.navigate(Routes.WANT_NEW) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Rounded.EmojiPeople, null, Modifier.size(20.dp), tint = c.onPrimary)
                Text("Request a ride", style = Safar.type.bodyStrong, color = c.onPrimary)
            }
            Glass(Modifier.weight(1f), shape = CircleShape) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .pressable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = { nav.navigate(Routes.post()) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = c.content)
                    Text("Offer a ride", style = Safar.type.bodyStrong, color = c.content)
                }
            }
        }
    }
}

/** Find's empty state: nothing posted, so offer to look out for it instead. */
@Composable
private fun NoRideYet(title: String, onNotify: () -> Unit) {
    val c = Safar.colors
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)) {
        // A drawn route: a big pickup ring, a dotted way, a small destination.
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val w = size.width
            val h = size.height
            val a = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.78f)
            val b = androidx.compose.ui.geometry.Offset(w * 0.80f, h * 0.18f)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(a.x, a.y)
                cubicTo(w * 0.42f, h * 0.80f, w * 0.40f, h * 0.30f, b.x, b.y)
            }
            drawPath(
                path, c.content,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1f, 9.dp.toPx())),
                ),
            )
            drawCircle(c.content.copy(alpha = 0.08f), 34.dp.toPx(), a)
            drawCircle(c.background, 10.dp.toPx(), a)
            drawCircle(c.content, 10.dp.toPx(), a, style = androidx.compose.ui.graphics.drawscope.Stroke(6.dp.toPx()))
            val s = 13.dp.toPx()
            drawRoundRect(c.content, androidx.compose.ui.geometry.Offset(b.x - s / 2, b.y - s / 2), androidx.compose.ui.geometry.Size(s, s), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
        Text(title, style = Safar.type.title, color = c.content)
        Text("We'll find one for you.", style = Safar.type.body, color = c.contentSecondary)
        Spacer(Modifier.height(20.dp))
        SafarButton("Notify me", onNotify, modifier = Modifier.fillMaxWidth())
    }
}

/** My Rides' top: the rider's wanted rides, or one quiet line inviting them to make one. */
@Composable
private fun LookingFor(state: RidesState, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val c = Safar.colors
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LOOKING FOR", style = Safar.type.overline, color = c.contentSecondary, modifier = Modifier.weight(1f))
            Text(
                if (state.wanted.isEmpty()) "Get matched ›" else "New ›", style = Safar.type.label, color = c.content,
                modifier = Modifier.clip(RoundedCornerShape(Radii.sm)).clickable(onClick = onNew).padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }
        if (state.wanted.isEmpty()) {
            Text("Need a ride? Ask once and Safar matches you as rides are posted.", style = Safar.type.label, color = c.contentTertiary)
        }
        state.wanted.forEachIndexed { i, w ->
            if (i > 0) com.safar.app.ui.components.Divider()
            com.safar.app.ui.wanted.WantedRow(
                w, state.wantedMatches[w.id].orEmpty(), state.offers.firstOrNull { it.wantId == w.id },
            ) { onOpen(w.id) }
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

/**
 * Local and City to city as two choice tiles: the two trips people take with Safar, side by
 * side. Each says what it means and how many rides are open. Neither selected shows every
 * ride; tapping the selected tile again clears it, so there is no separate "All".
 */
@Composable
private fun KindTiles(
    selected: com.safar.app.data.model.RouteKind?,
    counts: Map<com.safar.app.data.model.RouteKind, Int>,
    loading: Boolean,
    onSelect: (com.safar.app.data.model.RouteKind?) -> Unit,
) {
    val haptics = com.safar.app.ui.haptics()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple(com.safar.app.data.model.RouteKind.LOCAL, "Across town", "Local"),
            Triple(com.safar.app.data.model.RouteKind.CITY_TO_CITY, "Between cities", "City to city"),
        ).forEach { (kind, meaning, title) ->
            KindTile(
                kind = kind, title = title, meaning = meaning,
                count = if (loading) null else counts[kind] ?: 0,
                selected = selected == kind,
                modifier = Modifier.weight(1f),
            ) {
                haptics.tick()
                onSelect(if (selected == kind) null else kind)
            }
        }
    }
}

@Composable
private fun KindTile(
    kind: com.safar.app.data.model.RouteKind,
    title: String,
    meaning: String,
    count: Int?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Safar.colors
    val spec = androidx.compose.animation.core.tween<androidx.compose.ui.graphics.Color>(180)
    val bg by androidx.compose.animation.animateColorAsState(if (selected) c.primary else c.surfaceMuted, spec, label = "tile-bg")
    val fg by androidx.compose.animation.animateColorAsState(if (selected) c.onPrimary else c.content, spec, label = "tile-fg")
    val sub by androidx.compose.animation.animateColorAsState(
        if (selected) c.onPrimary.copy(alpha = 0.72f) else c.contentSecondary, spec, label = "tile-sub",
    )
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(com.safar.app.ui.theme.Radii.lg)
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .pressable(
                remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onClick, role = androidx.compose.ui.semantics.Role.Tab, pressedScale = 0.97f,
            )
            .semantics { this.selected = selected; stateDescription = if (selected) "Showing only $title rides" else "" }
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            com.safar.app.ui.components.RouteKindMark(kind, Modifier.size(26.dp), color = fg)
            Spacer(Modifier.weight(1f))
            androidx.compose.animation.AnimatedContent(
                targetState = count,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100))
                },
                label = "tile-count",
            ) { n ->
                Text(
                    when (n) { null -> " "; 0 -> "None"; 1 -> "1 ride"; else -> "$n rides" },
                    style = Safar.type.caption.copy(fontFeatureSettings = "tnum"), color = sub,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = Safar.type.bodyStrong, color = fg, maxLines = 1)
        Text(meaning, style = Safar.type.caption, color = sub, maxLines = 1)
    }
}
