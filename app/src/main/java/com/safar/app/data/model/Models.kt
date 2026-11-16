package com.safar.app.data.model

import com.google.firebase.database.DataSnapshot

/*
 * Domain models. Field names mirror the existing Realtime Database keys so data
 * written by the old app keeps loading. Mapping is explicit (no reflection), which
 * keeps it safe under R8 and tolerant of missing or legacy fields.
 */

private fun DataSnapshot.str(key: String): String = child(key).getValue(String::class.java).orEmpty()
private fun DataSnapshot.strOrNull(key: String): String? = child(key).getValue(String::class.java)?.takeIf { it.isNotBlank() }
private fun DataSnapshot.long(key: String): Long = (child(key).value as? Number)?.toLong() ?: 0L
private fun DataSnapshot.int(key: String): Int = (child(key).value as? Number)?.toInt() ?: 0
private fun DataSnapshot.double(key: String): Double = (child(key).value as? Number)?.toDouble() ?: 0.0
private fun DataSnapshot.bool(key: String): Boolean = child(key).getValue(Boolean::class.java) ?: false

/** "SAVe lab, NUST, H-12, Islamabad" → "SAVe lab"; skips Plus Codes like "P27F+JVM". */
fun shortPlaceName(name: String): String =
    name.split(",").map { it.trim() }.firstOrNull { it.isNotEmpty() && '+' !in it } ?: name.trim()

/** Shortens both ends of a stored "A → B" route label. */
fun shortRouteLabel(label: String): String = label.split(" → ").joinToString(" → ") { shortPlaceName(it) }

/** True for something a dialer can call: digits, spaces, dashes, a leading plus. Rejects emails. */
fun isCallablePhone(value: String): Boolean =
    value.isNotBlank() && value.all { it.isDigit() || it in "+ -" } && value.count(Char::isDigit) in 10..15

data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val joinedAt: Long = 0L,
    val totalRides: Int = 0,
    val authProvider: String = "",
) {
    val firstName: String get() = name.trim().substringBefore(' ').ifBlank { "there" }

    /** v1 accounts sometimes stored an email in `phone`; only a real number counts. */
    val hasCallablePhone: Boolean get() = isCallablePhone(phone)

    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "name" to name,
        "phone" to phone,
        "email" to email,
        "photoUrl" to photoUrl,
        "bio" to bio,
        "joinedAt" to joinedAt,
        "totalRides" to totalRides,
        "authProvider" to authProvider,
    )

    companion object {
        fun from(s: DataSnapshot): User? {
            if (!s.exists()) return null
            return User(
                uid = s.key.orEmpty(),
                name = s.str("name"),
                phone = s.str("phone"),
                email = s.str("email"),
                photoUrl = s.strOrNull("photoUrl"),
                bio = s.str("bio"),
                joinedAt = s.long("joinedAt"),
                totalRides = s.int("totalRides"),
                authProvider = s.str("authProvider"),
            )
        }
    }
}

/** Aggregate stored at /ratings/{uid}. Updated in a transaction by reviewers. */
data class RatingSummary(val sum: Long = 0, val count: Long = 0) {
    val average: Double get() = if (count == 0L) 0.0 else sum.toDouble() / count
}

data class Place(val name: String, val lat: Double, val lng: Double)

enum class RideStatus { OPEN, FILLED, COMPLETED, CANCELLED;
    companion object {
        fun parse(raw: String?): RideStatus = entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

data class Ride(
    val id: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val creatorPhone: String = "",
    val creatorPhotoUrl: String? = null,
    val origin: Place = Place("", 0.0, 0.0),
    val destination: Place = Place("", 0.0, 0.0),
    val departureTime: Long = 0L,
    val availableSeats: Int = 0,
    val pricePerSeat: Int = 0,
    val vehicleDetails: String = "",
    val notes: String = "",
    val status: RideStatus = RideStatus.OPEN,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        val expiry = if (expiresAt > 0) expiresAt else createdAt + EXPIRY_WINDOW_MS
        return expiry <= now
    }

    /** Open, has seats, and hasn't left yet (10 minutes' grace for a late start). */
    val isBookable: Boolean
        get() = status == RideStatus.OPEN && availableSeats > 0 && !isExpired() &&
            departureTime > System.currentTimeMillis() - DEPARTURE_GRACE_MS

    fun toMap(): Map<String, Any?> = mapOf(
        "adId" to id,
        "creatorUid" to creatorUid,
        "creatorName" to creatorName,
        "creatorPhone" to creatorPhone,
        "creatorPhotoUrl" to creatorPhotoUrl,
        "origin" to origin.name,
        "originLat" to origin.lat,
        "originLng" to origin.lng,
        "destination" to destination.name,
        "destinationLat" to destination.lat,
        "destinationLng" to destination.lng,
        "departureTime" to departureTime,
        "availableSeats" to availableSeats,
        "pricePerSeat" to pricePerSeat,
        "vehicleDetails" to vehicleDetails,
        "notes" to notes,
        "status" to status.name,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt,
    )

    companion object {
        const val EXPIRY_WINDOW_MS = 3L * 24 * 60 * 60 * 1000
        const val DEPARTURE_GRACE_MS = 10L * 60 * 1000

        fun from(s: DataSnapshot): Ride? {
            if (!s.exists()) return null
            return Ride(
                id = s.key.orEmpty(),
                creatorUid = s.str("creatorUid"),
                creatorName = s.str("creatorName"),
                creatorPhone = s.str("creatorPhone"),
                creatorPhotoUrl = s.strOrNull("creatorPhotoUrl"),
                origin = Place(s.str("origin"), s.double("originLat"), s.double("originLng")),
                destination = Place(s.str("destination"), s.double("destinationLat"), s.double("destinationLng")),
                departureTime = s.long("departureTime"),
                availableSeats = s.int("availableSeats"),
                pricePerSeat = s.int("pricePerSeat"),
                vehicleDetails = s.str("vehicleDetails"),
                notes = s.str("notes"),
                status = RideStatus.parse(s.strOrNull("status")),
                createdAt = s.long("createdAt"),
                expiresAt = s.long("expiresAt"),
            )
        }
    }
}

enum class RequestStatus { PENDING, ACCEPTED, REJECTED, CANCELLED;
    companion object {
        fun parse(raw: String?): RequestStatus = when (raw) {
            "COMPLETED" -> ACCEPTED // legacy value; completion lives in its own flags
            else -> entries.firstOrNull { it.name == raw } ?: PENDING
        }
    }
}

data class JoinRequest(
    val id: String = "",
    val adId: String = "",
    val requestorUid: String = "",
    val requestorName: String = "",
    val requestorPhone: String = "",
    val requestorPhotoUrl: String? = null,
    val adCreatorUid: String = "",
    val seats: Int = 1,
    val routeLabel: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = 0L,
    val requesterMarkedComplete: Boolean = false,
    val creatorMarkedComplete: Boolean = false,
    val requesterReviewedCreator: Boolean = false,
    val creatorReviewedRequester: Boolean = false,
) {
    val isCompleted: Boolean get() = requesterMarkedComplete && creatorMarkedComplete

    fun otherParty(myUid: String): String = if (myUid == adCreatorUid) requestorUid else adCreatorUid

    fun canReview(myUid: String): Boolean = isCompleted && when (myUid) {
        adCreatorUid -> !creatorReviewedRequester
        requestorUid -> !requesterReviewedCreator
        else -> false
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "requestId" to id,
        "adId" to adId,
        "requestorUid" to requestorUid,
        "requestorName" to requestorName,
        "requestorPhone" to requestorPhone,
        "requestorPhotoUrl" to requestorPhotoUrl,
        "adCreatorUid" to adCreatorUid,
        "seats" to seats,
        "routeLabel" to routeLabel,
        "status" to status.name,
        "timestamp" to timestamp,
        "requesterMarkedComplete" to requesterMarkedComplete,
        "creatorMarkedComplete" to creatorMarkedComplete,
        "requesterReviewedCreator" to requesterReviewedCreator,
        "creatorReviewedRequester" to creatorReviewedRequester,
    )

    companion object {
        fun from(s: DataSnapshot): JoinRequest? {
            if (!s.exists()) return null
            return JoinRequest(
                id = s.key.orEmpty(),
                adId = s.str("adId"),
                requestorUid = s.str("requestorUid"),
                requestorName = s.str("requestorName"),
                requestorPhone = s.str("requestorPhone"),
                requestorPhotoUrl = s.strOrNull("requestorPhotoUrl"),
                adCreatorUid = s.str("adCreatorUid"),
                seats = s.int("seats").coerceAtLeast(1),
                routeLabel = s.str("routeLabel"),
                status = RequestStatus.parse(s.strOrNull("status")),
                timestamp = s.long("timestamp"),
                requesterMarkedComplete = s.bool("requesterMarkedComplete"),
                creatorMarkedComplete = s.bool("creatorMarkedComplete"),
                requesterReviewedCreator = s.bool("requesterReviewedCreator"),
                creatorReviewedRequester = s.bool("creatorReviewedRequester"),
            )
        }
    }
}

data class Participant(val name: String = "", val photoUrl: String? = null)

data class Conversation(
    val id: String = "",
    val participants: Set<String> = emptySet(),
    val participantInfo: Map<String, Participant> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Long> = emptyMap(),
) {
    fun otherUid(myUid: String): String = participants.firstOrNull { it != myUid }
        ?: id.split('_').firstOrNull { it != myUid }.orEmpty()

    fun other(myUid: String): Participant = participantInfo[otherUid(myUid)] ?: Participant()
    fun unreadFor(uid: String): Long = unreadCount[uid] ?: 0L

    companion object {
        fun idFor(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

        fun from(s: DataSnapshot): Conversation? {
            if (!s.exists()) return null
            return Conversation(
                id = s.key.orEmpty(),
                participants = s.child("participants").children.mapNotNull { it.key }.toSet(),
                participantInfo = s.child("participantInfo").children.associate {
                    it.key.orEmpty() to Participant(it.str("name"), it.strOrNull("photoUrl"))
                },
                lastMessage = s.str("lastMessage"),
                lastMessageTimestamp = s.long("lastMessageTimestamp"),
                lastMessageSenderId = s.str("lastMessageSenderId"),
                unreadCount = s.child("unreadCount").children.associate {
                    it.key.orEmpty() to ((it.value as? Number)?.toLong() ?: 0L)
                },
            )
        }
    }
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val type: String = TYPE_TEXT,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "messageId" to id,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "timestamp" to timestamp,
        "type" to type,
    )

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_SYSTEM = "system"

        fun from(s: DataSnapshot): Message? {
            if (!s.exists()) return null
            return Message(
                id = s.key.orEmpty(),
                senderId = s.str("senderId"),
                senderName = s.str("senderName"),
                text = s.str("text"),
                timestamp = s.long("timestamp"),
                type = s.strOrNull("type") ?: TYPE_TEXT,
            )
        }
    }
}

data class Review(
    val id: String = "",
    val rideAdId: String = "",
    val joinRequestId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val revieweeId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0L,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "reviewId" to id,
        "rideAdId" to rideAdId,
        "joinRequestId" to joinRequestId,
        "reviewerId" to reviewerId,
        "reviewerName" to reviewerName,
        "revieweeId" to revieweeId,
        "rating" to rating,
        "comment" to comment,
        "timestamp" to timestamp,
    )

    companion object {
        fun from(s: DataSnapshot): Review? {
            if (!s.exists()) return null
            return Review(
                id = s.key.orEmpty(),
                rideAdId = s.str("rideAdId"),
                joinRequestId = s.str("joinRequestId"),
                reviewerId = s.str("reviewerId"),
                reviewerName = s.str("reviewerName"),
                revieweeId = s.str("revieweeId"),
                rating = s.int("rating"),
                comment = s.str("comment"),
                timestamp = s.long("timestamp"),
            )
        }
    }
}

enum class NotificationType { NEW_REQUEST, REQUEST_ACCEPTED, REQUEST_DECLINED, RIDE_COMPLETED, NEW_REVIEW, NEW_MESSAGE;
    companion object {
        fun parse(raw: String?): NotificationType = entries.firstOrNull { it.name == raw } ?: NEW_MESSAGE
    }
}

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.NEW_MESSAGE,
    val title: String = "",
    val message: String = "",
    val relatedAdId: String = "",
    val relatedRequestId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhoto: String? = null,
    val timestamp: Long = 0L,
    val read: Boolean = false,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "notificationId" to id,
        "type" to type.name,
        "title" to title,
        "message" to message,
        "relatedAdId" to relatedAdId,
        "relatedRequestId" to relatedRequestId,
        "fromUserId" to fromUserId,
        "fromUserName" to fromUserName,
        "fromUserPhoto" to fromUserPhoto,
        "timestamp" to timestamp,
        "read" to read,
    )

    companion object {
        fun from(s: DataSnapshot): AppNotification? {
            if (!s.exists()) return null
            return AppNotification(
                id = s.key.orEmpty(),
                type = NotificationType.parse(s.strOrNull("type")),
                title = s.str("title"),
                message = s.str("message"),
                relatedAdId = s.str("relatedAdId"),
                relatedRequestId = s.str("relatedRequestId"),
                fromUserId = s.str("fromUserId"),
                fromUserName = s.str("fromUserName"),
                fromUserPhoto = s.strOrNull("fromUserPhoto"),
                timestamp = s.long("timestamp"),
                read = s.bool("read"),
            )
        }
    }
}
