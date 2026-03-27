package com.safar.app.views.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.safar.app.models.ChatMessage
import com.safar.app.models.Conversation
import com.safar.app.models.JoinRequest
import com.safar.app.presenters.ConversationPresenter
import com.safar.app.presenters.RequestPresenter
import com.safar.app.utils.AppConstants
import com.safar.app.utils.DateUtils
import com.safar.app.views.activities.AdDetailActivity
import com.safar.app.views.activities.ChatActivity
import com.safar.app.views.ui.theme.SafarTheme
import com.safar.app.views.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Premium Inbox Screen
// Material 3 Design with modern notifications and chat preview
// ══════════════════════════════════════════════════════════════════════════════

data class InboxUiState(
    val isLoading: Boolean = true,
    val items: List<InboxItem> = emptyList()
)

enum class InboxItemType {
    CHAT, JOIN_REQUEST
}

data class InboxItem(
    val id: String,
    val type: InboxItemType,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val chatRoomId: String = "",
    val peerName: String = "",
    val peerUid: String = "",  // Added for direct messaging
    val peerPhotoUrl: String? = null,  // Added for avatar
    val adId: String = "",
    val statusLabel: String = "",
    val isUnread: Boolean = false,
    val requestId: String = ""
)

class InboxStateHolder {
    var state by mutableStateOf(InboxUiState())
        private set

    fun setLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }

    fun setItems(items: List<InboxItem>) {
        state = state.copy(items = items, isLoading = false)
    }
}

class InboxFragment : Fragment(), RequestPresenter.RequestView, ConversationPresenter.ConversationView {

    private val stateHolder = InboxStateHolder()
    private var requestPresenter: RequestPresenter? = null
    private var conversationPresenter: ConversationPresenter? = null
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid
    
    // Track loaded items from both sources
    private var requestItems = listOf<InboxItem>()
    private var conversationItems = listOf<InboxItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SafarTheme(darkTheme = true) {
                InboxScreen(
                    state = stateHolder.state,
                    onChatOpen = { chatRoomId, peerName, requestId, peerUid -> openChat(chatRoomId, peerName, requestId, peerUid) },
                    onRequestOpen = { adId -> openAdDetail(adId) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPresenter = RequestPresenter(this)
        conversationPresenter = ConversationPresenter(this)
        
        currentUid?.let { uid ->
            stateHolder.setLoading(true)
            // Load both ride requests and direct conversations
            requestPresenter?.loadIncomingRequests(uid)
            conversationPresenter?.loadConversations(uid)
        }
    }

    private fun openChat(chatRoomId: String, peerName: String, requestId: String, peerUid: String) {
        if (!isAdded) return
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra(AppConstants.KEY_CHAT_ROOM_ID, chatRoomId)
        intent.putExtra(AppConstants.KEY_OTHER_USER_NAME, peerName)
        intent.putExtra(AppConstants.KEY_REQUEST_ID, requestId)
        // For direct messaging, pass the peer UID
        if (peerUid.isNotEmpty()) {
            intent.putExtra(AppConstants.KEY_OTHER_USER_ID, peerUid)
        }
        startActivity(intent)
    }

    private fun openAdDetail(adId: String) {
        if (!isAdded) return
        val intent = Intent(requireContext(), AdDetailActivity::class.java)
        intent.putExtra(AppConstants.KEY_AD_ID, adId)
        startActivity(intent)
    }

    private fun JoinRequest.toInboxItem(): InboxItem {
        val isAccepted = JoinRequest.STATUS_ACCEPTED == status
        val isPending = JoinRequest.STATUS_PENDING == status
        val isMeCreator = adCreatorUid == currentUid
        
        val peerName = if (isMeCreator) requestorName else "Driver"
        val displayTitle = peerName ?: "Ride Partner"
        
        val unread = !isLastMessageRead && lastMessageSenderId != currentUid

        return if (isAccepted && !chatRoomId.isNullOrEmpty()) {
            InboxItem(
                id = requestId ?: adId ?: "",
                type = InboxItemType.CHAT,
                title = displayTitle,
                subtitle = lastMessage ?: "Chat opened - coordinate your ride",
                timestamp = if (lastMessageTimestamp > 0) DateUtils.formatChatTime(lastMessageTimestamp) else DateUtils.formatChatTime(timestamp),
                chatRoomId = chatRoomId ?: "",
                peerName = displayTitle,
                peerUid = "",
                adId = adId ?: "",
                statusLabel = "Accepted",
                isUnread = unread,
                requestId = requestId ?: ""
            )
        } else {
            InboxItem(
                id = requestId ?: adId ?: "",
                type = InboxItemType.JOIN_REQUEST,
                title = displayTitle,
                subtitle = if (isMeCreator) "Wants to join your ride" else "Request sent to join ride",
                timestamp = DateUtils.formatChatTime(timestamp),
                adId = adId ?: "",
                peerName = displayTitle,
                peerUid = "",
                chatRoomId = chatRoomId ?: "",
                statusLabel = when {
                    isPending -> "Pending"
                    isAccepted -> "Accepted"
                    else -> "Rejected"
                },
                isUnread = unread,
                requestId = requestId ?: ""
            )
        }
    }
    
    private fun Conversation.toInboxItem(): InboxItem {
        val uid = currentUid ?: return InboxItem(
            id = conversationId ?: "",
            type = InboxItemType.CHAT,
            title = "Unknown",
            subtitle = "",
            timestamp = ""
        )
        
        // Find the other participant
        val otherUid = participants?.keys?.firstOrNull { it != uid } ?: ""
        val otherInfo = participantInfo?.get(otherUid)
        val displayName = otherInfo?.name ?: "Unknown"
        val photoUrl = otherInfo?.photoUrl
        
        // Get unread count for current user
        val myUnreadCount = unreadCount?.get(uid) ?: 0
        
        return InboxItem(
            id = conversationId ?: "",
            type = InboxItemType.CHAT,
            title = displayName,
            subtitle = lastMessage ?: "Start chatting",
            timestamp = DateUtils.formatChatTime(lastMessageTimestamp),
            chatRoomId = conversationId ?: "",
            peerName = displayName,
            peerUid = otherUid,
            peerPhotoUrl = photoUrl,
            isUnread = myUnreadCount > 0
        )
    }
    
    private fun mergeAndUpdateItems() {
        // Combine both lists and sort by timestamp
        val allItems = (requestItems + conversationItems)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
        stateHolder.setItems(allItems)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RequestPresenter.RequestView Implementation
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onRequestSent() {}

    override fun onRequestsLoaded(requests: List<JoinRequest>) {
        if (!isAdded) return
        requestItems = requests.map { it.toInboxItem() }
        mergeAndUpdateItems()
    }

    override fun onRequestUpdated(request: JoinRequest) {
        if (!isAdded) return
        currentUid?.let { requestPresenter?.loadIncomingRequests(it) }
    }

    override fun onError(message: String) {
        if (!isAdded) return
        stateHolder.setLoading(false)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ConversationPresenter.ConversationView Implementation
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onConversationsLoaded(conversations: List<Conversation>) {
        if (!isAdded) return
        conversationItems = conversations.map { it.toInboxItem() }
        mergeAndUpdateItems()
    }

    override fun onConversationCreated(conversationId: String) {
        // Not used in inbox - handled by ChatActivity
    }

    override fun onMessageReceived(message: ChatMessage) {
        // Not used in inbox - handled by ChatActivity
    }

    override fun onMessageSent() {
        // Not used in inbox - handled by ChatActivity
    }

    override fun onUnreadCountUpdated(totalUnread: Int) {
        // Could be used to update badge in MainBridge
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestPresenter?.detach()
        conversationPresenter?.detach()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxUiState,
    onChatOpen: (String, String, String, String) -> Unit,  // chatRoomId, peerName, requestId, peerUid
    onRequestOpen: (String) -> Unit
) {
    Scaffold(
        containerColor = SafarBlack,
        topBar = {
            InboxTopBar(itemCount = state.items.count { it.isUnread })
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state.isLoading,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            modifier = Modifier.padding(paddingValues),
            label = "inbox_content"
        ) { isLoading ->
            when {
                isLoading -> LoadingState()
                state.items.isEmpty() -> EmptyInboxState()
                else -> InboxList(
                    items = state.items,
                    onChatOpen = onChatOpen,
                    onRequestOpen = onRequestOpen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxTopBar(itemCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafarDarkSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inbox",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SafarTextPrimary
                    )
                )

                if (itemCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = SafarError
                    ) {
                        Text(
                            text = "$itemCount New",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Messages & ride requests",
                style = MaterialTheme.typography.bodyMedium,
                color = SafarTextTertiary
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = SafarGreen)
    }
}

@Composable
private fun EmptyInboxState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SafarTextTertiary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No messages yet",
            fontWeight = FontWeight.SemiBold,
            color = SafarTextSecondary
        )
    }
}

@Composable
private fun InboxList(
    items: List<InboxItem>,
    onChatOpen: (String, String, String, String) -> Unit,  // chatRoomId, peerName, requestId, peerUid
    onRequestOpen: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { _, item ->
            InboxItemCard(
                item = item,
                onClick = {
                    when (item.type) {
                        InboxItemType.CHAT -> onChatOpen(item.chatRoomId, item.peerName, item.requestId, item.peerUid)
                        InboxItemType.JOIN_REQUEST -> onRequestOpen(item.adId)
                    }
                }
            )
        }
    }
}

@Composable
private fun InboxItemCard(
    item: InboxItem,
    onClick: () -> Unit
) {
    val isChat = item.type == InboxItemType.CHAT

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = SafarBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with unread indicator
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = SafarDarkCard
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.title.firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (item.isUnread) SafarGreen else SafarTextSecondary
                        )
                    }
                }
                
                if (item.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(SafarError)
                            .align(Alignment.TopEnd)
                            .border(2.dp, SafarBlack, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = if (item.isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (item.isUnread) SafarTextPrimary else SafarTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = item.timestamp,
                        fontSize = 11.sp,
                        color = if (item.isUnread) SafarGreen else SafarTextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 13.sp,
                    color = if (item.isUnread) SafarTextPrimary else SafarTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (item.isUnread) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SafarDivider, thickness = 0.5.dp)
}
