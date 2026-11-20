package com.safar.app.ui.wanted

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import com.safar.app.AppContainer
import com.safar.app.data.model.DriverOffer
import com.safar.app.data.model.OfferStatus
import com.safar.app.data.model.Place
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.Ride
import com.safar.app.data.model.RiderMatch
import com.safar.app.data.model.RouteKind
import com.safar.app.data.model.SeatOffer
import com.safar.app.data.model.WantedMatch
import com.safar.app.data.model.WantedRide
import com.safar.app.data.model.WantedStatus
import com.safar.app.data.model.cityFromName
import com.safar.app.data.model.distanceLabel
import com.safar.app.data.model.haversineKm
import com.safar.app.data.seatsLabel
import com.safar.app.ui.Messages
import com.safar.app.ui.PICK_RESULT
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.ButtonKind
import com.safar.app.ui.components.ChoiceChip
import com.safar.app.ui.components.Divider
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.Stepper
import com.safar.app.ui.components.pressable
import com.safar.app.ui.components.rememberRoadRoute
import com.safar.app.ui.components.routePoints
import com.safar.app.ui.dayLabel
import com.safar.app.ui.post.Geo
import com.safar.app.ui.post.defaultDeparture
import com.safar.app.ui.post.utcMidnight
import com.safar.app.ui.post.withDate
import com.safar.app.ui.post.withDay
import com.safar.app.ui.post.withTime
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

/** "tomorrow", "today", or "on Mon 23 Nov", for running text. */
private fun dayPhrase(ms: Long) = dayLabel(ms).let { if (it == "Today" || it == "Tomorrow") it.lowercase() else "on $it" }

private fun windowLabel(start: Long, end: Long, anyTime: Boolean) =
    if (anyTime) "Any time ${dayPhrase(start)}" else "${dayLabel(start)} · ${timeLabel(start)}–${timeLabel(end)}"

private fun firstName(name: String) = name.trim().substringBefore(' ').ifBlank { "Driver" }

// ═══ Ask: the wanted-ride form ═══════════════════════════════════════════════

data class WantForm(
    val from: Place? = null,
    val to: Place? = null,
    val fromCity: String = "",
    val toCity: String = "",
    val time: Long = defaultDeparture(),
    val flex: Int = 30,
    val seats: Int = 1,
    val visible: Boolean = true,
    val saving: Boolean = false,
) {
    val kind: RouteKind?
        get() = if (from == null || to == null) null
        else RouteKind.classify(fromCity, toCity, haversineKm(from.lat, from.lng, to.lat, to.lng))
    val window get() = WantedRide.window(time, flex)
}

class WantRideViewModel(private val app: AppContainer) : SafarViewModel() {
    val form = MutableStateFlow(WantForm())

    fun update(f: (WantForm) -> WantForm) = form.update(f)

    fun applyPick(encoded: String) {
        val parts = encoded.split('|', limit = 4)
        if (parts.size < 4) return
        val place = Place(parts[3], parts[1].toDoubleOrNull() ?: return, parts[2].toDoubleOrNull() ?: return)
        form.update { if (parts[0] == "from") it.copy(from = place) else it.copy(to = place) }
    }

    fun resolveCities(context: android.content.Context) {
        val f = form.value
        viewModelScope.launch {
            val from = f.from?.let { Geo.city(context, it.lat, it.lng) ?: cityFromName(it.name) }.orEmpty()
            val to = f.to?.let { Geo.city(context, it.lat, it.lng) ?: cityFromName(it.name) }.orEmpty()
            form.update { if (it.from == f.from && it.to == f.to) it.copy(fromCity = from, toCity = to) else it }
        }
    }

    fun submit(onDone: (String) -> Unit) {
        val f = form.value
        val problem = when {
            f.from == null -> "Choose your pickup"
            f.to == null -> "Choose where you're going"
            f.from.lat == f.to.lat && f.from.lng == f.to.lng -> "Pickup and drop-off can't be the same place"
            f.window.second < System.currentTimeMillis() + 10 * 60_000 -> "Pick a time later than now"
            else -> null
        }
        if (problem != null) { say(problem); return }
        val me = app.session.requireUser()
        val (start, end) = f.window
        form.update { it.copy(saving = true) }
        var id = ""
        action(onDone = { cheer(); onDone(id) }, onError = { form.update { it.copy(saving = false) } }) {
            id = app.wanted.create(
                WantedRide(
                    uid = me.uid, name = me.name, photoUrl = me.photoUrl,
                    origin = f.from!!, destination = f.to!!,
                    originCity = f.fromCity.ifBlank { cityFromName(f.from.name) },
                    destinationCity = f.toCity.ifBlank { cityFromName(f.to.name) },
                    kind = f.kind?.key.orEmpty(), time = f.time, flexMinutes = f.flex,
                    windowStart = start.coerceAtLeast(System.currentTimeMillis()), windowEnd = end,
                    seats = f.seats, visible = f.visible, status = WantedStatus.ACTIVE,
                    createdAt = System.currentTimeMillis(),
                    utcOffsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WantRideScreen(nav: NavHostController, entry: NavBackStackEntry) {
    val vm = appViewModel(key = "want-new") { WantRideViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val f by vm.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val pick by entry.savedStateHandle.getStateFlow<String?>(PICK_RESULT, null).collectAsStateWithLifecycle()
    LaunchedEffect(pick) { pick?.let { vm.applyPick(it); entry.savedStateHandle[PICK_RESULT] = null } }
    LaunchedEffect(f.from, f.to) { if (f.from != null || f.to != null) vm.resolveCities(context) }

    val zone = if (f.kind == RouteKind.CITY_TO_CITY) 8000.0 else 1000.0
    val (start, end) = f.window

    Box(Modifier.fillMaxSize().background(c.background)) {
        WantedMap(
            Modifier.fillMaxWidth().height(340.dp),
            pickup = f.from?.let { LatLng(it.lat, it.lng) }, zoneMeters = zone,
            padding = PaddingValues(top = 60.dp, bottom = 40.dp),
        )
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() }, Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))

        Column(
            Modifier.fillMaxSize().padding(top = 300.dp)
                .clip(RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl)).background(c.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 22.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // Pickup leads: it weighs most in matching, so it's the biggest thing here.
            PlaceRow("Pickup", f.from?.name, "Where should they pick you up?", big = true) { nav.navigate(Routes.pick("from")) }
            PlaceRow("Going to", f.to?.name, "Choose drop-off", big = false) { nav.navigate(Routes.pick("to")) }
            if (f.from != null) Text(
                if (zone > 1000) "Matching rides that pass within 8 km" else "Matching rides that pass within 1 km",
                style = Safar.type.caption, color = c.contentTertiary,
            )
            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val today = dayLabel(f.time) == "Today"
                val tomorrow = dayLabel(f.time) == "Tomorrow"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChoiceChip("Today", today, { vm.update { it.copy(time = withDay(it.time, 0)) } })
                    ChoiceChip("Tomorrow", tomorrow, { vm.update { it.copy(time = withDay(it.time, 1)) } })
                    ChoiceChip(if (!today && !tomorrow) dayLabel(f.time) else "Date", !today && !tomorrow, { pickDate = true })
                    Spacer(Modifier.weight(1f))
                    if (f.flex != 0) Text(
                        timeLabel(f.time), style = Safar.type.title.copy(fontFeatureSettings = "tnum"), color = c.content,
                        modifier = Modifier.clip(RoundedCornerShape(Radii.sm)).clickable { pickTime = true }.padding(4.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WantedRide.FLEX_OPTIONS.forEach { m ->
                        ChoiceChip(
                            when (m) { 0 -> "Any time"; 60 -> "±1 hr"; else -> "±$m min" },
                            f.flex == m, { vm.update { it.copy(flex = m) } },
                        )
                    }
                }
                TimeRibbon(start, end, preferred = if (f.flex == 0) null else f.time)
            }
            Divider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Seats", style = Safar.type.bodyStrong, color = c.content, modifier = Modifier.weight(1f))
                Stepper(f.seats, { n -> vm.update { it.copy(seats = n.coerceIn(1, 4)) } }, 1..4)
            }
            Row(
                Modifier.toggleable(f.visible, remember { MutableInteractionSource() }, indication = null, role = Role.Switch) { on ->
                    vm.update { it.copy(visible = on) }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Drivers can offer me a seat", style = Safar.type.bodyStrong, color = c.content)
                    Text("They see your name and photo, never your number.", style = Safar.type.caption, color = c.contentSecondary)
                }
                Switch(
                    checked = f.visible, onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.onPrimary, checkedTrackColor = c.primary, checkedBorderColor = c.primary,
                        uncheckedThumbColor = c.contentTertiary, uncheckedTrackColor = c.surface, uncheckedBorderColor = c.border,
                    ),
                )
            }
        }

        SafarButton(
            "Find my ride",
            onClick = { vm.submit { id -> nav.navigate(Routes.want(id)) { popUpTo(Routes.WANT_NEW) { inclusive = true } } } },
            loading = f.saving,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMidnight(f.time),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= utcMidnight(System.currentTimeMillis())
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { sel -> vm.update { it.copy(time = withDate(it.time, sel)) } }
                    pickDate = false
                }) { Text("Done", color = c.content) }
            },
            dismissButton = { TextButton({ pickDate = false }) { Text("Cancel", color = c.contentSecondary) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = f.time }
        val state = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false)
        Dialog(onDismissRequest = { pickTime = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(Radii.xl)).background(c.surfaceRaised).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Leave around", style = Safar.type.headline, color = c.content, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                TimePicker(state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ pickTime = false }) { Text("Cancel", color = c.contentSecondary) }
                    TextButton({
                        vm.update { it.copy(time = withTime(it.time, state.hour, state.minute)) }
                        pickTime = false
                    }) { Text("Done", color = c.content) }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(label: String, value: String?, placeholder: String, big: Boolean, onClick: () -> Unit) {
    val c = Safar.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.sm)).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), style = Safar.type.overline, color = c.contentSecondary)
            Text(
                value?.let { shortPlace(it) } ?: placeholder,
                style = if (big) Safar.type.display else Safar.type.title,
                color = if (value == null) c.contentTertiary else c.content,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text("Change".takeIf { value != null } ?: "Choose", style = Safar.type.label, color = c.content)
    }
}

// ═══ A wanted ride, live ═════════════════════════════════════════════════════

data class WantDetailState(
    val loading: Boolean = true,
    val want: WantedRide? = null,
    val matches: List<WantedMatch> = emptyList(),
    val offers: List<SeatOffer> = emptyList(),
)

class WantDetailViewModel(private val app: AppContainer, private val id: String) : SafarViewModel() {
    private val uid = app.session.uid.orEmpty()
    val state: StateFlow<WantDetailState> = combine(
        app.wanted.observe(uid, id).catch { emit(null) },
        app.wanted.observeMatches(uid, id).catch { emit(emptyList()) },
        app.wanted.observeOffers(uid).catch { emit(emptyList()) },
    ) { w, m, o ->
        WantDetailState(false, w, if (w?.status == WantedStatus.ACTIVE) m else emptyList(), o.filter { it.wantId == id && it.status == OfferStatus.PENDING })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WantDetailState())

    fun stop(onDone: () -> Unit) = action(onDone = onDone) { app.wanted.cancel(uid, id) }
}

@Composable
fun WantDetailScreen(id: String, nav: NavHostController) {
    val vm = appViewModel(key = "want-$id") { WantDetailViewModel(it, id) }
    Messages(vm)
    val c = Safar.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val want = state.want

    Box(Modifier.fillMaxSize().background(c.background)) {
        WantedMap(
            Modifier.fillMaxWidth().height(330.dp),
            pickup = want?.origin?.let { LatLng(it.lat, it.lng) },
            end = want?.destination?.let { LatLng(it.lat, it.lng) },
            padding = PaddingValues(top = 70.dp, bottom = 50.dp, start = 40.dp, end = 40.dp),
        )
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() }, Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))

        Column(
            Modifier.fillMaxSize().padding(top = 296.dp)
                .clip(RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl)).background(c.background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (want == null) {
                if (!state.loading) Text("This request has ended.", style = Safar.type.body, color = c.contentSecondary)
                return@Column
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    want.isActive -> StatusLine("Searching · ${dayLabel(want.time)}", c.positive)
                    want.status == WantedStatus.BOOKED -> StatusLine("Booked", c.content)
                    want.status == WantedStatus.CANCELLED -> StatusLine("Stopped", c.contentTertiary)
                    else -> StatusLine("Ended", c.contentTertiary)
                }
                Text("To ${shortPlace(want.destination.name)}", style = Safar.type.display, color = c.content)
                Text(
                    "From ${shortPlace(want.origin.name)} · ${seatsLabel(want.seats)}",
                    style = Safar.type.label, color = c.contentSecondary,
                )
            }
            TimeRibbon(
                want.windowStart, want.windowEnd,
                preferred = if (want.anyTime) null else want.time,
                ticks = state.matches.sortedByDescending { it.score }.map { it.departureTime },
            )

            state.offers.forEach { o ->
                Divider()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.sm)).clickable { nav.navigate(Routes.offer(o.id)) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Avatar(o.driverName, o.driverPhoto, size = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("${firstName(o.driverName)} offered a seat", style = Safar.type.bodyStrong, color = c.content)
                        Text(
                            listOfNotNull(timeLabel(o.departureTime), o.pickupM?.let { "${distanceLabel(it)} from pickup" }).joinToString(" · "),
                            style = Safar.type.label, color = c.contentSecondary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.content)
                }
            }

            if (want.isActive && state.matches.isEmpty() && state.offers.isEmpty()) {
                Divider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nothing yet.", style = Safar.type.title, color = c.content)
                    Text("We'll notify you the moment a ride fits.", style = Safar.type.body, color = c.contentSecondary)
                }
            }
            state.matches.forEach { m ->
                Divider()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.sm)).clickable { nav.navigate(Routes.ride(m.rideId)) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(104.dp)) {
                        BigDistance(m.pickupM)
                        Text("from pickup", style = Safar.type.caption, color = c.contentTertiary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("${firstName(m.driverName)} · ${timeLabel(m.departureTime)}", style = Safar.type.bodyStrong, color = c.content)
                        Text(
                            listOf(if (m.passes) "Passes your pickup" else "Starts near you", if (m.price > 0) "Rs ${m.price}" else "Split fuel").joinToString(" · "),
                            style = Safar.type.label, color = c.contentSecondary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.contentTertiary)
                }
            }

            if (want.isActive) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stop looking", style = Safar.type.label, color = c.negative,
                    modifier = Modifier.clip(RoundedCornerShape(Radii.sm)).clickable { vm.stop { nav.back() } }.padding(vertical = 6.dp),
                )
            }
        }
    }
}

// ═══ A seat offered to the rider ═════════════════════════════════════════════

data class OfferState(val loading: Boolean = true, val offer: SeatOffer? = null, val ride: Ride? = null, val rating: RatingSummary = RatingSummary(), val busy: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class OfferViewModel(private val app: AppContainer, private val id: String) : SafarViewModel() {
    private val uid = app.session.uid.orEmpty()
    private val busy = MutableStateFlow(false)
    private val offer = app.wanted.observeOffer(uid, id).catch { emit(null) }
    val state: StateFlow<OfferState> = combine(
        offer,
        offer.map { it?.rideId }.flatMapLatest { if (it.isNullOrBlank()) flowOf(null) else app.rides.observe(it).catch { emit(null) } },
        offer.map { it?.driverUid }.flatMapLatest { if (it.isNullOrBlank()) flowOf(RatingSummary()) else app.users.observeRating(it).catch { emit(RatingSummary()) } },
        busy,
    ) { o, r, rt, b -> OfferState(false, o, r, rt, b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfferState())

    fun respond(accept: Boolean) {
        busy.value = true
        action(onDone = { busy.value = false; if (accept) { cheer(big = true); say("Seat confirmed") } }, onError = { busy.value = false }) {
            app.wanted.respond(id, accept)
        }
    }
}

@Composable
fun OfferScreen(id: String, nav: NavHostController) {
    val vm = appViewModel(key = "offer-$id") { OfferViewModel(it, id) }
    Messages(vm)
    val c = Safar.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val o = state.offer

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() }, Modifier.padding(start = 16.dp, top = 8.dp))
        if (o == null) {
            if (!state.loading) Text("This offer is no longer available.", style = Safar.type.body, color = c.contentSecondary, modifier = Modifier.padding(24.dp))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(o.driverName, o.driverPhoto, size = 72.dp)
                Column {
                    Text("SEAT OFFERED", style = Safar.type.overline, color = c.contentSecondary)
                    Text(o.driverName, style = Safar.type.display, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.rating.count > 0) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = c.content)
                            Text(" %.1f".format(state.rating.average) + if (o.vehicle.isNotBlank()) " · " else "", style = Safar.type.label, color = c.contentSecondary)
                        }
                        if (o.vehicle.isNotBlank()) Text(o.vehicle, style = Safar.type.label, color = c.contentSecondary)
                    }
                }
            }
            Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                Column(Modifier.weight(1f)) {
                    val t = timeLabel(o.departureTime)
                    Text(t.substringBefore(' '), style = Safar.type.display.copy(fontSize = androidx.compose.ui.unit.TextUnit(44f, androidx.compose.ui.unit.TextUnitType.Sp), fontFeatureSettings = "tnum"), color = c.content)
                    Text("${t.substringAfter(' ')}, ${dayPhrase(o.departureTime)}", style = Safar.type.caption, color = c.contentTertiary)
                }
                Box(Modifier.width(1.dp).fillMaxHeightCompat().background(c.border))
                Column(Modifier.weight(1f).padding(start = 20.dp)) {
                    if (o.pickupM != null) {
                        BigDistance(o.pickupM, size = 44)
                        Text(if (o.passes) "passes your pickup" else "from your pickup", style = Safar.type.caption, color = c.contentTertiary)
                    } else {
                        Text(seatsLabel(o.seats), style = Safar.type.title, color = c.content)
                    }
                }
            }
            val road = rememberRoadRoute(state.ride)
            val points = state.ride?.routePoints(road) ?: listOf(LatLng(o.origin.lat, o.origin.lng), LatLng(o.destination.lat, o.destination.lng))
            WantedMap(
                Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp)),
                pickup = o.pickup?.let { LatLng(it.lat, it.lng) }, route = points,
                end = LatLng(o.destination.lat, o.destination.lng),
                padding = PaddingValues(16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${shortPlace(o.origin.name)} → ${shortPlace(o.destination.name)}", style = Safar.type.label, color = c.contentSecondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (o.price > 0) "Rs ${o.price}" else "Split fuel", style = Safar.type.title.copy(fontFeatureSettings = "tnum"), color = c.content)
            }
        }
        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            AnimatedContent(o.status, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "offer") { status ->
                when (status) {
                    OfferStatus.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafarButton("Decline", { vm.respond(false) }, kind = ButtonKind.Ghost, enabled = !state.busy, modifier = Modifier.widthIn(min = 120.dp))
                        SafarButton("Take the seat", { vm.respond(true) }, loading = state.busy, modifier = Modifier.weight(1f))
                    }
                    OfferStatus.ACCEPTED -> SafarButton("Seat confirmed · View ride", { nav.navigate(Routes.ride(o.rideId)) }, modifier = Modifier.fillMaxWidth())
                    OfferStatus.DECLINED -> Text("You declined this offer.", style = Safar.type.body, color = c.contentSecondary, modifier = Modifier.fillMaxWidth())
                    OfferStatus.EXPIRED -> Text("This offer has expired.", style = Safar.type.body, color = c.contentSecondary, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun Modifier.fillMaxHeightCompat() = this.then(Modifier.fillMaxHeight())

// ═══ Riders on a driver's way ═════════════════════════════════════════════════

data class RidersState(
    val loading: Boolean = true,
    val ride: Ride? = null,
    val riders: List<RiderMatch> = emptyList(),
    val offers: Map<String, DriverOffer> = emptyMap(),
    val sending: Set<String> = emptySet(),
)

class RidersViewModel(private val app: AppContainer, private val rideId: String) : SafarViewModel() {
    private val uid = app.session.uid.orEmpty()
    private val sending = MutableStateFlow(emptySet<String>())
    val state: StateFlow<RidersState> = combine(
        app.rides.observe(rideId).catch { emit(null) },
        app.wanted.observeRiders(rideId).catch { emit(emptyList()) },
        app.wanted.observeDriverOffers(uid).catch { emit(emptyList()) },
        sending,
    ) { ride, riders, offers, s ->
        val forRide = offers.filter { it.rideId == rideId }
            // Latest offer per rider wins, so a declined one can be followed by a new one.
            .associateBy { "${it.riderUid}_${it.wantId}" }
        RidersState(false, ride, riders.filter { it.uid != uid }, forRide, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RidersState())

    fun offer(r: RiderMatch) {
        val key = "${r.uid}_${r.wantId}"
        sending.update { it + key }
        action(
            onDone = { sending.update { it - key }; cheer(); say("Offer sent to ${firstName(r.name)}") },
            onError = { sending.update { it - key } },
        ) { app.wanted.offer(rideId, r.uid, r.wantId) }
    }
}

@Composable
fun RidersScreen(rideId: String, nav: NavHostController) {
    val vm = appViewModel(key = "riders-$rideId") { RidersViewModel(it, rideId) }
    Messages(vm)
    val c = Safar.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val ride = state.ride
    val road = rememberRoadRoute(ride)

    Box(Modifier.fillMaxSize().background(c.background)) {
        WantedMap(
            Modifier.fillMaxWidth().height(380.dp),
            route = ride?.routePoints(road).orEmpty(),
            people = state.riders.map { LatLng(it.pickupLat, it.pickupLng) to it.name },
            end = ride?.destination?.let { LatLng(it.lat, it.lng) },
            padding = PaddingValues(top = 70.dp, bottom = 50.dp, start = 30.dp, end = 30.dp),
        )
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() }, Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))

        Column(
            Modifier.fillMaxSize().padding(top = 346.dp)
                .clip(RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl)).background(c.background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (ride != null) Text(
                "Your ${timeLabel(ride.departureTime)} to ${shortPlace(ride.destination.name)}".uppercase(),
                style = Safar.type.overline, color = c.contentSecondary,
            )
            Text(
                when (state.riders.size) { 0 -> "No riders on your way yet"; 1 -> "1 rider on your way"; else -> "${state.riders.size} riders on your way" },
                style = Safar.type.display, color = c.content,
            )
            if (state.riders.isEmpty() && !state.loading) Text(
                "When someone needs this route, they'll show up here.", style = Safar.type.body, color = c.contentSecondary,
            )
            state.riders.forEach { r ->
                Divider()
                RiderRow(r, state.offers["${r.uid}_${r.wantId}"], sending = "${r.uid}_${r.wantId}" in state.sending) { vm.offer(r) }
            }
        }
    }
}

@Composable
private fun RiderRow(r: RiderMatch, offer: DriverOffer?, sending: Boolean, onOffer: () -> Unit) {
    val c = Safar.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Avatar(r.name, r.photoUrl, size = 46.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("${firstName(r.name)} · ${seatsLabel(r.seats)}", style = Safar.type.bodyStrong, color = c.content)
            Text(
                "Pickup ${shortPlace(r.pickup)} · " + if (r.pickupM <= 300) "on your route" else "${distanceLabel(r.pickupM)} off route",
                style = Safar.type.label, color = c.contentSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (r.anyTime) "Any time ${dayPhrase(r.windowStart)}" else "Needs ${timeLabel(r.windowStart)}–${timeLabel(r.windowEnd)}",
                style = Safar.type.caption, color = c.contentTertiary,
            )
        }
        Box(Modifier.width(112.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(offer?.status to sending, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "rider") { (status, busy) ->
                when (status) {
                    OfferStatus.PENDING -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Offered", style = Safar.type.bodyStrong, color = c.content)
                        Text("Waiting for ${firstName(r.name)}", style = Safar.type.caption, color = c.contentTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OfferStatus.ACCEPTED -> Text("Took your seat", style = Safar.type.bodyStrong, color = c.positive)
                    else -> SafarButton(
                        if (status == OfferStatus.DECLINED) "Offer again" else "Offer seat", onOffer,
                        kind = ButtonKind.Ghost, loading = busy, height = 42.dp, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ═══ Entry points used by other screens ══════════════════════════════════════

/** "2 riders on your way ›" on a driver's own ride, when anyone matches it. */
@Composable
fun RidersOnYourWayRow(rideId: String, nav: NavHostController) {
    val app = LocalContext.current.applicationContext.let { (it as com.safar.app.SafarApp).container }
    val riders by remember(rideId) { app.wanted.observeRiders(rideId).catch { emit(emptyList()) } }.collectAsStateWithLifecycle(emptyList())
    val others = riders.filter { it.uid != app.session.uid }
    if (others.isEmpty()) return
    val c = Safar.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).background(c.surfaceMuted)
            .pressable(remember { MutableInteractionSource() }, onClick = { nav.navigate(Routes.riders(rideId)) }, pressedScale = 0.985f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            others.take(3).forEachIndexed { i, r ->
                Box(Modifier.offset(x = (-10 * i).dp).border(2.dp, c.surfaceMuted, CircleShape)) { Avatar(r.name, r.photoUrl, size = 32.dp) }
            }
        }
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text(if (others.size == 1) "1 rider on your way" else "${others.size} riders on your way", style = Safar.type.bodyStrong, color = c.content)
            Text("Offer them a seat", style = Safar.type.label, color = c.contentSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.content)
    }
}

/** One wanted ride in My Rides: where to, its window with matches as ticks, and what's new. */
@Composable
fun WantedRow(want: WantedRide, matches: List<WantedMatch>, offer: SeatOffer?, onClick: () -> Unit) {
    val c = Safar.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.sm)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(shortPlace(want.destination.name), style = Safar.type.title, color = c.content, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    offer != null -> "Offer from ${firstName(offer.driverName)} ›"
                    matches.size == 1 -> "1 match ›"
                    matches.isNotEmpty() -> "${matches.size} matches ›"
                    else -> "Searching"
                },
                style = if (offer != null || matches.isNotEmpty()) Safar.type.bodyStrong else Safar.type.label,
                color = if (offer != null || matches.isNotEmpty()) c.content else c.contentSecondary,
            )
        }
        TimeRibbon(want.windowStart, want.windowEnd, ticks = matches.sortedByDescending { it.score }.map { it.departureTime }, labels = false)
        Text(windowLabel(want.windowStart, want.windowEnd, want.anyTime), style = Safar.type.caption, color = c.contentTertiary)
    }
}
