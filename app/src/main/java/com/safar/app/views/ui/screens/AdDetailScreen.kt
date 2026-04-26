package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.safar.app.views.ui.theme.*

@Immutable
data class AdDetailUiState(
    val origin: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val seats: String = "",
    val postedBy: String = "",
    val posterPhone: String = "",
    val posterRating: Float = 0f,
    val posterAvatar: String? = null,
    val vehicle: String = "",
    val status: String = "Open",
    val notes: String = "",
    val price: String = "",
    val isOwner: Boolean = false,
    val isLoading: Boolean = false,
    val hasRequested: Boolean = false,
    val userRequestStatus: String = "",
    val joinRequests: List<JoinRequestUiModel> = emptyList(),
    val errorMessage: String? = null
)

@Immutable
data class JoinRequestUiModel(
    val id: String,
    val userId: String,  // Added for profile navigation
    val userName: String,
    val userPhone: String? = null,
    val userAvatar: String? = null,
    val userRating: Float = 0f,
    val seatsRequested: Int = 1,
    val message: String? = null,
    val status: String = "Pending",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdDetailScreen(
    uiState: AdDetailUiState = AdDetailUiState(),
    showActionSheet: Boolean = false,
    showDeleteDialog: Boolean = false,
    showMarkFilledDialog: Boolean = false,
    processingRequestId: String? = null,
    onBack: () -> Unit = {},
    onShowActionSheet: () -> Unit = {},
    onDismissActionSheet: () -> Unit = {},
    onMarkFilled: () -> Unit = {},
    onConfirmMarkFilled: () -> Unit = {},
    onEditAd: () -> Unit = {},
    onDeleteAd: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDialogs: () -> Unit = {},
    onShareAd: () -> Unit = {},
    onSendRequest: () -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onAcceptRequest: (String) -> Unit = {},
    onRejectRequest: (String) -> Unit = {},
    onContactPoster: () -> Unit = {},
    onCallPoster: () -> Unit = {},
    onChatWithPoster: () -> Unit = {},
    onWhatsAppPoster: () -> Unit = {},
    onReportAd: () -> Unit = {},
    onUserClick: (String) -> Unit = {}  // Added for profile navigation
) {
    SafarTheme(darkTheme = false) {
        Scaffold(
            containerColor = SafarBg,
            topBar = {
                AdDetailTopBar(
                    onBack = onBack,
                    onShare = onShareAd
                )
            }
        ) { innerPadding ->
            if (uiState.isLoading) {
                LoadingView(modifier = Modifier.padding(innerPadding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { RouteCard(origin = uiState.origin, destination = uiState.destination) }
                    item {
                        InfoCard(
                            departureTime = uiState.departureTime,
                            seats = uiState.seats,
                            postedBy = uiState.postedBy,
                            vehicle = uiState.vehicle,
                            status = uiState.status,
                            price = uiState.price
                        )
                    }
                    if (uiState.notes.isNotBlank()) {
                        item { NotesCard(notes = uiState.notes) }
                    }
                    if (uiState.isOwner) {
                        item {
                            OwnerActionsCard(
                                onMarkFilled = onMarkFilled,
                                onDeleteAd = onDeleteAd
                            )
                        }
                    } else {
                        item {
                            RequesterActionsCard(
                                hasRequested = uiState.hasRequested,
                                userRequestStatus = uiState.userRequestStatus,
                                onSendRequest = onSendRequest,
                                onCallDriver = onCallPoster,
                                onChatWithDriver = onChatWithPoster,
                                onWhatsAppDriver = onWhatsAppPoster
                            )
                        }
                    }
                    if (uiState.isOwner && uiState.joinRequests.isNotEmpty()) {
                        item {
                            SectionHeader(title = "JOIN REQUESTS")
                        }
                        items(items = uiState.joinRequests, key = { it.id }) { request ->
                            JoinRequestCard(
                                request = request,
                                isProcessing = processingRequestId == request.id,
                                onAccept = { onAcceptRequest(request.id) },
                                onReject = { onRejectRequest(request.id) },
                                onUserClick = { onUserClick(request.userId) }
                            )
                        }
                    }
                    if (uiState.isOwner && uiState.joinRequests.isEmpty()) {
                        item { EmptyRequestsView() }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
        
        // Dialogs
        if (showMarkFilledDialog) {
            SimpleConfirmDialog(
                title = "Mark as Filled?",
                text = "This will hide the ride from search results.",
                onConfirm = onConfirmMarkFilled,
                onDismiss = onDismissDialogs
            )
        }
        if (showDeleteDialog) {
            SimpleConfirmDialog(
                title = "Delete Ride?",
                text = "This action cannot be undone.",
                confirmText = "Delete",
                confirmColor = SafarError,
                onConfirm = onConfirmDelete,
                onDismiss = onDismissDialogs
            )
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SafarGreen, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading ride details...", color = SafarTextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = SafarTextMuted,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SimpleConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "Confirm",
    confirmColor: Color = SafarGreen,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdDetailTopBar(onBack: () -> Unit, onShare: () -> Unit) {
    TopAppBar(
        title = { Text("Ride Detail", fontWeight = FontWeight.Bold, color = SafarTextDark) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = SafarTextDark)
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, "Share", tint = SafarTextDark)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SafarWhite)
    )
}

@Composable
private fun RouteCard(origin: String, destination: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            RouteItem(label = "PICKUP", text = origin, dotColor = SafarSuccess)
            Box(modifier = Modifier.padding(start = 5.dp, top = 6.dp, bottom = 6.dp).width(2.dp).height(24.dp).background(SafarDividerLight))
            RouteItem(label = "DESTINATION", text = destination, dotColor = SafarGreen)
        }
    }
}

@Composable
private fun RouteItem(label: String, text: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafarTextMuted, letterSpacing = 1.sp)
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = SafarTextDark)
        }
    }
}

@Composable
private fun InfoCard(departureTime: String, seats: String, postedBy: String, vehicle: String, status: String, price: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            StatusChip(status)
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(icon = Icons.Rounded.Schedule, label = "Departure", text = departureTime)
            HorizontalDivider(color = SafarDividerLight, modifier = Modifier.padding(vertical = 12.dp))
            InfoRow(icon = Icons.Rounded.EventSeat, label = "Seats", text = seats)
            HorizontalDivider(color = SafarDividerLight, modifier = Modifier.padding(vertical = 12.dp))
            InfoRow(icon = Icons.Rounded.Person, label = "Posted by", text = postedBy)
            if (vehicle.isNotBlank()) {
                HorizontalDivider(color = SafarDividerLight, modifier = Modifier.padding(vertical = 12.dp))
                InfoRow(icon = Icons.Rounded.DirectionsCar, label = "Vehicle", text = vehicle)
            }
            if (price.isNotBlank()) {
                HorizontalDivider(color = SafarDividerLight, modifier = Modifier.padding(vertical = 12.dp))
                InfoRow(icon = Icons.Rounded.CurrencyRupee, label = "Price per seat", text = "Rs $price", highlight = true)
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val isOpen = status.equals("Open", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isOpen) StatusOpen.copy(alpha = 0.12f) else StatusFilled.copy(alpha = 0.12f)
    ) {
        Text(
            text = status,
            color = if (isOpen) StatusOpen else StatusFilled,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, text: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(SafarGreen.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SafarGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = SafarTextMuted)
            Text(text = text, color = if (highlight) SafarGreen else SafarTextDark, fontSize = 15.sp, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Notes, null, tint = SafarGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("NOTES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafarTextMuted, letterSpacing = 1.sp)
                Text(notes, color = SafarTextMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun OwnerActionsCard(onMarkFilled: () -> Unit, onDeleteAd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("MANAGE RIDE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafarTextMuted, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onMarkFilled,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, SafarGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafarGreen)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Filled", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onDeleteAd,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafarError, contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RequesterActionsCard(
    hasRequested: Boolean,
    userRequestStatus: String = "",
    onSendRequest: () -> Unit,
    onCallDriver: () -> Unit,
    onChatWithDriver: () -> Unit,
    onWhatsAppDriver: () -> Unit
) {
    val isAccepted = userRequestStatus.equals("Accepted", ignoreCase = true)
    
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!hasRequested) {
            Button(
                onClick = onSendRequest,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafarGreen, contentColor = SafarBlack)
            ) {
                Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Request to Join", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isAccepted) SafarGreen.copy(alpha = 0.12f) else SafarWarning.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (isAccepted) Icons.Rounded.CheckCircle else Icons.Rounded.AccessTime,
                        null,
                        tint = if (isAccepted) SafarGreen else SafarWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isAccepted) "Request Accepted" else "Request Pending",
                        color = if (isAccepted) SafarGreen else SafarWarning,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
        
        // Contact options: in-app chat, WhatsApp deep-link, and phone call
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onChatWithDriver,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafarGreen,
                    contentColor = SafarBlack
                )
            ) {
                Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Message",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            OutlinedButton(
                onClick = onWhatsAppDriver,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, SafarGreen.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SafarGreen
                )
            ) {
                Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "WhatsApp",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onCallDriver,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, SafarDividerLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SafarTextDark
                )
            ) {
                Icon(Icons.Rounded.Phone, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun JoinRequestCard(
    request: JoinRequestUiModel,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onUserClick: () -> Unit = {}
) {
    val isPending = request.status.equals("Pending", ignoreCase = true)
    val isAccepted = request.status.equals("Accepted", ignoreCase = true)
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SafarWhite),
        elevation = CardDefaults.cardElevation(0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar / Initial - Now clickable
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SafarGreen.copy(alpha = 0.1f))
                    .clickable { onUserClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!request.userAvatar.isNullOrEmpty()) {
                    AsyncImage(
                        model = request.userAvatar,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = request.userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SafarGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // User Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.userName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SafarTextDark
                )
                Text(
                    text = "${request.seatsRequested} seat${if (request.seatsRequested > 1) "s" else ""}",
                    color = SafarTextMuted,
                    fontSize = 12.sp
                )
            }
            
            // Action Buttons or Status
            if (isPending) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SafarGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimal Reject Button (Cross) - Aesthetic Red
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onReject() },
                            shape = CircleShape,
                            color = Color(0xFFFFEBEE)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Reject",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Minimal Accept Button (Tick) - Aesthetic Green
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onAccept() },
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Accept",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Status Badge
                val badgeColor = if (isAccepted) Color(0xFF4CAF50) else Color(0xFFEF5350)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = request.status,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsView() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SafarWhite), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.PersonAdd, null, tint = SafarTextMuted.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No requests yet", fontWeight = FontWeight.SemiBold, color = SafarTextMuted)
            Text("Requests to join will appear here", fontSize = 13.sp, color = SafarTextLight)
        }
    }
}
