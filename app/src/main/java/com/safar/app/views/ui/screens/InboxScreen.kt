package com.safar.app.views.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

// ── Palette ────────────────────────────────────────────────────────────────────
private val BgBlack        = Color(0xFF000000)
private val HeaderGreen    = Color(0xFF1B3A2D)
private val AccentGreen    = Color(0xFF00C853)
private val AvatarGradientStart = Color(0xFF1B5E4D)
private val AvatarGradientEnd   = Color(0xFF00C853)
private val AvatarGrey     = Color(0xFF2A2A2A)
private val TextPrimary    = Color(0xFFFFFFFF)
private val TextSecondary  = Color(0xFF9E9E9E)
private val TextTertiary   = Color(0xFF757575)
private val Divider        = Color(0xFF1C1C1C)

// ── UI Models ──────────────────────────────────────────────────────────────────
enum class InboxItemType { CHAT, JOIN_REQUEST }

data class InboxItem(
    val id: String,
    val type: InboxItemType,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val adId: String = "",
    val chatRoomId: String = "",
    val peerName: String = "",
    val peerId: String = "",
    val statusLabel: String = "",
    val isRequest: Boolean = false,
    val isRead: Boolean = false
)

data class InboxUiState(
    val items: List<InboxItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ── Root Screen ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxUiState = InboxUiState(),
    onChatOpen: (String, String) -> Unit = { _, _ -> },
    onRequestOpen: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
    ) {
        InboxHeader()

        when {
            state.isLoading -> InboxLoadingState()
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        when (item.type) {
                            InboxItemType.CHAT -> InboxChatRow(
                                item = item,
                                onClick = { onChatOpen(item.chatRoomId, item.peerName) },
                                onAvatarClick = {
                                    if (item.peerId.isNotEmpty()) onUserClick(item.peerId)
                                }
                            )
                            InboxItemType.JOIN_REQUEST -> InboxRequestRow(
                                item = item,
                                onClick = { onRequestOpen(item.adId) }
                            )
                        }
                    }

                    item {
                        EmptyInboxFooter()
                    }
                }
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────────
@Composable
private fun InboxHeader() {
    Surface(
        color = HeaderGreen,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Inbox",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Messages & ride requests",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ── Chat Row ───────────────────────────────────────────────────────────────────
@Composable
private fun InboxChatRow(
    item: InboxItem,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val hasUnread = item.unreadCount > 0

    val initials = item.peerName
        .trim()
        .split("\\s+".toRegex())
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    // Determine avatar style: gradient for named contacts, flat grey for groups/others
    val useGradient = item.peerName.isNotEmpty() && !item.peerName.contains("NUST")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBlack)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (useGradient) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AvatarGradientStart, AvatarGradientEnd),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                        } else {
                            Modifier.background(AvatarGrey)
                        }
                    )
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Text Block ────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {

                // Row 1: Name + (green dot?) + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Green dot for unread — appears before timestamp
                        if (hasUnread) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                        }
                        Text(
                            text = item.timestamp,
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row 2: Preview + (read receipt OR unread badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.subtitle,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    when {
                        // Unread badge — green pill with count
                        hasUnread -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentGreen
                            ) {
                                Text(
                                    text = if (item.unreadCount > 9) "9+" else item.unreadCount.toString(),
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        // Double checkmark for read/sent messages
                        item.isRead -> {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Read",
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Divider — starts after avatar (20 + 54 + 14 = 88dp)
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 88.dp),
            thickness = 0.6.dp,
            color = Divider
        )
    }
}

// ── Request Row ────────────────────────────────────────────────────────────────
@Composable
private fun InboxRequestRow(item: InboxItem, onClick: () -> Unit) {
    val statusColor = when (item.statusLabel.lowercase()) {
        "accepted" -> Color(0xFF00C853)
        "rejected" -> Color(0xFFEF5350)
        else -> Color(0xFFFFA726)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBlack)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = item.timestamp,
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.subtitle,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.statusLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = item.statusLabel,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 88.dp),
            thickness = 0.6.dp,
            color = Divider
        )
    }
}

// ── Empty Footer ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyInboxFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Line-art illustration: chat bubbles + car
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeColor = Color(0xFF2D5A3D)
            val drawingStroke = Stroke(
                width = 3.5f,
                pathEffect = null
            )
            val w = size.width
            val h = size.height

            // ── Chat bubble 1 (back, top-right) ────────────────────────────
            val bubble1Path = Path().apply {
                val bx = w * 0.38f
                val by = h * 0.08f
                val bw = w * 0.52f
                val bh = h * 0.30f
                val r  = 14f
                moveTo(bx + r, by)
                lineTo(bx + bw - r, by)
                quadraticTo(bx + bw, by, bx + bw, by + r)
                lineTo(bx + bw, by + bh - r)
                quadraticTo(bx + bw, by + bh, bx + bw - r, by + bh)
                lineTo(bx + r * 2f, by + bh)
                lineTo(bx + r, by + bh + 12f)
                lineTo(bx + r * 0.5f, by + bh)
                quadraticTo(bx, by + bh, bx, by + bh - r)
                lineTo(bx, by + r)
                quadraticTo(bx, by, bx + r, by)
                close()
            }
            drawPath(bubble1Path, color = strokeColor, style = drawingStroke)

            // ── Lines inside bubble 1 ─────────────────────────────────────
            val b1x = w * 0.38f
            val b1y = h * 0.08f
            val b1w = w * 0.52f
            val b1h = h * 0.30f
            for (i in 0..2) {
                val ly = b1y + b1h * 0.28f + i * b1h * 0.22f
                drawLine(
                    color = strokeColor,
                    start = Offset(b1x + b1w * 0.14f, ly),
                    end   = Offset(b1x + b1w * 0.78f, ly),
                    strokeWidth = 2.5f
                )
            }

            // ── Chat bubble 2 (front, center-left) ────────────────────────
            val bubble2Path = Path().apply {
                val bx = w * 0.06f
                val by = h * 0.26f
                val bw = w * 0.52f
                val bh = h * 0.30f
                val r  = 14f
                moveTo(bx + r, by)
                lineTo(bx + bw - r, by)
                quadraticTo(bx + bw, by, bx + bw, by + r)
                lineTo(bx + bw, by + bh - r)
                quadraticTo(bx + bw, by + bh, bx + bw - r, by + bh)
                lineTo(bx + bw - r * 0.5f, by + bh + 12f)
                lineTo(bx + bw - r * 2f, by + bh)
                lineTo(bx + r, by + bh)
                quadraticTo(bx, by + bh, bx, by + bh - r)
                lineTo(bx, by + r)
                quadraticTo(bx, by, bx + r, by)
                close()
            }
            drawPath(bubble2Path, color = strokeColor, style = drawingStroke)

            // ── Lines inside bubble 2 ─────────────────────────────────────
            val b2x = w * 0.06f
            val b2y = h * 0.26f
            val b2w = w * 0.52f
            val b2h = h * 0.30f
            for (i in 0..2) {
                val ly = b2y + b2h * 0.28f + i * b2h * 0.22f
                drawLine(
                    color = strokeColor,
                    start = Offset(b2x + b2w * 0.14f, ly),
                    end   = Offset(b2x + b2w * (if (i == 2) 0.50f else 0.78f), ly),
                    strokeWidth = 2.5f
                )
            }

            // ── Car body ──────────────────────────────────────────────────
            val carY  = h * 0.66f
            val carX  = w * 0.14f
            val carW  = w * 0.72f
            val carH  = h * 0.15f

            // Car base rectangle
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(carX, carY),
                size = androidx.compose.ui.geometry.Size(carW, carH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                style = drawingStroke
            )

            // Car roof / cabin
            val roofPath = Path().apply {
                moveTo(carX + carW * 0.22f, carY)
                lineTo(carX + carW * 0.32f, carY - h * 0.10f)
                lineTo(carX + carW * 0.68f, carY - h * 0.10f)
                lineTo(carX + carW * 0.78f, carY)
                close()
            }
            drawPath(roofPath, color = strokeColor, style = drawingStroke)

            // Wheels
            val wheelR = carH * 0.55f
            val wheelY = carY + carH
            drawCircle(strokeColor, wheelR, Offset(carX + carW * 0.22f, wheelY), style = drawingStroke)
            drawCircle(strokeColor, wheelR, Offset(carX + carW * 0.78f, wheelY), style = drawingStroke)

            // Ground line
            drawLine(
                color = strokeColor.copy(alpha = 0.4f),
                start = Offset(w * 0.05f, wheelY + wheelR),
                end   = Offset(w * 0.95f, wheelY + wheelR),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f, 6f)
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No more conversations",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = TextTertiary
        )
    }
}

// ── Loading State ──────────────────────────────────────────────────────────────
@Composable
private fun InboxLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AccentGreen,
                modifier = Modifier.size(36.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Loading messages...",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}