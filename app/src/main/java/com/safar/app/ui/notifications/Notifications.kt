package com.safar.app.ui.notifications

import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiPeople
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.AppNotification
import com.safar.app.data.model.NotificationType
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.EmptyState
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.relativeAgo
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(private val app: AppContainer) : SafarViewModel() {
    private val uid = app.session.uid.orEmpty()
    val items: StateFlow<List<AppNotification>?> = app.notifications.observe(uid).catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun open(n: AppNotification) { if (!n.read) app.notifications.markRead(uid, n.id) }
    fun markAll() = action { app.notifications.markAllRead(uid, items.value.orEmpty()) }
}

@Composable
fun NotificationsScreen(nav: NavHostController) {
    val vm = appViewModel { NotificationsViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val items by vm.items.collectAsStateWithLifecycle()
    ScreenScaffold(
        "Notifications", onBack = { nav.back() },
        actions = { if (items.orEmpty().any { !it.read }) GlassIconButton(Icons.Rounded.DoneAll, "Mark all read", vm::markAll) },
    ) { padding ->
        val list = items
        if (list != null && list.isEmpty()) {
            EmptyState(Icons.Rounded.NotificationsNone, "You're all caught up", "Seat requests, confirmations and reviews show up here.", Modifier.padding(padding))
            return@ScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            items(list.orEmpty(), key = { it.id }) { n ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        vm.open(n)
                        when {
                            n.type == NotificationType.RIDE_COMPLETED && n.relatedRequestId.isNotBlank() -> nav.navigate(Routes.ride(n.relatedAdId))
                            n.type == NotificationType.NEW_REVIEW -> nav.navigate(Routes.user(n.fromUserId.ifBlank { return@clickable }))
                            n.relatedAdId.isNotBlank() -> nav.navigate(Routes.ride(n.relatedAdId))
                            n.fromUserId.isNotBlank() -> nav.navigate(Routes.chat(n.fromUserId))
                        }
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
                        Icon(iconFor(n.type), null, Modifier.size(20.dp), tint = c.content)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(n.title, style = Safar.type.bodyStrong, color = c.content)
                        Text(n.message, style = Safar.type.label, color = c.contentSecondary)
                        Text(relativeAgo(n.timestamp), style = Safar.type.caption, color = c.contentTertiary, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (!n.read) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(c.info))
                }
            }
        }
    }
}

private fun iconFor(type: NotificationType) = when (type) {
    NotificationType.NEW_REQUEST -> Icons.Rounded.EmojiPeople
    NotificationType.REQUEST_ACCEPTED -> Icons.Rounded.CheckCircle
    NotificationType.REQUEST_DECLINED -> Icons.Rounded.Cancel
    NotificationType.RIDE_STARTED -> Icons.Rounded.DirectionsCar
    NotificationType.RIDE_COMPLETED -> Icons.Rounded.Flag
    NotificationType.NEW_REVIEW -> Icons.Rounded.Star
    NotificationType.NEW_MESSAGE -> Icons.Rounded.ChatBubble
}
