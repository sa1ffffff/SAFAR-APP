package com.safar.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.RatingSummary
import com.safar.app.data.model.Review
import com.safar.app.data.model.Ride
import com.safar.app.data.model.User
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.BottomActionBar
import com.safar.app.ui.components.Card
import com.safar.app.ui.components.EmptyState
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.ReportSheet
import com.safar.app.ui.components.RideCard
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.memberSince
import com.safar.app.ui.relativeAgo
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileState(
    val loading: Boolean = true,
    val user: User? = null,
    val rating: RatingSummary = RatingSummary(),
    val reviews: List<Review> = emptyList(),
    val rides: List<Ride> = emptyList(),
)

class UserProfileViewModel(private val app: AppContainer, val uid: String) : SafarViewModel() {
    val isMe = uid == app.session.uid

    val state: StateFlow<ProfileState> = combine(
        app.users.observe(uid).catch { emit(null) },
        app.users.observeRating(uid).catch { emit(RatingSummary()) },
        app.reviews.observeFor(uid).catch { emit(emptyList()) },
        app.rides.observeRidesBy(uid).map { l -> l.filter { it.isBookable } }.catch { emit(emptyList()) },
    ) { u, r, rev, rides -> ProfileState(false, u, r, rev, rides) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    fun report(reason: String, details: String) = action(onDone = { say("Thanks. We'll look into it.") }) {
        app.reports.report(app.session.uid.orEmpty(), uid, null, reason, details)
    }
}

@Composable
fun UserProfileScreen(uid: String, nav: NavHostController) {
    val vm = appViewModel(key = "user-$uid") { UserProfileViewModel(it, uid) }
    Messages(vm)
    val c = Safar.colors
    val s by vm.state.collectAsStateWithLifecycle()
    var reporting by remember { mutableStateOf(false) }

    ScreenScaffold(
        "", onBack = { nav.back() },
        actions = { if (!vm.isMe && s.user != null) GlassIconButton(Icons.Rounded.Flag, "Report", { reporting = true }) },
        bottomBar = if (!vm.isMe && s.user != null) ({
            BottomActionBar {
                SafarButton("Message ${s.user?.firstName}", { nav.navigate(Routes.chat(uid)) }, modifier = Modifier.weight(1f))
            }
        }) else null,
    ) { padding ->
        val user = s.user
        if (!s.loading && user == null) {
            EmptyState(Icons.Rounded.PersonOff, "Profile not found", "This account may have been deleted.", Modifier.padding(padding))
            return@ScreenScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding(), start = 16.dp, end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(user?.name.orEmpty(), user?.photoUrl, size = 104.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(user?.name.orEmpty(), style = Safar.type.title, color = c.content)
                    Text(memberSince(user?.joinedAt ?: 0), style = Safar.type.label, color = c.contentSecondary)
                    if (!user?.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(user!!.bio, style = Safar.type.body, color = c.content, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Stars(s.rating.average)
                        Text(
                            if (s.rating.count == 0L) "No reviews yet" else "%.1f · %d review%s".format(s.rating.average, s.rating.count, if (s.rating.count == 1L) "" else "s"),
                            style = Safar.type.label, color = c.contentSecondary,
                        )
                    }
                }
            }
            if (s.rides.isNotEmpty()) {
                item { Text("Upcoming rides", style = Safar.type.headline, color = c.content, modifier = Modifier.padding(top = 12.dp)) }
                items(s.rides, key = { it.id }) { RideCard(it, { nav.navigate(Routes.ride(it.id)) }, showDriver = false) }
            }
            item { Text("Reviews", style = Safar.type.headline, color = c.content, modifier = Modifier.padding(top = 12.dp)) }
            if (s.reviews.isEmpty()) item {
                Text("Reviews appear after completed rides.", style = Safar.type.body, color = c.contentSecondary)
            } else items(s.reviews, key = { it.id }) { r ->
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Stars(r.rating.toDouble(), Modifier.weight(1f))
                            Text(relativeAgo(r.timestamp), style = Safar.type.caption, color = c.contentTertiary)
                        }
                        if (r.comment.isNotBlank()) Text(r.comment, style = Safar.type.body, color = c.content)
                        Text(r.reviewerName.substringBefore(' '), style = Safar.type.label, color = c.contentSecondary)
                    }
                }
            }
        }
    }

    if (reporting) {
        ReportSheet("Report ${s.user?.firstName ?: "user"}", onDismiss = { reporting = false }) { reason, details ->
            reporting = false
            vm.report(reason, details)
        }
    }
}

@Composable
fun Stars(value: Double, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 16.dp) {
    val c = Safar.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            Icon(
                if (value >= i + 0.5) Icons.Rounded.Star else Icons.Rounded.StarOutline, null,
                Modifier.size(size), tint = if (value >= i + 0.5) c.content else c.contentTertiary,
            )
        }
    }
}

