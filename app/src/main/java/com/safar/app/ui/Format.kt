package com.safar.app.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private fun sameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

fun dayLabel(ms: Long, now: Long = System.currentTimeMillis()): String {
    val t = Calendar.getInstance().apply { timeInMillis = ms }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(t, today) -> "Today"
        sameDay(t, tomorrow) -> "Tomorrow"
        sameDay(t, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(ms))
    }
}

fun timeLabel(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

/** "Today · 5:30 PM" */
fun departureLabel(ms: Long): String = "${dayLabel(ms)} · ${timeLabel(ms)}"

/** "Leaves in 2h 10m", "Left 3h ago" */
fun countdownLabel(ms: Long, now: Long = System.currentTimeMillis()): String {
    val diff = ms - now
    val abs = kotlin.math.abs(diff)
    val h = TimeUnit.MILLISECONDS.toHours(abs)
    val m = TimeUnit.MILLISECONDS.toMinutes(abs) % 60
    val d = TimeUnit.MILLISECONDS.toDays(abs)
    val span = when {
        d >= 1 -> "${d}d ${h % 24}h"
        h >= 1 -> "${h}h ${m}m"
        else -> "${m.coerceAtLeast(1)}m"
    }
    return if (diff >= 0) "Leaves in $span" else "Left $span ago"
}

/** Chat-list style timestamp: time today, weekday this week, else date. */
fun shortStamp(ms: Long, now: Long = System.currentTimeMillis()): String {
    if (ms <= 0) return ""
    val age = now - ms
    return when {
        dayLabel(ms, now) == "Today" -> timeLabel(ms)
        dayLabel(ms, now) == "Yesterday" -> "Yesterday"
        age < TimeUnit.DAYS.toMillis(6) -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(ms))
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
    }
}

fun relativeAgo(ms: Long, now: Long = System.currentTimeMillis()): String {
    val mins = TimeUnit.MILLISECONDS.toMinutes(now - ms)
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}d ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
    }
}

fun memberSince(ms: Long): String =
    if (ms <= 0) "" else "Member since " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(ms))

fun priceLabel(pkr: Int): String = if (pkr <= 0) "Split fuel" else "Rs %,d".format(pkr)

/** Short place name: the first part of a geocoded address ("NUST H-12, Islamabad, Pakistan" → "NUST H-12"). */
fun shortPlace(name: String): String = com.safar.app.data.model.shortPlaceName(name)

fun shortRoute(label: String): String = com.safar.app.data.model.shortRouteLabel(label)
