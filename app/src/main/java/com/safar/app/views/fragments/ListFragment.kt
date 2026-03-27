package com.safar.app.views.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.safar.app.models.RideAd
import com.safar.app.presenters.AdsPresenter
import com.safar.app.utils.AppConstants
import com.safar.app.utils.DateUtils
import com.safar.app.views.activities.AdDetailActivity
import com.safar.app.views.ui.theme.SafarTheme

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Premium Rides List Screen
// Material 3 Design with modern search, filters, and animated cards
// ══════════════════════════════════════════════════════════════════════════════

// ── UI State ──────────────────────────────────────────────────────────────────
data class RidesListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val currentTab: RideTab = RideTab.ALL,
    val rides: List<RideAdListItem> = emptyList(),
    val resultCount: Int = 0
)

enum class RideTab(val label: String, val icon: ImageVector) {
    ALL("All Rides", Icons.Filled.List),
    CITY("City", Icons.Filled.LocationCity),
    LONG("Long Distance", Icons.Filled.Route)
}

data class RideAdListItem(
    val adId: String,
    val origin: String,
    val destination: String,
    val departureTime: Long,
    val seats: Int,
    val postedBy: String,
    val vehicle: String,
    val status: String
)

// ── State Holder ──────────────────────────────────────────────────────────────
class RidesListStateHolder {
    var state by mutableStateOf(RidesListUiState())
        private set

    fun setLoading(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }

    fun setRefreshing(refreshing: Boolean) {
        state = state.copy(isRefreshing = refreshing)
    }

    fun setSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
    }

    fun setCurrentTab(tab: RideTab) {
        state = state.copy(currentTab = tab)
    }

    fun setRides(rides: List<RideAdListItem>, count: Int) {
        state = state.copy(rides = rides, resultCount = count, isLoading = false, isRefreshing = false)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// FRAGMENT
// ══════════════════════════════════════════════════════════════════════════════

class ListFragment : Fragment(), AdsPresenter.AdsView {

    private val stateHolder = RidesListStateHolder()
    private var presenter: AdsPresenter? = null
    private val allAds = mutableListOf<RideAd>()
    private var currentTab = RideTab.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SafarTheme(darkTheme = true) {
                RidesListScreen(
                    state = stateHolder.state,
                    onRideClick = { adId -> openAdDetail(adId) },
                    onSearchChange = { query ->
                        stateHolder.setSearchQuery(query)
                        applyFilters()
                    },
                    onTabChange = { tab ->
                        currentTab = tab
                        stateHolder.setCurrentTab(tab)
                        applyFilters()
                    },
                    onRefresh = {
                        stateHolder.setRefreshing(true)
                        presenter?.loadActiveAds()
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = AdsPresenter(this)
        presenter?.loadActiveAds()
    }

    private fun applyFilters() {
        val query = stateHolder.state.searchQuery.lowercase().trim()

        val filtered = allAds.filter { ad ->
            val matchesSearch = query.isEmpty()
                    || ad.origin?.lowercase()?.contains(query) == true
                    || ad.destination?.lowercase()?.contains(query) == true

            val matchesTab = when (currentTab) {
                RideTab.CITY -> ad.origin?.equals(ad.destination, ignoreCase = true) == true
                RideTab.LONG -> ad.origin?.equals(ad.destination, ignoreCase = true) == false
                RideTab.ALL -> true
            }

            matchesSearch && matchesTab
        }.map { ad ->
            RideAdListItem(
                adId = ad.adId ?: "",
                origin = ad.origin ?: "",
                destination = ad.destination ?: "",
                departureTime = ad.departureTime,
                seats = ad.availableSeats,
                postedBy = ad.creatorName ?: "",
                vehicle = ad.vehicleDetails ?: "",
                status = ad.status ?: "Open"
            )
        }

        stateHolder.setRides(filtered, filtered.size)
    }

    private fun openAdDetail(adId: String) {
        if (!isAdded) return
        val intent = Intent(requireContext(), AdDetailActivity::class.java)
        intent.putExtra(AppConstants.KEY_AD_ID, adId)
        startActivity(intent)
    }

    // ── AdsPresenter Callbacks ────────────────────────────────────────────────
    override fun onAdsLoaded(ads: List<RideAd>) {
        if (!isAdded) return
        allAds.clear()
        allAds.addAll(ads)
        applyFilters()
    }

    override fun onAdPosted(adId: String) {}
    override fun onAdDeleted() {}

    override fun onError(message: String) {
        if (!isAdded) return
        stateHolder.setLoading(false)
        stateHolder.setRefreshing(false)
    }

    override fun onLoading(isLoading: Boolean) {
        stateHolder.setLoading(isLoading)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.detach()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPOSABLE UI - Premium Material 3 Rides List
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidesListScreen(
    state: RidesListUiState,
    onRideClick: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onTabChange: (RideTab) -> Unit,
    onRefresh: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RidesListTopBar(
                searchQuery = state.searchQuery,
                onSearchChange = onSearchChange,
                resultCount = state.resultCount
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter Tabs
                FilterTabRow(
                    currentTab = state.currentTab,
                    onTabChange = onTabChange
                )

                // Content
                AnimatedContent(
                    targetState = state.isLoading,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "content"
                ) { isLoading ->
                    if (isLoading) {
                        LoadingState()
                    } else if (state.rides.isEmpty()) {
                        EmptyState(searchQuery = state.searchQuery)
                    } else {
                        RidesList(
                            rides = state.rides,
                            onRideClick = onRideClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RidesListTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    resultCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // Title
            Text(
                text = "Find Rides",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$resultCount rides available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search by origin or destination",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun FilterTabRow(
    currentTab: RideTab,
    onTabChange: (RideTab) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(RideTab.entries) { tab ->
            FilterChip(
                selected = currentTab == tab,
                onClick = { onTabChange(tab) },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading rides...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) "No rides found" else "No rides available",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) {
                "Try adjusting your search criteria"
            } else {
                "Be the first to post a ride!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RidesList(
    rides: List<RideAdListItem>,
    onRideClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = rides,
            key = { _, ride -> ride.adId }
        ) { index, ride ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 50
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 50
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                RideCard(
                    ride = ride,
                    onClick = { onRideClick(ride.adId) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RideCard(
    ride: RideAdListItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Route Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Route Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Location Names
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.origin,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = ride.destination,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                StatusBadge(status = ride.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChipSmall(
                    icon = Icons.Outlined.Schedule,
                    text = DateUtils.formatShort(ride.departureTime)
                )
                InfoChipSmall(
                    icon = Icons.Outlined.AirlineSeatReclineNormal,
                    text = "${ride.seats} seat(s)"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Driver Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ride.postedBy.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.postedBy,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (ride.vehicle.isNotEmpty()) {
                        Text(
                            text = ride.vehicle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "open" -> Pair(Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF4CAF50))
        "full" -> Pair(Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFFF9800))
        "closed" -> Pair(Color(0xFFF44336).copy(alpha = 0.15f), Color(0xFFF44336))
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}

@Composable
private fun InfoChipSmall(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
