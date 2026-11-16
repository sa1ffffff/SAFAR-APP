package com.safar.app.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.Conversation
import com.safar.app.data.model.JoinRequest
import com.safar.app.data.model.RequestStatus
import com.safar.app.data.seatsLabel
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.ButtonKind
import com.safar.app.ui.components.Card
import com.safar.app.ui.components.EmptyState
import com.safar.app.ui.components.Pill
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.Segmented
import com.safar.app.ui.components.Skeleton
import com.safar.app.ui.relativeAgo
import com.safar.app.ui.shortStamp
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InboxState(
    val conversations: List<Conversation>? = null,
    val requests: List<JoinRequest>? = null,
    val busy: Set<String> = emptySet(),
)

class InboxViewModel(private val app: AppContainer) : SafarViewModel() {
    val uid = app.session.uid.orEmpty()
    private val busy = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<InboxState> = combine(
        app.chat.observeConversations(uid).catch { emit(emptyList()) },
        app.requests.observeIncoming(uid).map { list -> list.filter { it.status != RequestStatus.CANCELLED } }.catch { emit(emptyList()) },
        busy,
    ) { convos, requests, b -> InboxState(convos, requests, b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxState())

    fun accept(r: JoinRequest) = respond(r) { app.requests.accept(r, app.session.requireUser()) }
    fun decline(r: JoinRequest) = respond(r) { app.requests.decline(r, app.session.requireUser()) }

    private fun respond(r: JoinRequest, block: suspend () -> Unit) {
        busy.value += r.id
        action(onDone = { busy.value -= r.id }, onError = { busy.value -= r.id }) { block() }
    }
}

@Composable
fun InboxTab(nav: NavHostController) {
    val vm = appViewModel { InboxViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val pending = state.requests?.count { it.status == RequestStatus.PENDING } ?: 0
    val bottom = TabBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp

    LazyColumn(
        Modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(bottom = bottom),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Text("Inbox", style = Safar.type.display, color = c.content)
                Spacer(Modifier.height(16.dp))
                Segmented(listOf("Messages", if (pending > 0) "Requests · $pending" else "Requests"), segment, { segment = it })
            }
        }
        if (segment == 0) {
            val list = state.conversations
            when {
                list == null -> items(4) { ConversationSkeleton() }
                list.isEmpty() -> item {
                    EmptyState(Icons.Rounded.ChatBubbleOutline, "No messages yet", "Message a driver from any ride to ask about pickup or timing.")
                }
                else -> items(list, key = { it.id }) { convo ->
                    ConversationRow(convo, vm.uid) { nav.navigate(Routes.chat(convo.otherUid(vm.uid))) }
                }
            }
        } else {
            val list = state.requests
            when {
                list == null -> items(2) { Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { ConversationSkeleton() } }
                list.isEmpty() -> item {
                    EmptyState(Icons.Rounded.Inbox, "No seat requests", "When riders ask to join your rides, you'll accept or decline them here.")
                }
                else -> items(list, key = { it.id }) { r ->
                    RequestCard(
                        r, busy = r.id in state.busy,
                        onAccept = { vm.accept(r) }, onDecline = { vm.decline(r) },
                        onMessage = { nav.navigate(Routes.chat(r.requestorUid)) },
                        onProfile = { nav.navigate(Routes.user(r.requestorUid)) },
                        onRide = { nav.navigate(Routes.ride(r.adId)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(convo: Conversation, myUid: String, onClick: () -> Unit) {
    val c = Safar.colors
    val other = convo.other(myUid)
    val unread = convo.unreadFor(myUid)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(other.name, other.photoUrl, size = 52.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(other.name.ifBlank { "Safar user" }, style = Safar.type.bodyStrong, color = c.content, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(shortStamp(convo.lastMessageTimestamp), style = Safar.type.caption, color = if (unread > 0) c.content else c.contentTertiary)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (convo.lastMessageSenderId == myUid) "You: " else ""
                Text(
                    prefix + convo.lastMessage.ifBlank { "Say salam 👋" },
                    style = if (unread > 0) Safar.type.bodyStrong else Safar.type.body,
                    color = if (unread > 0) c.content else c.contentSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (unread > 0) {
                    Box(
                        Modifier.padding(start = 8.dp).widthIn(min = 20.dp).height(20.dp).clip(CircleShape).background(c.info).padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (unread > 9) "9+" else "$unread", style = Safar.type.caption, color = androidx.compose.ui.graphics.Color.White) }
                }
            }
        }
    }
}

@Composable
private fun ConversationSkeleton() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Safar.colors.surfaceMuted))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            Skeleton(Modifier.fillMaxWidth(0.4f).height(14.dp))
            Skeleton(Modifier.fillMaxWidth(0.75f).height(12.dp))
        }
    }
}

@Composable
fun RequestCard(
    r: JoinRequest,
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit,
    onProfile: () -> Unit,
    onRide: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = Safar.colors
    Card(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(CircleShape).clickable(onClick = onProfile)) { Avatar(r.requestorName, r.requestorPhotoUrl, size = 44.dp) }
                Column(Modifier.weight(1f)) {
                    Text(r.requestorName.ifBlank { "Rider" }, style = Safar.type.bodyStrong, color = c.content)
                    Text("${seatsLabel(r.seats)} · ${relativeAgo(r.timestamp)}", style = Safar.type.label, color = c.contentSecondary)
                }
                when (r.status) {
                    RequestStatus.ACCEPTED -> Pill("Accepted", background = c.positiveMuted, color = c.positive)
                    RequestStatus.REJECTED -> Pill("Declined")
                    else -> {}
                }
            }
            if (r.routeLabel.isNotBlank()) {
                Text(
                    com.safar.app.ui.shortRoute(r.routeLabel), style = Safar.type.label, color = c.content,
                    modifier = if (onRide != null) Modifier.clip(CircleShape).clickable(onClick = onRide) else Modifier,
                )
            }
            when (r.status) {
                RequestStatus.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SafarButton("Decline", onDecline, kind = ButtonKind.Secondary, enabled = !busy, height = 46.dp, modifier = Modifier.weight(1f))
                    SafarButton("Accept", onAccept, loading = busy, height = 46.dp, modifier = Modifier.weight(1f))
                }
                RequestStatus.ACCEPTED -> SafarButton("Message ${r.requestorName.substringBefore(' ')}", onMessage, kind = ButtonKind.Secondary, height = 46.dp, modifier = Modifier.fillMaxWidth())
                else -> {}
            }
        }
    }
}
