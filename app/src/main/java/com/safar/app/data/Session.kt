package com.safar.app.data

import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where the app should start, derived from onboarding, auth and profile state. */
sealed interface Gate {
    data object Loading : Gate
    data object Welcome : Gate
    data object SignedOut : Gate
    data class NeedsTerms(val uid: String) : Gate
    data class NeedsProfile(val uid: String) : Gate
    data class Ready(val user: User) : Gate
}

/** App-wide view of the signed-in user, shared by every screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class Session(
    private val auth: AuthRepository,
    private val users: UserRepository,
    private val chat: ChatRepository,
    private val notifications: NotificationRepository,
    preferences: Preferences,
    private val scope: CoroutineScope,
) {
    private sealed interface ProfileState {
        data object Loading : ProfileState
        data object SignedOut : ProfileState
        data class Loaded(val uid: String, val user: User?) : ProfileState
    }

    private val profile = auth.uid.flatMapLatest { uid ->
        if (uid == null) flowOf(ProfileState.SignedOut)
        else users.observe(uid)
            .map<User?, ProfileState> { ProfileState.Loaded(uid, it) }
            .catch { emit(ProfileState.Loaded(uid, null)) }
    }

    val gate: StateFlow<Gate> = combine(preferences.onboarded, profile) { onboarded, p ->
        when {
            !onboarded -> Gate.Welcome
            p is ProfileState.Loading -> Gate.Loading
            p is ProfileState.SignedOut -> Gate.SignedOut
            // Everyone agrees to the current terms first, including accounts made before they existed.
            p is ProfileState.Loaded && (p.user?.termsVersion ?: 0) < com.safar.app.ui.terms.TERMS_VERSION -> Gate.NeedsTerms(p.uid)
            // Sign-in is Google-only, so the phone number riders call is collected right after.
            p is ProfileState.Loaded && (p.user == null || p.user.name.isBlank() || !p.user.hasCallablePhone) -> Gate.NeedsProfile(p.uid)
            p is ProfileState.Loaded -> Gate.Ready(p.user!!)
            else -> Gate.Loading
        }
    }
        .distinctUntilChanged()
        .onEach { if (it is Gate.Ready) scope.launch { users.registerPushToken(it.user.uid) } }
        .stateIn(scope, SharingStarted.Eagerly, Gate.Loading)

    val user: StateFlow<User?> = gate
        .map { (it as? Gate.Ready)?.user }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** The signed-in user; screens behind the gate can rely on it being present. */
    fun requireUser(): User = user.value ?: error("Not signed in")

    val uid: String? get() = auth.currentUid

    val unreadMessages: StateFlow<Long> = user.map { it?.uid }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) flowOf(0L) else chat.observeUnreadTotal(uid).catch { emit(0L) }
    }.stateIn(scope, SharingStarted.Eagerly, 0L)

    val unreadNotifications: StateFlow<Int> = user.map { it?.uid }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) flowOf(0) else notifications.observeUnread(uid).catch { emit(0) }
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    val myRating: StateFlow<RatingSummary> = user.map { it?.uid }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) flowOf(RatingSummary()) else users.observeRating(uid).catch { emit(RatingSummary()) }
    }.stateIn(scope, SharingStarted.Eagerly, RatingSummary())
}
