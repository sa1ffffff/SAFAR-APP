package com.safar.app.data.model

import com.google.firebase.database.DataSnapshot

private fun DataSnapshot.s(key: String): String = child(key).getValue(String::class.java).orEmpty()
private fun DataSnapshot.sOrNull(key: String): String? = child(key).getValue(String::class.java)?.takeIf { it.isNotBlank() }
private fun DataSnapshot.l(key: String): Long = (child(key).value as? Number)?.toLong() ?: 0L
private fun DataSnapshot.i(key: String): Int = (child(key).value as? Number)?.toInt() ?: 0
private fun DataSnapshot.iOrNull(key: String): Int? = (child(key).value as? Number)?.toInt()
private fun DataSnapshot.d(key: String): Double = (child(key).value as? Number)?.toDouble() ?: 0.0
private fun DataSnapshot.b(key: String): Boolean = child(key).getValue(Boolean::class.java) ?: false

enum class WantedStatus { ACTIVE, BOOKED, CANCELLED, EXPIRED;
    companion object { fun parse(raw: String?) = entries.firstOrNull { it.name == raw } ?: EXPIRED }
}

/**
 * A rider's wanted ride, at wanted/{uid}/{id}. Safar's Worker matches it against every ride
 * posted until [windowEnd] and, when [visible], shows it to drivers on the route.
 */
data class WantedRide(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val origin: Place = Place("", 0.0, 0.0),
    val destination: Place = Place("", 0.0, 0.0),
    val originCity: String = "",
    val destinationCity: String = "",
    val kind: String = "",
    /** Preferred departure. */
    val time: Long = 0L,
    /** How far either side of [time] the rider can leave, in minutes; 0 means any time that day. */
    val flexMinutes: Int = 30,
    val windowStart: Long = 0L,
    val windowEnd: Long = 0L,
    val seats: Int = 1,
    val visible: Boolean = true,
    val status: WantedStatus = WantedStatus.ACTIVE,
    val createdAt: Long = 0L,
    val utcOffsetMinutes: Int = 300,
) {
    val isActive get() = status == WantedStatus.ACTIVE && windowEnd > System.currentTimeMillis()
    val anyTime get() = flexMinutes == 0

    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid, "name" to name, "photoUrl" to photoUrl,
        "origin" to origin.name, "originLat" to origin.lat, "originLng" to origin.lng,
        "destination" to destination.name, "destinationLat" to destination.lat, "destinationLng" to destination.lng,
        "originCity" to originCity, "destinationCity" to destinationCity, "kind" to kind,
        "time" to time, "flex" to flexMinutes, "windowStart" to windowStart, "windowEnd" to windowEnd,
        "seats" to seats, "visible" to visible, "status" to status.name, "createdAt" to createdAt,
        "utcOffsetMinutes" to utcOffsetMinutes,
    )

    companion object {
        /** Flex choices offered to riders, in minutes; 0 is "any time that day". */
        val FLEX_OPTIONS = listOf(15, 30, 60, 0)

        fun from(s: DataSnapshot, uid: String): WantedRide? {
            if (!s.exists()) return null
            return WantedRide(
                id = s.key.orEmpty(), uid = uid, name = s.s("name"), photoUrl = s.sOrNull("photoUrl"),
                origin = Place(s.s("origin"), s.d("originLat"), s.d("originLng")),
                destination = Place(s.s("destination"), s.d("destinationLat"), s.d("destinationLng")),
                originCity = s.s("originCity"), destinationCity = s.s("destinationCity"), kind = s.s("kind"),
                time = s.l("time"), flexMinutes = s.i("flex"), windowStart = s.l("windowStart"), windowEnd = s.l("windowEnd"),
                seats = s.i("seats").coerceAtLeast(1), visible = s.child("visible").getValue(Boolean::class.java) ?: true,
                status = WantedStatus.parse(s.sOrNull("status")), createdAt = s.l("createdAt"),
                utcOffsetMinutes = s.iOrNull("utcOffsetMinutes") ?: 300,
            )
        }

        /** The window for [time] with [flexMinutes] either side, or the whole local day for "any time". */
        fun window(time: Long, flexMinutes: Int): Pair<Long, Long> {
            if (flexMinutes > 0) return (time - flexMinutes * 60_000L) to (time + flexMinutes * 60_000L)
            val start = java.util.Calendar.getInstance().apply {
                timeInMillis = time
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            return start to start + 24 * 60 * 60_000L - 1
        }
    }
}

/** A ride that matches one of the rider's wanted rides (written by the Worker). */
data class WantedMatch(
    val rideId: String,
    val score: Double,
    val pickupM: Int,
    val dropM: Int,
    val passes: Boolean,
    val departureTime: Long,
    val price: Int,
    val driverUid: String,
    val driverName: String,
    val driverPhoto: String?,
    val origin: String,
    val destination: String,
) {
    companion object {
        fun from(s: DataSnapshot) = WantedMatch(
            rideId = s.key.orEmpty(), score = s.d("score"), pickupM = s.i("pickupM"), dropM = s.i("dropM"),
            passes = s.b("passes"), departureTime = s.l("departureTime"), price = s.i("price"),
            driverUid = s.s("driverUid"), driverName = s.s("driverName"), driverPhoto = s.sOrNull("driverPhoto"),
            origin = s.s("origin"), destination = s.s("destination"),
        )
    }
}

/** A rider a driver's ride could take (written by the Worker, read by the driver). */
data class RiderMatch(
    val uid: String,
    val wantId: String,
    val name: String,
    val photoUrl: String?,
    val seats: Int,
    val pickup: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoff: String,
    val pickupM: Int,
    val passes: Boolean,
    val score: Double,
    val windowStart: Long,
    val windowEnd: Long,
    val anyTime: Boolean,
) {
    companion object {
        fun from(s: DataSnapshot) = RiderMatch(
            uid = s.s("uid"), wantId = s.s("wantId"), name = s.s("name"), photoUrl = s.sOrNull("photoUrl"),
            seats = s.i("seats").coerceAtLeast(1), pickup = s.s("pickup"), pickupLat = s.d("pickupLat"), pickupLng = s.d("pickupLng"),
            dropoff = s.s("dropoff"), pickupM = s.i("pickupM"), passes = s.b("passes"), score = s.d("score"),
            windowStart = s.l("windowStart"), windowEnd = s.l("windowEnd"), anyTime = s.b("anyTime"),
        )
    }
}

enum class OfferStatus { PENDING, ACCEPTED, DECLINED, EXPIRED;
    companion object { fun parse(raw: String?) = entries.firstOrNull { it.name == raw } ?: EXPIRED }
}

/** A driver's offer of a seat, at offers/{riderUid}/{id} (written by the Worker). */
data class SeatOffer(
    val id: String,
    val wantId: String,
    val rideId: String,
    val driverUid: String,
    val driverName: String,
    val driverPhoto: String?,
    val vehicle: String,
    val departureTime: Long,
    val price: Int,
    val seats: Int,
    val origin: Place,
    val destination: Place,
    val pickup: Place?,
    val pickupM: Int?,
    val passes: Boolean,
    val status: OfferStatus,
    val createdAt: Long,
) {
    companion object {
        fun from(s: DataSnapshot) = SeatOffer(
            id = s.key.orEmpty(), wantId = s.s("wantId"), rideId = s.s("rideId"), driverUid = s.s("driverUid"),
            driverName = s.s("driverName"), driverPhoto = s.sOrNull("driverPhoto"), vehicle = s.s("vehicle"),
            departureTime = s.l("departureTime"), price = s.i("price"), seats = s.i("seats").coerceAtLeast(1),
            origin = Place(s.s("origin"), s.d("originLat"), s.d("originLng")),
            destination = Place(s.s("destination"), s.d("destinationLat"), s.d("destinationLng")),
            pickup = if (s.hasChild("pickupLat")) Place("", s.d("pickupLat"), s.d("pickupLng")) else null,
            pickupM = s.iOrNull("pickupM"), passes = s.b("passes"),
            status = OfferStatus.parse(s.sOrNull("status")), createdAt = s.l("createdAt"),
        )
    }
}

/** The driver's side of an offer: which rider, which ride, and where it stands. */
data class DriverOffer(val id: String, val riderUid: String, val wantId: String, val rideId: String, val status: OfferStatus) {
    companion object {
        fun from(s: DataSnapshot) = DriverOffer(
            s.key.orEmpty(), s.s("riderUid"), s.s("wantId"), s.s("rideId"), OfferStatus.parse(s.sOrNull("status")),
        )
    }
}

/** "200 m" / "1.4 km", for distances from a pickup. */
fun distanceLabel(meters: Int): String =
    if (meters < 1000) "${(meters / 10 * 10).coerceAtLeast(10)} m" else "%.1f km".format(meters / 1000.0)
