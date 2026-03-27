package com.safar.app.views.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.safar.app.firebase.FirebaseManager
import com.safar.app.models.JoinRequest
import com.safar.app.models.RideAd
import com.safar.app.presenters.RequestPresenter
import com.safar.app.utils.AppConstants
import com.safar.app.utils.DateUtils
import com.safar.app.utils.SessionManager
import com.safar.app.views.activities.AdDetailActivity
import com.safar.app.views.activities.ChatActivity
import com.safar.app.views.ui.theme.SafarTheme

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Premium Ride Ad Bottom Sheet
// Material 3 Design with modern animations and micro-interactions
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Premium BottomSheetDialogFragment with Jetpack Compose UI
 * Displays ride details when a map marker is tapped
 */
class AdBottomSheetFragment : BottomSheetDialogFragment(), RequestPresenter.RequestView {

    companion object {
        private const val ARG_AD_ID = "arg_ad_id"

        fun newInstance(adId: String): AdBottomSheetFragment {
            val fragment = AdBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_AD_ID, adId)
            fragment.arguments = args
            return fragment
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private var requestPresenter: RequestPresenter? = null
    private var currentAd: RideAd? = null
    private var adId: String? = null
    private lateinit var session: SessionManager

    // Compose state
    private val uiState = mutableStateOf(AdBottomSheetUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SafarTheme(darkTheme = true) {
                AdBottomSheetContent(
                    state = uiState.value,
                    onRequestJoin = { sendJoinRequest() },
                    onViewDetail = { navigateToDetail() },
                    onCall = { makePhoneCall() },
                    onOpenChat = { openChat() },
                    onDismiss = { dismiss() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adId = arguments?.getString(ARG_AD_ID)
        session = SessionManager(requireContext())
        requestPresenter = RequestPresenter(this)

        if (adId == null) {
            dismiss()
            return
        }

        loadAdDetails()
    }

    private fun loadAdDetails() {
        uiState.value = uiState.value.copy(isLoading = true)
        
        FirebaseManager.getInstance().getAdRef(adId!!).get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                
                currentAd = snapshot.getValue(RideAd::class.java)
                if (currentAd == null) {
                    dismiss()
                    return@addOnSuccessListener
                }
                currentAd!!.adId = adId
                updateUiState()
                checkExistingRequest()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                uiState.value = uiState.value.copy(isLoading = false, error = "Failed to load ride details")
            }
    }

    private fun updateUiState() {
        val ad = currentAd ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = ad.creatorUid == currentUid

        uiState.value = AdBottomSheetUiState(
            isLoading = false,
            origin = ad.origin ?: "",
            destination = ad.destination ?: "",
            departureTime = DateUtils.formatDeparture(ad.departureTime),
            availableSeats = ad.availableSeats,
            creatorName = ad.creatorName ?: "Unknown",
            vehicleDetails = ad.vehicleDetails,
            isOwner = isOwner,
            buttonState = if (isOwner) ButtonState.MANAGE else ButtonState.REQUEST
        )
    }

    private fun checkExistingRequest() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        requestPresenter?.checkExistingRequest(adId!!, currentUid) { existingRequest ->
            if (!isAdded) return@checkExistingRequest
            
            when {
                existingRequest == null -> Unit
                JoinRequest.STATUS_PENDING == existingRequest.status -> {
                    uiState.value = uiState.value.copy(buttonState = ButtonState.PENDING)
                }
                JoinRequest.STATUS_ACCEPTED == existingRequest.status -> {
                    uiState.value = uiState.value.copy(
                        buttonState = ButtonState.CHAT,
                        chatRoomId = existingRequest.chatRoomId
                    )
                }
            }
        }
    }

    private fun sendJoinRequest() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val request = JoinRequest(
            adId!!,
            currentUid,
            session.getUserName(),
            session.getUserPhone(),
            currentAd?.creatorUid
        )
        
        uiState.value = uiState.value.copy(isRequestLoading = true)
        requestPresenter?.sendJoinRequest(request)
    }

    private fun navigateToDetail() {
        val intent = Intent(requireContext(), AdDetailActivity::class.java)
        intent.putExtra(AppConstants.KEY_AD_ID, adId)
        startActivity(intent)
        dismiss()
    }

    private fun makePhoneCall() {
        val phone = currentAd?.creatorPhone
        if (!phone.isNullOrEmpty()) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        } else {
            Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openChat() {
        val chatRoomId = uiState.value.chatRoomId ?: return
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra(AppConstants.KEY_CHAT_ROOM_ID, chatRoomId)
        intent.putExtra(AppConstants.KEY_OTHER_USER_NAME, currentAd?.creatorName)
        startActivity(intent)
        dismiss()
    }

    // ── RequestPresenter callbacks ────────────────────────────────────────────
    override fun onRequestSent() {
        if (!isAdded) return
        uiState.value = uiState.value.copy(
            isRequestLoading = false,
            buttonState = ButtonState.PENDING
        )
        Toast.makeText(requireContext(), "Request sent! Waiting for approval.", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestsLoaded(requests: List<JoinRequest>) {}
    override fun onRequestUpdated(request: JoinRequest) {}

    override fun onError(message: String) {
        if (!isAdded) return
        uiState.value = uiState.value.copy(isRequestLoading = false, error = message)
        Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestPresenter?.detach()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// UI STATE
// ══════════════════════════════════════════════════════════════════════════════

data class AdBottomSheetUiState(
    val isLoading: Boolean = true,
    val isRequestLoading: Boolean = false,
    val origin: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val availableSeats: Int = 0,
    val creatorName: String = "",
    val vehicleDetails: String? = null,
    val isOwner: Boolean = false,
    val buttonState: ButtonState = ButtonState.REQUEST,
    val chatRoomId: String? = null,
    val error: String? = null
)

enum class ButtonState {
    REQUEST, PENDING, CHAT, MANAGE
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPOSABLE UI - Premium Material 3 Design
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdBottomSheetContent(
    state: AdBottomSheetUiState,
    onRequestJoin: () -> Unit,
    onViewDetail: () -> Unit,
    onCall: () -> Unit,
    onOpenChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = surfaceColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoadingState()
            }

            AnimatedVisibility(
                visible = !state.isLoading,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Column {
                    // Route Header
                    RouteHeader(
                        origin = state.origin,
                        destination = state.destination
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Info Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoChip(
                            icon = Icons.Outlined.Schedule,
                            text = state.departureTime,
                            modifier = Modifier.weight(1f)
                        )
                        InfoChip(
                            icon = Icons.Outlined.AirlineSeatReclineNormal,
                            text = "${state.availableSeats} seat(s)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Driver Info Card
                    DriverInfoCard(
                        name = state.creatorName,
                        vehicle = state.vehicleDetails
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    ActionButtons(
                        state = state,
                        onRequestJoin = onRequestJoin,
                        onViewDetail = onViewDetail,
                        onCall = onCall,
                        onOpenChat = onOpenChat
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading ride details...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RouteHeader(
    origin: String,
    destination: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Route Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Location Names
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = origin,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = destination,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DriverInfoCard(
    name: String,
    vehicle: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!vehicle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = vehicle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Verified Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    state: AdBottomSheetUiState,
    onRequestJoin: () -> Unit,
    onViewDetail: () -> Unit,
    onCall: () -> Unit,
    onOpenChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Call Button
        FilledTonalIconButton(
            onClick = onCall,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Primary Action Button
        when (state.buttonState) {
            ButtonState.REQUEST -> {
                Button(
                    onClick = onRequestJoin,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isRequestLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isRequestLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Request to Join",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
            ButtonState.PENDING -> {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Request Sent",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            ButtonState.CHAT -> {
                Button(
                    onClick = onOpenChat,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Chat",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            ButtonState.MANAGE -> {
                Button(
                    onClick = onViewDetail,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manage Ride",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // View Details Button
        if (state.buttonState != ButtonState.MANAGE) {
            FilledTonalIconButton(
                onClick = onViewDetail,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
