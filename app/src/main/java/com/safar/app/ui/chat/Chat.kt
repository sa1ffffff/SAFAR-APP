package com.safar.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.Message
import com.safar.app.data.model.User
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.Glass
import com.safar.app.ui.components.GlassIconButton
import com.safar.app.ui.components.LocalHaze
import com.safar.app.ui.components.TopBarHeight
import com.safar.app.ui.components.pressable
import com.safar.app.ui.dayLabel
import com.safar.app.ui.theme.Safar
import com.safar.app.ui.timeLabel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val app: AppContainer, private val otherUid: String) : SafarViewModel() {
    private val me = app.session.requireUser()
    private val conversationId = MutableStateFlow<String?>(null)

    val other: StateFlow<User?> = app.users.observe(otherUid).catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val thread: StateFlow<List<Message>?> = conversationId.filterNotNull()
        .flatMapLatest { id -> app.chat.observeMessages(id) }
        .onEach { conversationId.value?.let { app.chat.markRead(it, me.uid) } }
        .catch { say("Couldn't load messages."); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val draft = MutableStateFlow("")
    val myUid = me.uid

    init {
        action {
            val other = app.users.get(otherUid) ?: error("This person's account no longer exists.")
            conversationId.value = app.chat.open(me, other)
        }
    }

    fun send() {
        val id = conversationId.value ?: return
        val text = draft.value.trim()
        if (text.isEmpty()) return
        draft.value = ""
        action(onError = { draft.value = text }) { app.chat.send(id, me, otherUid, text) }
    }
}

private sealed interface ChatItem {
    data class Day(val label: String) : ChatItem
    data class Bubble(val message: Message, val mine: Boolean, val firstInGroup: Boolean, val lastInGroup: Boolean) : ChatItem
}

private fun group(messages: List<Message>, myUid: String): List<ChatItem> {
    val out = mutableListOf<ChatItem>()
    var lastDay = ""
    messages.forEachIndexed { i, m ->
        val day = dayLabel(m.timestamp)
        if (day != lastDay) { out += ChatItem.Day(day); lastDay = day }
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        val close = { a: Message?, b: Message -> a != null && a.senderId == b.senderId && kotlin.math.abs(a.timestamp - b.timestamp) < 5 * 60_000 && dayLabel(a.timestamp) == day }
        out += ChatItem.Bubble(m, m.senderId == myUid, !close(prev, m), !close(next, m))
    }
    return out
}

@Composable
fun ChatScreen(otherUid: String, nav: NavHostController) {
    val vm = appViewModel(key = "chat-$otherUid") { ChatViewModel(it, otherUid) }
    Messages(vm)
    val c = Safar.colors
    val other by vm.other.collectAsStateWithLifecycle()
    val messages by vm.thread.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val items = remember(messages) { group(messages.orEmpty(), vm.myUid).asReversed() }
    val list = rememberLazyListState()
    val haze = remember { HazeState() }
    LaunchedEffect(messages?.size) { if (list.firstVisibleItemIndex <= 2) list.animateScrollToItem(0) }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight
    CompositionLocalProvider(LocalHaze provides haze) {
        Box(Modifier.fillMaxSize().background(c.background).imePadding()) {
            LazyColumn(
                Modifier.fillMaxSize().haze(haze),
                state = list,
                reverseLayout = true,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = top + 8.dp, bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                items(items, key = { if (it is ChatItem.Bubble) it.message.id else (it as ChatItem.Day).label }) { item ->
                    when (item) {
                        is ChatItem.Day -> Text(
                            item.label, style = Safar.type.caption, color = c.contentTertiary,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        is ChatItem.Bubble -> Bubble(item)
                    }
                }
                if (messages != null) item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 24.dp).clip(RoundedCornerShape(14.dp)).background(c.surfaceMuted).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Shield, null, Modifier.size(18.dp), tint = c.content)
                        Text("Keep chats in Safar. Meet in public and never pay in advance.", style = Safar.type.caption, color = c.contentSecondary)
                    }
                }
            }

            // Header
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(TopBarHeight).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { nav.back() })
                Glass(shape = CircleShape, elevation = 6.dp) {
                    Row(
                        Modifier.clickable { nav.navigate(Routes.user(otherUid)) }.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Avatar(other?.name.orEmpty(), other?.photoUrl, size = 36.dp)
                        Text(other?.name ?: " ", style = Safar.type.bodyStrong, color = c.content, maxLines = 1)
                    }
                }
            }

            // Composer
            Glass(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 8.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Row(Modifier.padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f).heightIn(min = 44.dp).padding(vertical = 11.dp)) {
                        if (draft.isEmpty()) Text("Message", style = Safar.type.body, color = c.contentTertiary)
                        BasicTextField(
                            draft, { vm.draft.value = it.take(2000) },
                            textStyle = Safar.type.body.copy(color = c.content), cursorBrush = SolidColor(c.content),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    AnimatedVisibility(
                        draft.isNotBlank(),
                        enter = scaleIn(tween(160), initialScale = 0.6f) + fadeIn(tween(120)),
                        exit = scaleOut(tween(120), targetScale = 0.6f) + fadeOut(tween(100)),
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(c.primary)
                                .pressable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = vm::send, pressedScale = 0.9f),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Rounded.Send, "Send", Modifier.size(20.dp), tint = c.onPrimary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bubble(item: ChatItem.Bubble) {
    val c = Safar.colors
    val big = 20.dp
    val small = 6.dp
    val shape = if (item.mine) RoundedCornerShape(big, if (item.firstInGroup) big else small, if (item.lastInGroup) small else small, big)
    else RoundedCornerShape(if (item.firstInGroup) big else small, big, big, if (item.lastInGroup) small else small)
    Column(
        Modifier.fillMaxWidth().padding(top = if (item.firstInGroup) 8.dp else 2.dp),
        horizontalAlignment = if (item.mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier.widthIn(max = 300.dp).clip(shape).background(if (item.mine) c.primary else c.surfaceMuted)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(item.message.text, style = Safar.type.body, color = if (item.mine) c.onPrimary else c.content)
        }
        if (item.lastInGroup) {
            Spacer(Modifier.height(3.dp))
            Text(timeLabel(item.message.timestamp), style = Safar.type.caption, color = c.contentTertiary, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}
