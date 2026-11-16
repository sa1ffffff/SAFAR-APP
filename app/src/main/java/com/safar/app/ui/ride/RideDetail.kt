package com.safar.app.ui.ride

import android.content.Intent
import android.content.Context
import kotlinx.coroutines.delay
import androidx.compose.runtime.produceState
import com.safar.app.data.model.repeatLabel
import com.safar.app.ui.components.rememberRoadRoute
import com.safar.app.ui.components.routePoints
import com.safar.app.ui.components.summary
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.safar.app.AppContainer
import com.safar.app.R
import com.safar.app.data.model.JoinRequest
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.RequestStatus
import com.safar.app.data.model.Ride
import com.safar.app.data.model.RideStatus
import com.safar.app.data.model.User
import com.safar.app.data.model.isCallablePhone
import com.safar.app.data.seatsLabel
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.BottomActionBar
import com.safar.app.ui.components.ButtonKind
import com.safar.app.ui.components.Card
import com.safar.app.ui.components.EmptyState
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.IconCircle
import com.safar.app.ui.components.Pill
import com.safar.app.ui.components.RatingLabel
import com.safar.app.ui.components.ReportSheet
import com.safar.app.ui.components.RouteMarks
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.SeatsBadge
import com.safar.app.ui.components.Skeleton
import com.safar.app.ui.components.Stepper
import com.safar.app.ui.countdownLabel
import com.safar.app.ui.dayLabel
import com.safar.app.ui.main.RequestCard
import com.safar.app.ui.priceLabel
import com.safar.app.ui.shortPlace
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import com.safar.app.ui.timeLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

data class RideDetailState(
    val loading: Boolean = true,
    val ride: Ride? = null,
    val driver: User? = null,
    val driverRating: RatingSummary = RatingSummary(),
    val isOwner: Boolean = false,
    val myRequest: JoinRequest? = null,
    val requests: List<JoinRequest> = emptyList(),
    val busy: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RideDetailViewModel(private val app: AppContainer, private val rideId: String) : SafarViewModel() {
    private val uid = app.session.uid.orEmpty()
    private val busy = MutableStateFlow(false)

    private val ride = app.rides.observe(rideId).catch { emit(null) }
    private val creator = ride.map { it?.creatorUid }.distinctUntilChanged()

    private val driver = creator.flatMapLatest { if (it.isNullOrBlank()) flowOf(null) else app.users.observe(it).catch { emit(null) } }
    private val rating = creator.flatMapLatest { if (it.isNullOrBlank()) flowOf(RatingSummary()) else app.users.observeRating(it).catch { emit(RatingSummary()) } }

    private val requests = creator.flatMapLatest { owner ->
        when {
            owner == null -> flowOf(emptyList())
            owner == uid -> app.requests.observeIncoming(uid).map { l -> l.filter { it.adId == rideId && it.status != RequestStatus.CANCELLED } }
            else -> app.requests.observeOutgoing(uid).map { l -> l.filter { it.adId == rideId } }
        }.catch { emit(emptyList()) }
    }

    val state: StateFlow<RideDetailState> = combine(ride, driver, rating, requests, busy) { r, d, rt, reqs, b ->
        val owner = r?.creatorUid == uid
        RideDetailState(
            loading = false, ride = r, driver = d, driverRating = rt, isOwner = owner,
            myRequest = if (owner) null else reqs.filter { it.status != RequestStatus.CANCELLED }.maxByOrNull { it.timestamp },
            requests = if (owner) reqs else emptyList(),
            busy = b,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RideDetailState())

    private fun run(block: suspend () -> Unit, done: String? = null) {
        busy.value = true
        action(onDone = { busy.value = false; done?.let { cheer(big = it.startsWith("Ride started")); say(it) } }, onError = { busy.value = false }) { block() }
    }

    private val me get() = app.session.requireUser()

    fun request(seats: Int) = state.value.ride?.let { r -> run({ app.requests.send(r, me, seats) }, "Request sent. We'll let you know when they reply.") }
    fun cancelRequest() = state.value.myRequest?.let { r -> run({ app.requests.cancel(r) }, "Request cancelled") }
    fun accept(r: JoinRequest) = run({ app.requests.accept(r, me) }, "${r.requestorName.substringBefore(' ').ifBlank { "Rider" }} is confirmed")
    fun decline(r: JoinRequest) = run({ app.requests.decline(r, me) })
    fun markComplete(r: JoinRequest) = run({ app.requests.markComplete(r, me) }, "Marked as complete")
    fun setStatus(status: RideStatus) = run({ app.rides.setStatus(rideId, status) })

    /** Marks the ride started and tells every confirmed rider. */
    fun start() = state.value.ride?.let { ride ->
        val riders = state.value.requests.filter { it.status == RequestStatus.ACCEPTED }
        run({
            app.rides.start(ride.id)
            riders.forEach { r ->
                app.notifications.send(
                    to = r.requestorUid, from = me, type = com.safar.app.data.model.NotificationType.RIDE_STARTED,
                    title = "Your ride has started",
                    message = "${me.firstName} is on the way: ${shortPlace(ride.origin.name)} → ${shortPlace(ride.destination.name)}",
                    adId = ride.id, requestId = r.id,
                )
            }
        }, "Ride started. Drive safe!")
    }

    fun stopRepeating() = state.value.ride?.let { ride ->
        run({ app.rides.stopRepeating(ride.seriesId, ride.creatorUid, keepId = ride.id) }, "Stopped repeating. This ride stays posted.")
    }
    fun delete(onDeleted: () -> Unit) {
        busy.value = true
        action(onDone = onDeleted, onError = { busy.value = false }) { app.rides.delete(rideId) }
    }
    fun report(reason: String, details: String) = state.value.ride?.let { r ->
        run({ app.reports.report(uid, r.creatorUid, r.id, reason, details) }, "Thanks. We'll look into it.")
    }
}

@Composable
fun RideDetailScreen(rideId: String, nav: NavHostController) {
    val vm = appViewModel(key = "ride-$rideId") { RideDetailViewModel(it, rideId) }
    Messages(vm)
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Safar.colors
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var seats by remember { mutableIntStateOf(1) }
    val ride = state.ride

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                if (ride != null) RouteMap(ride) else Box(Modifier.fillMaxSize().background(c.surfaceMuted))
            }
            Column(
                Modifier.offset(y = (-28).dp).clip(RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl))
                    .background(c.background).padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when {
                    state.loading -> DetailSkeleton()
                    ride == null -> EmptyState(Icons.Rounded.DirectionsCar, "This ride is gone", "The driver may have removed it, or it expired.", action = "Back", onAction = { nav.back() })
                    else -> RideBody(ride, state, vm, nav)
                }
                Spacer(Modifier.height(96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }

        // Floating header controls over the map.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() })
            Spacer(Modifier.weight(1f))
            if (ride != null) {
                GlassIconButton(Icons.Rounded.Share, "Share", {
                    val text = "Ride on Safar: ${shortPlace(ride.origin.name)} → ${shortPlace(ride.destination.name)}, ${dayLabel(ride.departureTime)} at ${timeLabel(ride.departureTime)}. ${seatsLabel(ride.availableSeats)} left."
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share ride"))
                })
                Box {
                    GlassIconButton(Icons.Rounded.MoreHoriz, "More", { menu = true })
                    DropdownMenu(menu, { menu = false }, containerColor = c.surfaceRaised) {
                        if (state.isOwner) {
                            DropdownMenuItem({ Text("Edit ride") }, { menu = false; nav.navigate(Routes.post(ride.id)) })
                            if (ride.status == RideStatus.OPEN && !ride.hasDeparted()) DropdownMenuItem({ Text("Mark as full") }, { menu = false; vm.setStatus(RideStatus.FILLED) })
                            if (ride.status == RideStatus.FILLED && ride.availableSeats > 0) DropdownMenuItem({ Text("Reopen for requests") }, { menu = false; vm.setStatus(RideStatus.OPEN) })
                            if (ride.status != RideStatus.CANCELLED && ride.status != RideStatus.STARTED) DropdownMenuItem({ Text("Cancel ride") }, { menu = false; vm.setStatus(RideStatus.CANCELLED) })
                            if (ride.isRepeating) DropdownMenuItem({ Text("Stop repeating") }, { menu = false; confirmStop = true })
                            DropdownMenuItem({ Text("Delete ride", color = c.negative) }, { menu = false; confirmDelete = true })
                        } else {
                            DropdownMenuItem({ Text("View driver profile") }, { menu = false; nav.navigate(Routes.user(ride.creatorUid)) })
                            DropdownMenuItem({ Text("Report ride", color = c.negative) }, { menu = false; reporting = true })
                        }
                    }
                }
            }
        }

        if (ride != null && !state.loading) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                ActionBar(ride, state, seats, { seats = it }, vm, nav)
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop repeating?", style = Safar.type.headline) },
            text = {
                Text(
                    "No new rides will be posted. Later rides nobody has asked to join are removed; this one and rides with requests stay.",
                    style = Safar.type.body,
                )
            },
            confirmButton = { TextButton({ confirmStop = false; vm.stopRepeating() }) { Text("Stop repeating", color = c.negative) } },
            dismissButton = { TextButton({ confirmStop = false }) { Text("Keep it", color = c.content) } },
            containerColor = c.surfaceRaised,
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this ride?", style = Safar.type.headline) },
            text = { Text("Riders with pending requests will no longer see it. This can't be undone.", style = Safar.type.body) },
            confirmButton = { TextButton({ confirmDelete = false; vm.delete { nav.back() } }) { Text("Delete", color = c.negative) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep it", color = c.content) } },
            containerColor = c.surfaceRaised,
        )
    }
    if (reporting) {
        ReportSheet("Report this ride", onDismiss = { reporting = false }) { reason, details ->
            reporting = false
            vm.report(reason, details)
        }
    }
}

@Composable
private fun RideBody(ride: Ride, state: RideDetailState, vm: RideDetailViewModel, nav: NavHostController) {
    val c = Safar.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${dayLabel(ride.departureTime)} · ${countdownLabel(ride.departureTime)}".uppercase(),
                style = Safar.type.overline, color = c.contentSecondary, modifier = Modifier.weight(1f),
            )
            SeatsBadge(ride, long = true)
        }
        Text("${shortPlace(ride.origin.name)} → ${shortPlace(ride.destination.name)}", style = Safar.type.display, color = c.content)
        if (ride.isRepeating) Row(
            Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Repeat, null, Modifier.size(16.dp), tint = c.contentSecondary)
            Text("Repeats ${repeatLabel(ride.repeatDays).lowercaseFirstWord()} at ${timeLabel(ride.departureTime)}", style = Safar.type.label, color = c.contentSecondary)
        }
        if (ride.status == RideStatus.STARTED) Text(
            "Started at ${timeLabel(ride.startedAt)} · closes at ${timeLabel(ride.expiresAt)}",
            style = Safar.type.label, color = c.positive, modifier = Modifier.padding(top = 4.dp),
        )
    }

    // A rider's own status is the most important thing on this page, so it leads.
    if (!state.isOwner) state.myRequest?.let { RequestStatusCard(it) }

    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RouteMarks(gap = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column {
                    Text(ride.origin.name, style = Safar.type.bodyStrong, color = c.content)
                    Text("${timeLabel(ride.departureTime)} · Pickup", style = Safar.type.label, color = c.contentSecondary)
                }
                Column {
                    Text(ride.destination.name, style = Safar.type.bodyStrong, color = c.content)
                    val road = rememberRoadRoute(ride)
                    Text(if (road != null) "Drop-off · ${road.summary()}" else "Drop-off", style = Safar.type.label, color = c.contentSecondary)
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("Seats left", ride.availableSeats.toString(), Modifier.weight(1f))
        Stat("Per seat", priceLabel(ride.pricePerSeat), Modifier.weight(1f))
        Stat("Car", ride.vehicleDetails.ifBlank { "—" }, Modifier.weight(1f), small = true)
    }

    if (ride.notes.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Notes from ${ride.creatorName.substringBefore(' ')}", style = Safar.type.headline, color = c.content)
            Text(ride.notes, style = Safar.type.body, color = c.contentSecondary)
        }
    }

    if (!state.isOwner) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).clickable { nav.navigate(Routes.user(ride.creatorUid)) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(ride.creatorName, state.driver?.photoUrl ?: ride.creatorPhotoUrl, size = 52.dp)
            Column(Modifier.weight(1f)) {
                Text(ride.creatorName, style = Safar.type.headline, color = c.content)
                RatingLabel(state.driverRating.average, state.driverRating.count)
            }
            val context = LocalContext.current
            val phone = driverPhone(ride, state)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (phone != null) {
                    IconCircle(Icons.Rounded.Phone, "Call ${ride.creatorName.substringBefore(' ')}", { dial(context, phone) })
                    IconCircle(
                        ImageVector.vectorResource(R.drawable.ic_whatsapp), "WhatsApp ${ride.creatorName.substringBefore(' ')}",
                        { openWhatsApp(context, phone, whatsAppGreeting(ride)) },
                    )
                }
                IconCircle(Icons.Rounded.ChatBubbleOutline, "Message in Safar", { nav.navigate(Routes.chat(ride.creatorUid)) })
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Requests", style = Safar.type.headline, color = c.content)
            if (state.requests.isEmpty()) {
                Text("No one has asked for a seat yet. We'll notify you when they do.", style = Safar.type.body, color = c.contentSecondary)
            } else state.requests.forEach { r ->
                RequestCard(
                    r, busy = state.busy,
                    onAccept = { vm.accept(r) }, onDecline = { vm.decline(r) },
                    onMessage = { nav.navigate(Routes.chat(r.requestorUid)) },
                    onProfile = { nav.navigate(Routes.user(r.requestorUid)) },
                    onRide = null,
                )
                if (r.status == RequestStatus.ACCEPTED) CompletionRow(r, isOwner = true, departed = ride.hasDeparted(), vm = vm, nav = nav)
            }
        }
    }
}

@Composable
private fun RequestStatusCard(r: JoinRequest) {
    val c = Safar.colors
    val (title, body, bg, fg) = when (r.status) {
        RequestStatus.PENDING -> Quad("Request sent", "Waiting for the driver to respond.", c.warningMuted, c.warning)
        RequestStatus.ACCEPTED -> if (r.isCompleted) Quad("Ride completed", "Thanks for riding together.", c.surfaceMuted, c.content)
        else Quad("You're confirmed", "${seatsLabel(r.seats)} reserved. Message the driver to sort out pickup.", c.positiveMuted, c.positive)
        RequestStatus.REJECTED -> Quad("Not this time", "The driver couldn't take this request. Try another ride.", c.negativeMuted, c.negative)
        RequestStatus.CANCELLED -> Quad("Cancelled", "You cancelled this request.", c.surfaceMuted, c.contentSecondary)
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).background(bg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = Safar.type.bodyStrong, color = fg)
        Text(body, style = Safar.type.label, color = c.content)
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun CompletionRow(r: JoinRequest, isOwner: Boolean, departed: Boolean, vm: RideDetailViewModel, nav: NavHostController) {
    if (!departed) return
    val iMarked = if (isOwner) r.creatorMarkedComplete else r.requesterMarkedComplete
    val myUid = if (isOwner) r.adCreatorUid else r.requestorUid
    when {
        r.canReview(myUid) -> SafarButton("Rate ${if (isOwner) r.requestorName.substringBefore(' ') else "your driver"}", { nav.navigate(Routes.review(r.id)) }, kind = ButtonKind.Secondary, height = 46.dp, modifier = Modifier.fillMaxWidth())
        !iMarked -> SafarButton("Mark ride complete", { vm.markComplete(r) }, kind = ButtonKind.Ghost, height = 46.dp, modifier = Modifier.fillMaxWidth())
        !r.isCompleted -> Text("Waiting for the other person to confirm the ride.", style = Safar.type.label, color = Safar.colors.contentSecondary)
    }
}

@Composable
private fun ActionBar(
    ride: Ride, state: RideDetailState, seats: Int, onSeats: (Int) -> Unit,
    vm: RideDetailViewModel, nav: NavHostController,
) {
    val c = Safar.colors
    val req = state.myRequest
    val departed = ride.hasDeparted()
    if (state.isOwner) {
        // Re-checked every 30 s, so Start appears on its own as departure gets close.
        val now by produceState(System.currentTimeMillis()) { while (true) { delay(30_000); value = System.currentTimeMillis() } }
        val riders = state.requests.count { it.status == RequestStatus.ACCEPTED }
        // Startable once someone's confirmed and the car is full or it's nearly time to go.
        val canStart = riders > 0 && (ride.status == RideStatus.OPEN || ride.status == RideStatus.FILLED) && !ride.isExpired(now) &&
            (ride.availableSeats == 0 || now >= ride.departureTime - START_WINDOW_MS)
        var confirmStart by remember { mutableStateOf(false) }
        BottomActionBar {
            when {
                ride.status == RideStatus.STARTED -> {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = c.positive)
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text("On the way", style = Safar.type.bodyStrong, color = c.content)
                        Text("Closes at ${timeLabel(ride.expiresAt)}", style = Safar.type.label, color = c.contentSecondary)
                    }
                }
                canStart -> {
                    SafarButton("Edit", { nav.navigate(Routes.post(ride.id)) }, kind = ButtonKind.Secondary)
                    SafarButton(
                        if (riders == 1) "Start ride · 1 rider" else "Start ride · $riders riders",
                        { confirmStart = true }, loading = state.busy, modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    SafarButton("Edit ride", { nav.navigate(Routes.post(ride.id)) }, kind = ButtonKind.Secondary, modifier = Modifier.weight(1f))
                    val pending = state.requests.count { it.status == RequestStatus.PENDING }
                    Text(
                        if (pending > 0) "$pending waiting" else "${seatsLabel(ride.availableSeats)} open",
                        style = Safar.type.label, color = c.contentSecondary, modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
        if (confirmStart) AlertDialog(
            onDismissRequest = { confirmStart = false },
            title = { Text("Start this ride?", style = Safar.type.headline) },
            text = {
                Text(
                    "We'll let your ${if (riders == 1) "rider" else "$riders riders"} know you're on the way. The ride stops taking requests and closes an hour from now.",
                    style = Safar.type.body,
                )
            },
            confirmButton = { TextButton({ confirmStart = false; vm.start() }) { Text("Start ride", color = c.content) } },
            dismissButton = { TextButton({ confirmStart = false }) { Text("Not yet", color = c.contentSecondary) } },
            containerColor = c.surfaceRaised,
        )
        return
    }
    BottomActionBar {
        when {
            req == null && ride.isBookable -> {
                Row(
                    Modifier.clip(RoundedCornerShape(Radii.md)).border(1.dp, c.border, RoundedCornerShape(Radii.md)).padding(horizontal = 6.dp, vertical = 8.dp),
                ) { Stepper(seats, { onSeats(it.coerceIn(1, ride.availableSeats)) }, 1..ride.availableSeats) }
                SafarButton(if (seats == 1) "Request seat" else "Request $seats seats", { vm.request(seats) }, loading = state.busy, modifier = Modifier.weight(1f))
            }
            req == null -> SafarButton("Not taking requests", {}, enabled = false, modifier = Modifier.weight(1f))
            req.status == RequestStatus.PENDING ->
                SafarButton("Cancel request", { vm.cancelRequest() }, kind = ButtonKind.Secondary, loading = state.busy, modifier = Modifier.weight(1f))
            req.status == RequestStatus.ACCEPTED && departed && req.canReview(req.requestorUid) ->
                SafarButton("Rate your driver", { nav.navigate(Routes.review(req.id)) }, modifier = Modifier.weight(1f))
            req.status == RequestStatus.ACCEPTED && departed && !req.requesterMarkedComplete ->
                SafarButton("Mark ride complete", { vm.markComplete(req) }, loading = state.busy, modifier = Modifier.weight(1f))
            req.status == RequestStatus.ACCEPTED -> {
                SafarButton("Message driver", { nav.navigate(Routes.chat(ride.creatorUid)) }, modifier = Modifier.weight(1f))
            }
            else -> SafarButton("Find another ride", { nav.back() }, kind = ButtonKind.Secondary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, small: Boolean = false) {
    val c = Safar.colors
    Column(
        modifier.clip(RoundedCornerShape(Radii.md)).background(c.surfaceMuted).padding(12.dp),
    ) {
        Text(label, style = Safar.type.caption, color = c.contentSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = if (small) Safar.type.bodyStrong else Safar.type.title, color = c.content, maxLines = 2)
    }
}

@Composable
private fun DetailSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Skeleton(Modifier.fillMaxWidth(0.4f).height(12.dp))
        Skeleton(Modifier.fillMaxWidth(0.8f).height(30.dp))
        Skeleton(Modifier.fillMaxWidth().height(110.dp))
        Skeleton(Modifier.fillMaxWidth().height(70.dp))
    }
}

/** Static route preview: lite-mode map framed on pickup and drop-off. */
@Composable
fun RouteMap(ride: Ride) {
    val c = Safar.colors
    val context = LocalContext.current
    val density = LocalDensity.current
    val from = LatLng(ride.origin.lat, ride.origin.lng)
    val to = LatLng(ride.destination.lat, ride.destination.lng)
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(from, 11f) }
    val road = rememberRoadRoute(ride)
    val points = ride.routePoints(road)
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        contentPadding = PaddingValues(top = 72.dp, bottom = 72.dp, start = 32.dp, end = 32.dp),
        properties = MapProperties(mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, if (c.isDark) R.raw.map_style_dark else R.raw.map_style_light)),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false, myLocationButtonEnabled = false),
    ) {
        MapEffect(from, to, points.size) {
            val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
            runCatching { camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 80)) }
        }
        Polyline(points, color = c.content, width = with(density) { 4.dp.toPx() }, geodesic = road == null)
        MarkerComposable("from", c.isDark, state = remember(from) { MarkerState(from) }, anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(c.background).border(4.dp, c.content, CircleShape))
        }
        MarkerComposable("to", c.isDark, state = remember(to) { MarkerState(to) }, anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)) {
            Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(c.content).border(2.dp, c.background, RoundedCornerShape(3.dp)))
        }
    }
}

/** The driver's live profile number, else the one saved on the ride; never an email from v1 data. */
private fun driverPhone(ride: Ride, state: RideDetailState): String? =
    state.driver?.phone?.takeIf(::isCallablePhone) ?: ride.creatorPhone.takeIf(::isCallablePhone)

private fun dial(context: Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
        .onFailure { Toast.makeText(context, "No phone app found", Toast.LENGTH_SHORT).show() }
}

/** wa.me needs the international number as bare digits; old local numbers (03xx…) are assumed Pakistani. */
internal fun whatsAppDigits(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    return when {
        phone.trim().startsWith("+") -> digits
        digits.startsWith("00") -> digits.drop(2)
        digits.startsWith("03") && digits.length == 11 -> "92" + digits.drop(1)
        else -> digits
    }
}

private fun whatsAppGreeting(ride: Ride): String =
    "Salam ${ride.creatorName.substringBefore(' ')}! I found your ride on Safar: " +
        "${shortPlace(ride.origin.name)} → ${shortPlace(ride.destination.name)}, ${dayLabel(ride.departureTime)} at ${timeLabel(ride.departureTime)}."

/** Opens the chat in WhatsApp (or WhatsApp Business); wa.me falls back to the browser if neither is installed. */
private fun openWhatsApp(context: Context, phone: String, text: String) {
    val uri = Uri.parse("https://wa.me/${whatsAppDigits(phone)}?text=${Uri.encode(text)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { Toast.makeText(context, "WhatsApp isn't available on this phone", Toast.LENGTH_SHORT).show() }
}

/** How long before departure a driver can start a ride that isn't full yet. */
private const val START_WINDOW_MS = 30L * 60 * 1000

/** "Weekdays" → "weekdays", "Mon, Wed" stays as is. */
private fun String.lowercaseFirstWord() = if (this in setOf("Weekdays", "Weekends", "Every day")) lowercase() else this
