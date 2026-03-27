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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Tab enum ──────────────────────────────────────────────────────────────────
enum class RideTab(val label: String) {
    ALL("All Rides"),
    CITY("My City"),
    LONG("Long Distance")
}

// ── UI model ──────────────────────────────────────────────────────────────────
data class RideAdListItem(
    val adId: String,
    val origin: String,
    val destination: String,
    val departureTime: Long,
    val seats: Int,
    val postedBy: String,
    val vehicle: String = "",
    val status: String = "Open",
    val price: String = ""
)

data class RidesListUiState(
    val rides: List<RideAdListItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val currentTab: RideTab = RideTab.ALL,
    val resultCount: Int = 0
)

/**
 * SAFAR RIDES LIST SCREEN
 * Enterprise-level ride browsing interface with Uber-inspired design
 * 
 * Features:
 * - Tabbed navigation (All, My City, Long Distance)
 * - Search functionality
 * - Premium ride cards
 * - Empty and loading states
 * - Pull-to-refresh ready
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidesListScreen(
    state: RidesListUiState = RidesListUiState(),
    onRideClick: (String) -> Unit = {},
    onSearchChange: (String) -> Unit = {},
    onTabChange: (RideTab) -> Unit = {},
    onRefresh: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    SafarTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SafarBlack)
        ) {
            // ── Header with Search ────────────────────────────────────────────
            Surface(
                color = SafarDarkSurface,
                tonalElevation = 2.dp
            ) {
                Column {
                    // Top row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBackIosNew,
                                "Back",
                                tint = SafarTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Text(
                            "Find Rides",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = SafarTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(onClick = { /* Filter */ }) {
                            Icon(
                                Icons.Rounded.FilterList,
                                null,
                                tint = SafarTextSecondary
                            )
                        }
                    }

                    // Search bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = {
                                Text(
                                    "Search origin or destination...",
                                    color = SafarTextTertiary,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    null,
                                    tint = SafarTextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = if (state.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            null,
                                            tint = SafarTextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafarGreen,
                                unfocusedBorderColor = SafarDivider,
                                focusedContainerColor = SafarDarkCard,
                                unfocusedContainerColor = SafarDarkCard,
                                focusedTextColor = SafarTextPrimary,
                                unfocusedTextColor = SafarTextPrimary,
                                cursorColor = SafarGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )
                    }

                    // Tab row
                    ScrollableTabRow(
                        selectedTabIndex = state.currentTab.ordinal,
                        containerColor = SafarDarkSurface,
                        contentColor = SafarGreen,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            val idx = state.currentTab.ordinal
                            if (idx in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                                    color = SafarGreen,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {}
                    ) {
                        RideTab.entries.forEach { tab ->
                            Tab(
                                selected = state.currentTab == tab,
                                onClick = { onTabChange(tab) },
                                text = {
                                    Text(
                                        tab.label,
                                        fontSize = 14.sp,
                                        fontWeight = if (state.currentTab == tab)
                                            FontWeight.Bold else FontWeight.Normal,
                                        color = if (state.currentTab == tab)
                                            SafarGreen else SafarTextSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── Result count ──────────────────────────────────────────────────
            AnimatedVisibility(visible = !state.isLoading && state.rides.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.resultCount} ride${if (state.resultCount != 1) "s" else ""} available",
                        color = SafarTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
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
                                "Finding rides...",
                                color = SafarTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                state.rides.isEmpty() -> {
                    EmptyRidesView(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.rides, key = { it.adId }) { ride ->
                            RideListCard(
                                ride = ride,
                                onClick = { onRideClick(ride.adId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RideListCard(ride: RideAdListItem, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()) }
    val isOpen = ride.status.equals("Open", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SafarDarkCard),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status row
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
                        ride.status,
                        color = if (isOpen) StatusOpen else StatusFilled,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Seats badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.EventSeat,
                        null,
                        tint = SafarGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${ride.seats} seat${if (ride.seats != 1) "s" else ""}",
                        color = SafarTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                    ride.origin,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SafarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Connector
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(SafarDivider)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SafarGreen)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    ride.destination,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SafarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SafarDivider.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        null,
                        tint = SafarGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        sdf.format(Date(ride.departureTime)),
                        color = SafarTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Price or Posted by
                if (ride.price.isNotEmpty()) {
                    Text(
                        ride.price,
                        color = SafarGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            null,
                            tint = SafarTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            ride.postedBy,
                            color = SafarTextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRidesView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
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
                Icons.Rounded.SearchOff,
                null,
                tint = SafarTextTertiary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "No rides found",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = SafarTextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Try a different search or check back later",
            fontSize = 14.sp,
            color = SafarTextSecondary
        )
    }
}
