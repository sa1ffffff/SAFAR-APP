package com.safar.app.ui.post

import com.safar.app.ui.components.RouteKindMark
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.safar.app.data.model.cityFromName
import com.safar.app.data.model.haversineKm
import com.safar.app.data.model.RouteKind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.safar.app.ui.components.pressable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.Place
import com.safar.app.data.model.Ride
import com.safar.app.data.model.RideSeries
import com.safar.app.data.model.RideStatus
import com.safar.app.data.model.repeatLabel
import com.safar.app.ui.Messages
import com.safar.app.ui.PICK_RESULT
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.BottomActionBar
import com.safar.app.ui.components.ChoiceChip
import com.safar.app.ui.components.Field
import com.safar.app.ui.components.FieldButton
import com.safar.app.ui.components.IconCircle
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.components.Stepper
import com.safar.app.ui.dayLabel
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import com.safar.app.ui.timeLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.TimeZone

data class PostForm(
    val from: Place? = null,
    val to: Place? = null,
    val departure: Long = defaultDeparture(),
    val seats: Int = 3,
    val price: String = "",
    val vehicle: String = "",
    val notes: String = "",
    val saving: Boolean = false,
    val loaded: Boolean = false,
    /** Weekly repeat: ISO weekdays (Mon = 1). Only offered when posting a new ride. */
    val repeat: Boolean = false,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5),
    /** Cities of the chosen places, looked up as they're picked. */
    val fromCity: String = "",
    val toCity: String = "",
) {
    val kind: RouteKind?
        get() = if (from == null || to == null) null
        else RouteKind.classify(fromCity, toCity, haversineKm(from.lat, from.lng, to.lat, to.lng))
}

/** The first :00 or :30 that is at least 30 minutes from now (7:12 → 8:00, 7:45 → 8:30). */
fun defaultDeparture(): Long = Calendar.getInstance().apply {
    add(Calendar.MINUTE, 30)
    val m = get(Calendar.MINUTE)
    if (m == 0 || m == 30) { set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    else {
        add(Calendar.MINUTE, if (m < 30) 30 - m else 60 - m)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
}.timeInMillis

class PostRideViewModel(private val app: AppContainer, private val editId: String?) : SafarViewModel() {
    val form = MutableStateFlow(PostForm(loaded = editId == null))
    private var original: Ride? = null

    init {
        if (editId != null) action {
            val ride = app.rides.get(editId) ?: error("This ride no longer exists.")
            original = ride
            form.value = PostForm(
                from = ride.origin, to = ride.destination, departure = ride.departureTime, seats = ride.availableSeats.coerceAtLeast(1),
                price = if (ride.pricePerSeat > 0) ride.pricePerSeat.toString() else "",
                vehicle = ride.vehicleDetails, notes = ride.notes, loaded = true,
            )
        } else action {
            // Prefill the car from the driver's last ride; most people drive the same one.
            val uid = app.session.uid ?: return@action
            val last = app.rides.observeRidesBy(uid).firstOrNull()?.firstOrNull()
            if (last != null && form.value.vehicle.isBlank()) form.update { it.copy(vehicle = last.vehicleDetails) }
        }
    }

    fun update(f: (PostForm) -> PostForm) = form.update(f)

    /** Looks up the city of each chosen place, falling back to the place name. */
    fun resolveCities(context: android.content.Context) {
        val f = form.value
        viewModelScope.launch {
            val from = f.from?.let { p -> Geo.city(context, p.lat, p.lng) ?: cityFromName(p.name) }.orEmpty()
            val to = f.to?.let { p -> Geo.city(context, p.lat, p.lng) ?: cityFromName(p.name) }.orEmpty()
            form.update { if (it.from == f.from && it.to == f.to) it.copy(fromCity = from, toCity = to) else it }
        }
    }

    fun applyPick(encoded: String) {
        val parts = encoded.split('|', limit = 4)
        if (parts.size < 4) return
        val place = Place(parts[3], parts[1].toDoubleOrNull() ?: return, parts[2].toDoubleOrNull() ?: return)
        form.update { if (parts[0] == "from") it.copy(from = place) else it.copy(to = place) }
    }

    fun error(f: PostForm): String? = when {
        f.from == null -> "Choose where you're leaving from"
        f.to == null -> "Choose where you're heading"
        f.from.lat == f.to.lat && f.from.lng == f.to.lng -> "Pickup and drop-off can't be the same place"
        f.repeat && f.days.isEmpty() -> "Choose at least one day to repeat on"
        !f.repeat && f.departure < System.currentTimeMillis() + 5 * 60_000 -> "Pick a departure time in the future"
        else -> null
    }

    fun submit(onDone: (String) -> Unit) {
        val f = form.value
        error(f)?.let { say(it); return }
        val me = app.session.requireUser()
        form.update { it.copy(saving = true) }
        var savedId = editId.orEmpty()
        action(onDone = { cheer(big = editId == null); onDone(savedId) }, onError = { form.update { it.copy(saving = false) } }) {
            val base = original ?: Ride()
            val ride = base.copy(
                creatorUid = me.uid, creatorName = me.name, creatorPhone = me.phone.takeIf { me.hasCallablePhone }.orEmpty(), creatorPhotoUrl = me.photoUrl,
                origin = f.from!!, destination = f.to!!, departureTime = f.departure, availableSeats = f.seats,
                // Seats already booked stay booked when the driver edits the number offered.
                totalSeats = f.seats + (base.totalSeats - base.availableSeats).coerceAtLeast(0),
                pricePerSeat = f.price.toIntOrNull() ?: 0, vehicleDetails = f.vehicle.trim(), notes = f.notes.trim(),
                originCity = f.fromCity.ifBlank { cityFromName(f.from.name) },
                destinationCity = f.toCity.ifBlank { cityFromName(f.to.name) },
                kind = f.kind?.key.orEmpty(),
            )
            savedId = when {
                original != null -> {
                    if (ride.status != RideStatus.STARTED) app.rides.update(ride.copy(expiresAt = f.departure + Ride.RIDE_LIFETIME_MS))
                    else app.rides.update(ride)
                    ride.id
                }
                f.repeat -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = f.departure }
                    app.rides.postRepeating(ride, f.days.sorted(), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
                else -> app.rides.post(ride)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRideScreen(editId: String?, nav: NavHostController, entry: NavBackStackEntry) {
    val vm = appViewModel(key = "post-$editId") { PostRideViewModel(it, editId) }
    Messages(vm)
    val c = Safar.colors
    val f by vm.form.collectAsStateWithLifecycle()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val pick by entry.savedStateHandle.getStateFlow<String?>(PICK_RESULT, null).collectAsStateWithLifecycle()
    LaunchedEffect(pick) {
        pick?.let { vm.applyPick(it); entry.savedStateHandle[PICK_RESULT] = null }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(f.from, f.to) { if (f.from != null || f.to != null) vm.resolveCities(context) }

    val today = dayLabel(f.departure) == "Today"
    val tomorrow = dayLabel(f.departure) == "Tomorrow"

    ScreenScaffold(
        title = if (editId == null) "Offer a ride" else "Edit ride",
        onBack = { nav.back() },
        bottomBar = {
            BottomActionBar {
                SafarButton(
                    if (editId == null) "Post ride" else "Save changes",
                    onClick = {
                        vm.submit { id ->
                            if (editId == null) {
                                nav.navigate(Routes.ride(id)) { popUpTo(Routes.MAIN) }
                            } else nav.back()
                        }
                    },
                    loading = f.saving, enabled = f.loaded, modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(if (editId == null) "Where are you driving?" else "Update your ride", style = Safar.type.display, color = c.content, modifier = Modifier.padding(top = 8.dp))

            Box {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldButton(
                        "From", f.from?.name ?: "Choose pickup", onClick = { nav.navigate(Routes.pick("from")) }, placeholder = f.from == null,
                        leading = { Box(Modifier.size(12.dp).border(2.5.dp, c.content, CircleShape)) },
                    )
                    FieldButton(
                        "To", f.to?.name ?: "Choose drop-off", onClick = { nav.navigate(Routes.pick("to")) }, placeholder = f.to == null,
                        leading = { Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(c.content)) },
                    )
                }
                IconCircle(
                    Icons.Rounded.SwapVert, "Swap pickup and drop-off",
                    onClick = { vm.update { it.copy(from = it.to, to = it.from) } },
                    size = 36.dp, background = c.surface,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).border(1.dp, c.border, CircleShape),
                )
            }

            f.kind?.let { kind -> RouteKindNote(kind, f.fromCity, f.toCity) }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("When", style = Safar.type.headline, color = c.content)
                // A repeat sets its own dates, so the one-off date choice steps aside.
                AnimatedVisibility(
                    visible = !f.repeat,
                    enter = fadeIn(tween(200)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip("Today", today, { vm.update { it.copy(departure = withDay(it.departure, 0)) } })
                        ChoiceChip("Tomorrow", tomorrow, { vm.update { it.copy(departure = withDay(it.departure, 1)) } })
                        ChoiceChip(if (!today && !tomorrow) dayLabel(f.departure) else "Pick date", !today && !tomorrow, { pickDate = true })
                    }
                }
                FieldButton(
                    "Departure", timeLabel(f.departure), onClick = { pickTime = true },
                    leading = { Icon(Icons.Rounded.Schedule, null, tint = c.contentSecondary) },
                )
                if (editId == null) RepeatPicker(
                    repeat = f.repeat, days = f.days, departure = f.departure,
                    onRepeat = { on -> vm.update { it.copy(repeat = on) } },
                    onToggleDay = { d -> vm.update { it.copy(days = if (d in it.days) it.days - d else it.days + d) } },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(Radii.md)).background(c.surfaceMuted).padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Seats", style = Safar.type.caption, color = c.contentSecondary)
                    Spacer(Modifier.height(6.dp))
                    Stepper(f.seats, { n -> vm.update { it.copy(seats = n.coerceIn(1, 7)) } }, 1..7)
                }
                Field(
                    value = f.price, onValueChange = { v -> vm.update { it.copy(price = v.filter(Char::isDigit).take(5)) } },
                    label = "Per seat (Rs)", placeholder = "Optional",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }
            Field(
                value = f.vehicle, onValueChange = { v -> vm.update { it.copy(vehicle = v.take(40)) } },
                label = "Car", placeholder = "White Honda Civic",
                leading = { Icon(Icons.Rounded.DirectionsCar, null, tint = c.contentSecondary) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Field(
                value = f.notes, onValueChange = { v -> vm.update { it.copy(notes = v.take(300)) } },
                label = "Notes for riders", placeholder = "Luggage space, pickup point, AC…",
                singleLine = false, minLines = 3, textStyle = Safar.type.body,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Text(
                "Leave the price empty to split fuel at pickup. Safar never handles payments.",
                style = Safar.type.caption, color = c.contentTertiary,
            )
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMidnight(f.departure),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= utcMidnight(System.currentTimeMillis())
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { sel -> vm.update { it.copy(departure = withDate(it.departure, sel)) } }
                    pickDate = false
                }) { Text("Done", color = c.content) }
            },
            dismissButton = { TextButton({ pickDate = false }) { Text("Cancel", color = c.contentSecondary) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = f.departure }
        val state = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false)
        Dialog(onDismissRequest = { pickTime = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(Radii.xl)).background(c.surfaceRaised).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Departure time", style = Safar.type.headline, color = c.content, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                TimePicker(state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ pickTime = false }) { Text("Cancel", color = c.contentSecondary) }
                    TextButton({
                        vm.update { it.copy(departure = withTime(it.departure, state.hour, state.minute)) }
                        pickTime = false
                    }) { Text("Done", color = c.content) }
                }
            }
        }
    }
}

private fun withDay(current: Long, daysFromToday: Int): Long {
    val src = Calendar.getInstance().apply { timeInMillis = current }
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, src.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun withDate(current: Long, utcDate: Long): Long {
    val src = Calendar.getInstance().apply { timeInMillis = current }
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcDate }
    return Calendar.getInstance().apply {
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH),
            src.get(Calendar.HOUR_OF_DAY), src.get(Calendar.MINUTE), 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun withTime(current: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = current
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun utcMidnight(local: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** "Repeat weekly" switch, then the days it repeats on and when the next ride goes out. */
@Composable
private fun RepeatPicker(
    repeat: Boolean, days: Set<Int>, departure: Long,
    onRepeat: (Boolean) -> Unit, onToggleDay: (Int) -> Unit,
) {
    val c = Safar.colors
    val haptics = com.safar.app.ui.haptics()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).background(c.surfaceMuted).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier.toggleable(repeat, remember { MutableInteractionSource() }, indication = null, role = Role.Switch) {
                haptics.tap(); onRepeat(it)
            },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Repeat, null, tint = c.content, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text("Repeat weekly", style = Safar.type.bodyStrong, color = c.content)
                Text(
                    if (repeat && days.isNotEmpty()) "${repeatLabel(days.sorted())} at ${timeLabel(departure)}" else "Drive this route on a schedule",
                    style = Safar.type.caption, color = c.contentSecondary,
                )
            }
            Switch(
                checked = repeat,
                onCheckedChange = null, // the whole row toggles
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onPrimary, checkedTrackColor = c.primary, checkedBorderColor = c.primary,
                    uncheckedThumbColor = c.contentTertiary, uncheckedTrackColor = c.surface, uncheckedBorderColor = c.border,
                ),
            )
        }
        AnimatedVisibility(
            visible = repeat,
            enter = fadeIn(tween(220, delayMillis = 60)) + expandVertically(tween(280, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..7).forEach { day ->
                        DayToggle(DAY_LETTERS[day - 1], DAY_NAMES[day - 1], day in days) {
                            haptics.tick()
                            onToggleDay(day)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val next = remember(days, departure) {
                    val cal = Calendar.getInstance().apply { timeInMillis = departure }
                    val now = System.currentTimeMillis()
                    val offset = TimeZone.getDefault().getOffset(now) / 60_000
                    RideSeries.upcoming(days.toList(), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), offset, now)
                }
                Text(
                    when {
                        days.isEmpty() -> "Choose the days you drive."
                        next.isEmpty() -> "No rides in the next week at this time."
                        else -> "First ride ${dayLabel(next.first()).let { if (it == "Today" || it == "Tomorrow") it.lowercase() else "on $it" }}. Rides are posted a week ahead until you stop repeating."
                    },
                    style = Safar.type.caption, color = c.contentSecondary,
                )
            }
        }
    }
}

@Composable
private fun DayToggle(letter: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val c = Safar.colors
    val bg by animateColorAsState(if (selected) c.primary else c.surface, tween(180), label = "day-bg")
    val fg by animateColorAsState(if (selected) c.onPrimary else c.content, tween(180), label = "day-fg")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(dampingRatio = 0.6f, stiffness = 700f), label = "day-scale")
    Box(
        Modifier.size(40.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape).background(bg)
            .border(1.dp, if (selected) c.primary else c.border, CircleShape)
            .pressable(remember { MutableInteractionSource() }, onClick = onClick, role = Role.Checkbox, pressedScale = 0.9f)
            .semantics { contentDescription = name; stateDescription = if (selected) "Repeats" else "Off" },
        contentAlignment = Alignment.Center,
    ) { Text(letter, style = Safar.type.label, color = fg) }
}

/** What kind of ride this is, worked out from the places: no choice for the driver to get wrong. */
@Composable
private fun RouteKindNote(kind: RouteKind, fromCity: String, toCity: String) {
    val c = Safar.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RouteKindMark(kind, Modifier.size(18.dp))
        Text(
            when {
                kind == RouteKind.CITY_TO_CITY && fromCity.isNotBlank() && toCity.isNotBlank() -> "City to city · $fromCity to $toCity"
                kind == RouteKind.CITY_TO_CITY -> "City to city"
                fromCity.isNotBlank() -> "Local · within $fromCity"
                else -> "Local ride"
            },
            style = Safar.type.label, color = c.contentSecondary,
        )
    }
}
