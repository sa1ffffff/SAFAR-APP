package com.safar.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.safar.app.data.model.AppNotification
import com.safar.app.data.model.Conversation
import com.safar.app.data.model.JoinRequest
import com.safar.app.data.model.Message
import com.safar.app.data.model.NotificationType
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.RequestStatus
import com.safar.app.data.model.Review
import com.safar.app.data.model.Ride
import com.safar.app.data.model.RideStatus
import com.safar.app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private object Nodes {
    const val USERS = "users"
    const val ADS = "ads"
    const val REQUESTS = "joinRequests"
    const val CONVERSATIONS = "conversations"
    const val USER_CONVERSATIONS = "userConversations"
    const val MESSAGES = "messages"
    const val REVIEWS = "reviews"
    const val RATINGS = "ratings"
    const val NOTIFICATIONS = "notifications"
    const val PUSH_TOKENS = "pushTokens"
    const val REPORTS = "reports"
}

// ─── Auth ────────────────────────────────────────────────────────────────────

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUid: String? get() = auth.currentUser?.uid

    /** Emits the signed-in uid, or null when signed out. */
    val uid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    suspend fun signIn(credential: AuthCredential): String {
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid ?: error("Sign-in failed. Please try again.")
    }

    val email: String get() = auth.currentUser?.email.orEmpty()
    val displayName: String get() = auth.currentUser?.displayName.orEmpty()
    val photoUrl: String? get() = auth.currentUser?.photoUrl?.toString()

    fun signOut() = auth.signOut()

    suspend fun deleteAccount() {
        auth.currentUser?.delete()?.await()
    }

    /** Firebase only allows deleting an account signed in within roughly the last five minutes. */
    fun signedInRecently(windowMs: Long = 5 * 60 * 1000L): Boolean {
        val last = auth.currentUser?.metadata?.lastSignInTimestamp ?: return false
        return System.currentTimeMillis() - last < windowMs
    }
}

// ─── Users ───────────────────────────────────────────────────────────────────

class UserRepository(
    private val db: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val context: Context,
) {
    private fun ref(uid: String) = db.getReference(Nodes.USERS).child(uid)

    fun observe(uid: String): Flow<User?> = ref(uid).snapshots().map { User.from(it) }

    suspend fun get(uid: String): User? = User.from(ref(uid).get().await())

    suspend fun save(user: User) {
        ref(user.uid).updateChildren(user.toMap()).await()
    }

    fun observeRating(uid: String): Flow<RatingSummary> =
        db.getReference(Nodes.RATINGS).child(uid).snapshots().map {
            RatingSummary(
                sum = (it.child("sum").value as? Number)?.toLong() ?: 0,
                count = (it.child("count").value as? Number)?.toLong() ?: 0,
            )
        }

    /** Ratings for several users at once (e.g. every driver in a ride list). */
    fun observeRatings(uids: Set<String>): Flow<Map<String, RatingSummary>> =
        if (uids.isEmpty()) flowOf(emptyMap())
        else combine(uids.map { uid -> observeRating(uid).map { uid to it } }) { it.toMap() }

    /** Uploads a square-cropped, compressed avatar and returns its download URL. */
    suspend fun uploadAvatar(uid: String, uri: Uri): String {
        val bytes = withContext(Dispatchers.IO) { compressAvatar(uri) }
        val file = storage.reference.child("profile_pictures/$uid.jpg")
        file.putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        val url = file.downloadUrl.await().toString()
        ref(uid).child("photoUrl").setValue(url).await()
        return url
    }

    private fun compressAvatar(uri: Uri): ByteArray {
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Couldn't read that image.")
        val side = minOf(source.width, source.height)
        val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    suspend fun registerPushToken(uid: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            tokenRef(uid).setValue(token).await()
        }
    }

    fun savePushToken(uid: String, token: String) {
        tokenRef(uid).setValue(token)
    }

    /** Owner-only node; the Cloud Function reads it with admin rights. */
    private fun tokenRef(uid: String) = db.getReference(Nodes.PUSH_TOKENS).child(uid)

    /**
     * Removes everything the user owns. Rules allow deleting userConversations entries
     * one conversation at a time, not the whole node.
     */
    suspend fun deleteAllData(uid: String) {
        val ads = db.getReference(Nodes.ADS).orderByChild("creatorUid").equalTo(uid).get().await()
        ads.children.forEach { it.ref.removeValue().await() }
        val convos = db.getReference(Nodes.USER_CONVERSATIONS).child(uid).get().await()
        convos.children.forEach { it.ref.removeValue().await() }
        tokenRef(uid).removeValue().await()
        db.getReference(Nodes.NOTIFICATIONS).child(uid).removeValue().await()
        ref(uid).removeValue().await()
        runCatching { storage.reference.child("profile_pictures/$uid.jpg").delete().await() }
    }
}

// ─── Rides ───────────────────────────────────────────────────────────────────

class RideRepository(private val db: FirebaseDatabase) {
    private val ads get() = db.getReference(Nodes.ADS)

    /** Open, unexpired rides with seats left, soonest departure first. */
    /** Only rides departing from two hours ago onward are downloaded (indexed on departureTime). */
    fun observeOpenRides(): Flow<List<Ride>> = ads.orderByChild("departureTime")
        .startAt((System.currentTimeMillis() - 2 * 60 * 60 * 1000L).toDouble())
        .snapshots().map { snap ->
        snap.children.mapNotNull { Ride.from(it) }
            .filter { it.isBookable }
            .sortedBy { it.departureTime }
    }

    fun observeRidesBy(uid: String): Flow<List<Ride>> =
        ads.orderByChild("creatorUid").equalTo(uid).snapshots().map { snap ->
            snap.children.mapNotNull { Ride.from(it) }.sortedByDescending { it.departureTime }
        }

    fun observe(id: String): Flow<Ride?> = ads.child(id).snapshots().map { Ride.from(it) }

    suspend fun get(id: String): Ride? = Ride.from(ads.child(id).get().await())

    suspend fun post(ride: Ride): String {
        val id = ads.push().key ?: error("Couldn't reach the server.")
        val now = System.currentTimeMillis()
        val toSave = ride.copy(
            id = id,
            status = RideStatus.OPEN,
            createdAt = now,
            // Expire a day after departure so riders can still find it, capped at the window.
            expiresAt = maxOf(ride.departureTime + 24 * 60 * 60 * 1000L, now + Ride.EXPIRY_WINDOW_MS),
        )
        ads.child(id).setValue(toSave.toMap()).await()
        return id
    }

    suspend fun update(ride: Ride) {
        ads.child(ride.id).updateChildren(ride.toMap()).await()
    }

    suspend fun setStatus(id: String, status: RideStatus) {
        ads.child(id).child("status").setValue(status.name).await()
    }

    suspend fun delete(id: String) {
        ads.child(id).removeValue().await()
    }
}

// ─── Join requests ───────────────────────────────────────────────────────────

class RequestRepository(
    private val db: FirebaseDatabase,
    private val notifications: NotificationRepository,
) {
    private val requests get() = db.getReference(Nodes.REQUESTS)

    fun observeIncoming(uid: String): Flow<List<JoinRequest>> =
        requests.orderByChild("adCreatorUid").equalTo(uid).snapshots().map(::parse)

    fun observeOutgoing(uid: String): Flow<List<JoinRequest>> =
        requests.orderByChild("requestorUid").equalTo(uid).snapshots().map(::parse)

    fun observe(id: String): Flow<JoinRequest?> = requests.child(id).snapshots().map { JoinRequest.from(it) }

    private fun parse(snap: com.google.firebase.database.DataSnapshot) =
        snap.children.mapNotNull { JoinRequest.from(it) }.sortedByDescending { it.timestamp }

    suspend fun send(ride: Ride, me: User, seats: Int) {
        val id = requests.push().key ?: error("Couldn't reach the server.")
        val request = JoinRequest(
            id = id,
            adId = ride.id,
            requestorUid = me.uid,
            requestorName = me.name,
            requestorPhone = me.phone,
            requestorPhotoUrl = me.photoUrl,
            adCreatorUid = ride.creatorUid,
            seats = seats,
            routeLabel = "${ride.origin.name} → ${ride.destination.name}",
            status = RequestStatus.PENDING,
            timestamp = System.currentTimeMillis(),
        )
        requests.child(id).setValue(request.toMap()).await()
        notifications.send(
            to = ride.creatorUid, from = me, type = NotificationType.NEW_REQUEST,
            title = "New seat request",
            message = "${me.firstName} wants ${seatsLabel(seats)} on ${request.routeLabel}",
            adId = ride.id, requestId = id,
        )
    }

    suspend fun cancel(request: JoinRequest) {
        requests.child(request.id).child("status").setValue(RequestStatus.CANCELLED.name).await()
    }

    /**
     * Accepts a request and reserves its seats in one transaction on the ride,
     * so two drivers' taps or two riders can't overbook.
     */
    suspend fun accept(request: JoinRequest, me: User) {
        db.getReference(Nodes.ADS).child(request.adId).transact { ride ->
            if (ride.value == null) throw TransactionAbortedException("This ride no longer exists.")
            val left = (ride.child("availableSeats").value as? Number)?.toInt() ?: 0
            if (left < request.seats) throw TransactionAbortedException("Only ${seatsLabel(left)} left on this ride.")
            ride.child("availableSeats").value = left - request.seats
            if (left - request.seats == 0) ride.child("status").value = RideStatus.FILLED.name
        }
        requests.child(request.id).child("status").setValue(RequestStatus.ACCEPTED.name).await()
        notifications.send(
            to = request.requestorUid, from = me, type = NotificationType.REQUEST_ACCEPTED,
            title = "You're in!",
            message = "${me.firstName} accepted your request for ${request.routeLabel.ifBlank { "the ride" }}",
            adId = request.adId, requestId = request.id,
        )
    }

    suspend fun decline(request: JoinRequest, me: User) {
        requests.child(request.id).child("status").setValue(RequestStatus.REJECTED.name).await()
        notifications.send(
            to = request.requestorUid, from = me, type = NotificationType.REQUEST_DECLINED,
            title = "Request declined",
            message = "${me.firstName} couldn't take you on ${request.routeLabel.ifBlank { "this ride" }}",
            adId = request.adId, requestId = request.id,
        )
    }

    suspend fun markComplete(request: JoinRequest, me: User) {
        val field = if (me.uid == request.adCreatorUid) "creatorMarkedComplete" else "requesterMarkedComplete"
        requests.child(request.id).child(field).setValue(true).await()
        notifications.send(
            to = request.otherParty(me.uid), from = me, type = NotificationType.RIDE_COMPLETED,
            title = "Ride marked complete",
            message = "${me.firstName} marked ${request.routeLabel.ifBlank { "your ride" }} as complete. Confirm and leave a review.",
            adId = request.adId, requestId = request.id,
        )
    }
}

fun seatsLabel(n: Int) = if (n == 1) "1 seat" else "$n seats"

// ─── Chat ────────────────────────────────────────────────────────────────────

class ChatRepository(private val db: FirebaseDatabase) {
    private val conversations get() = db.getReference(Nodes.CONVERSATIONS)
    private val index get() = db.getReference(Nodes.USER_CONVERSATIONS)
    private val messages get() = db.getReference(Nodes.MESSAGES)

    /**
     * Conversations are listed through /userConversations/{uid}, an index of ids the user
     * belongs to. Reading /conversations as a whole is denied by the security rules.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeConversations(uid: String): Flow<List<Conversation>> =
        index.child(uid).snapshots()
            .map { snap -> snap.children.mapNotNull { it.key } }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyList())
                else combine(ids.map { id -> conversations.child(id).snapshots().map { Conversation.from(it) } }) { all ->
                    all.filterNotNull().sortedByDescending { it.lastMessageTimestamp }
                }
            }

    fun observeUnreadTotal(uid: String): Flow<Long> =
        observeConversations(uid).map { list -> list.sumOf { it.unreadFor(uid) } }

    fun observeConversation(id: String): Flow<Conversation?> =
        conversations.child(id).snapshots().map { Conversation.from(it) }

    fun observeMessages(conversationId: String): Flow<List<Message>> =
        messages.child(conversationId).orderByChild("timestamp").limitToLast(200).snapshots().map { snap ->
            snap.children.mapNotNull { Message.from(it) }
        }

    /** Creates the conversation (if needed) and returns its id. */
    suspend fun open(me: User, other: User): String {
        val id = Conversation.idFor(me.uid, other.uid)
        val updates = mutableMapOf<String, Any?>(
            "${Nodes.CONVERSATIONS}/$id/conversationId" to id,
            "${Nodes.CONVERSATIONS}/$id/participants/${me.uid}" to true,
            "${Nodes.CONVERSATIONS}/$id/participants/${other.uid}" to true,
            "${Nodes.CONVERSATIONS}/$id/participantInfo/${me.uid}/name" to me.name,
            "${Nodes.CONVERSATIONS}/$id/participantInfo/${me.uid}/photoUrl" to me.photoUrl,
            "${Nodes.CONVERSATIONS}/$id/participantInfo/${other.uid}/name" to other.name,
            "${Nodes.CONVERSATIONS}/$id/participantInfo/${other.uid}/photoUrl" to other.photoUrl,
            "${Nodes.USER_CONVERSATIONS}/${me.uid}/$id" to true,
            "${Nodes.USER_CONVERSATIONS}/${other.uid}/$id" to true,
        )
        db.reference.updateChildren(updates).await()
        return id
    }

    suspend fun send(conversationId: String, me: User, otherUid: String, text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val msgRef = messages.child(conversationId).push()
        val now = System.currentTimeMillis()
        val message = Message(msgRef.key.orEmpty(), me.uid, me.name, body, now)
        val updates = mapOf<String, Any?>(
            "${Nodes.MESSAGES}/$conversationId/${msgRef.key}" to message.toMap(),
            "${Nodes.CONVERSATIONS}/$conversationId/lastMessage" to body.take(200),
            "${Nodes.CONVERSATIONS}/$conversationId/lastMessageTimestamp" to now,
            "${Nodes.CONVERSATIONS}/$conversationId/lastMessageSenderId" to me.uid,
            "${Nodes.CONVERSATIONS}/$conversationId/unreadCount/$otherUid" to ServerValue.increment(1),
        )
        db.reference.updateChildren(updates).await()
    }

    fun markRead(conversationId: String, uid: String) {
        conversations.child(conversationId).child("unreadCount").child(uid).setValue(0)
    }
}

// ─── Reviews ─────────────────────────────────────────────────────────────────

class ReviewRepository(
    private val db: FirebaseDatabase,
    private val notifications: NotificationRepository,
) {
    fun observeFor(uid: String): Flow<List<Review>> =
        db.getReference(Nodes.REVIEWS).orderByChild("revieweeId").equalTo(uid).snapshots().map { snap ->
            snap.children.mapNotNull { Review.from(it) }.sortedByDescending { it.timestamp }
        }

    suspend fun submit(request: JoinRequest, me: User, rating: Int, comment: String) {
        require(rating in 1..5) { "Pick a rating from 1 to 5 stars." }
        check(request.canReview(me.uid)) { "This ride can't be reviewed yet." }
        val revieweeId = request.otherParty(me.uid)
        val reviewRef = db.getReference(Nodes.REVIEWS).push()
        val review = Review(
            id = reviewRef.key.orEmpty(),
            rideAdId = request.adId,
            joinRequestId = request.id,
            reviewerId = me.uid,
            reviewerName = me.name,
            revieweeId = revieweeId,
            rating = rating,
            comment = comment.trim(),
            timestamp = System.currentTimeMillis(),
        )
        val reviewedFlag = if (me.uid == request.adCreatorUid) "creatorReviewedRequester" else "requesterReviewedCreator"
        db.reference.updateChildren(
            mapOf(
                "${Nodes.REVIEWS}/${review.id}" to review.toMap(),
                "${Nodes.REQUESTS}/${request.id}/$reviewedFlag" to true,
            )
        ).await()
        db.getReference(Nodes.RATINGS).child(revieweeId).transact { node ->
            val sum = (node.child("sum").value as? Number)?.toLong() ?: 0
            val count = (node.child("count").value as? Number)?.toLong() ?: 0
            node.child("sum").value = sum + rating
            node.child("count").value = count + 1
        }
        notifications.send(
            to = revieweeId, from = me, type = NotificationType.NEW_REVIEW,
            title = "New review",
            message = "${me.firstName} gave you $rating★",
            adId = request.adId, requestId = request.id,
        )
    }
}

// ─── In-app notifications ────────────────────────────────────────────────────

class NotificationRepository(private val db: FirebaseDatabase) {
    private fun inbox(uid: String): DatabaseReference = db.getReference(Nodes.NOTIFICATIONS).child(uid)

    fun observe(uid: String): Flow<List<AppNotification>> =
        inbox(uid).orderByChild("timestamp").limitToLast(100).snapshots().map { snap ->
            snap.children.mapNotNull { AppNotification.from(it) }.sortedByDescending { it.timestamp }
        }

    fun observeUnread(uid: String): Flow<Int> = observe(uid).map { list -> list.count { !it.read } }

    suspend fun send(
        to: String, from: User, type: NotificationType,
        title: String, message: String, adId: String = "", requestId: String = "",
    ) {
        if (to.isBlank() || to == from.uid) return
        val ref = inbox(to).push()
        val notification = AppNotification(
            id = ref.key.orEmpty(), type = type, title = title, message = message,
            relatedAdId = adId, relatedRequestId = requestId,
            fromUserId = from.uid, fromUserName = from.name, fromUserPhoto = from.photoUrl,
            timestamp = System.currentTimeMillis(), read = false,
        )
        // A notification is a side effect; failing to deliver it must not fail the action.
        runCatching { ref.setValue(notification.toMap()).await() }
    }

    fun markRead(uid: String, id: String) {
        inbox(uid).child(id).child("read").setValue(true)
    }

    suspend fun markAllRead(uid: String, items: List<AppNotification>) {
        val updates = items.filter { !it.read }.associate { "${it.id}/read" to true }
        if (updates.isNotEmpty()) inbox(uid).updateChildren(updates).await()
    }

    suspend fun clear(uid: String) {
        inbox(uid).removeValue().await()
    }
}

// ─── Reports ─────────────────────────────────────────────────────────────────

/** Write-only safety reports, reviewed from the Firebase console. */
class ReportRepository(private val db: FirebaseDatabase) {
    suspend fun report(reporterUid: String, targetUid: String, rideId: String?, reason: String, details: String) {
        val ref = db.getReference(Nodes.REPORTS).push()
        ref.setValue(
            mapOf(
                "reportId" to ref.key,
                "reporterId" to reporterUid,
                "targetUserId" to targetUid,
                "targetRideId" to rideId,
                "reason" to reason,
                "details" to details.trim().take(1000),
                "timestamp" to System.currentTimeMillis(),
            )
        ).await()
    }
}
