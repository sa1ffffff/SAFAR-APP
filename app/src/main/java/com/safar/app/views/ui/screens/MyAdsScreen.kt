package com.safar.app.views.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

// ─── Data Models ──────────────────────────────────────────────────────────────
data class RideAdUi @JvmOverloads constructor(
    val id: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val seats: String,
    val vehicle: String,
    val status: String = "Open",
    val statusColor: String = "#06C167",
    val requestsCount: Int = 0
)

data class MyAdsUiState(
    val ads: List<RideAdUi> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,
    val selectedAd: RideAdUi? = null
)

/**
 * SAFAR MY ADS SCREEN
 * Enterprise-level ride management with Uber-inspired design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAdsScreen(
    state: MyAdsUiState = MyAdsUiState(),
    currentFilter: Any? = null,
    filters: List<Any> = emptyList(),
    isRefreshing: Boolean = false,
    onFilterChange: (Any) -> Unit = {},
    onAdClick: (String) -> Unit = {},
    onAdDelete: (RideAdUi) -> Unit = {},
    onConfirmDelete: (String) -> Unit = {},
    onDismissDeleteDialog: () -> Unit = {},
    onAdEdit: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onPostNewAd: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    SafarTheme(darkTheme = true) {
        Scaffold(
            containerColor = SafarBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "My Rides",
                                fontWeight = FontWeight.Bold,
                                color = SafarTextPrimary,
                                fontSize = 18.sp
                            )
                            Text(
                                "${state.ads.size} total",
                                fontSize = 12.sp,
                                color = SafarTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Rounded.ArrowBackIosNew,
                                "Back",
                                tint = SafarTextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onPostNewAd) {
                            Icon(
                                Icons.Rounded.Add,
                                "Post New",
                                tint = SafarGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SafarDarkSurface
                    )
                )
            },
            floatingActionButton = {
                if (state.ads.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = onPostNewAd,
                        containerColor = SafarGreen,
                        contentColor = SafarBlack,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Add, "Post New Ride")
                    }
                }
            }
        ) { padding ->
            when {
                state.isLoading -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = SafarGreen,
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading your rides...",
                                color = SafarTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                state.ads.isEmpty() && !state.isLoading -> {
                    EmptyAdsView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onPostNew = onPostNewAd
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.ads, key = { it.id }) { ad ->
                            RideAdCard(
                                ad = ad,
                                onClick = { onAdClick(ad.id) },
                                onDelete = { onAdDelete(ad) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (state.showDeleteDialog && state.selectedAd != null) {
            AlertDialog(
                onDismissRequest = onDismissDeleteDialog,
                title = { Text("Delete Ride?") },
                text = { Text("Are you sure you want to delete this ride from ${state.selectedAd.origin} to ${state.selectedAd.destination}?") },
                confirmButton = {
                    TextButton(onClick = { onConfirmDelete(state.selectedAd.id) }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeleteDialog) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun RideAdCard(ad: RideAdUi, onClick: () -> Unit, onDelete: () -> Unit) {
    val isOpen = ad.status.equals("Open", ignoreCase = true) || ad.status.equals("Active", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SafarDarkCard),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOpen) StatusOpen.copy(alpha = 0.12f)
                    else StatusFilled.copy(alpha = 0.12f)
                ) {
                    Text(
                        ad.status,
                        color = if (isOpen) StatusOpen else StatusFilled,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    // Request count badge
                    if (ad.requestsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SafarGreen.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.PersonAdd,
                                    null,
                                    tint = SafarGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${ad.requestsCount} request${if (ad.requestsCount > 1) "s" else ""}",
                                    color = SafarGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            null,
                            tint = SafarTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Route
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SafarSuccess)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    ad.origin,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SafarTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Connector
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    .width(2.dp)
                    .height(16.dp)
                    .background(SafarDivider)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SafarGreen)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    ad.destination,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SafarTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SafarDivider.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoPill(
                    icon = Icons.Rounded.Schedule,
                    text = ad.departureTime
                )
                InfoPill(
                    icon = Icons.Rounded.EventSeat,
                    text = ad.seats
                )
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            tint = SafarGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = SafarTextSecondary
        )
    }
}

@Composable
private fun EmptyAdsView(
    modifier: Modifier = Modifier,
    onPostNew: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SafarDarkCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.DirectionsCar,
                null,
                tint = SafarTextTertiary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No rides posted yet",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = SafarTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Your posted rides will appear here.\nShare your trip and find co-travelers!",
            fontSize = 14.sp,
            color = SafarTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPostNew,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SafarGreen,
                contentColor = SafarBlack
            )
        ) {
            Icon(
                Icons.Rounded.Add,
                null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Post Your First Ride",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
