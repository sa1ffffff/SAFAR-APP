package com.safar.app.views.activities

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.views.ui.theme.SafarBlack
import com.safar.app.views.ui.theme.SafarDarkSurface
import com.safar.app.views.ui.theme.SafarGreen
import com.safar.app.views.ui.theme.SafarTextSecondary
import com.safar.app.views.ui.theme.SafarTextTertiary
import com.safar.app.views.ui.theme.SafarTheme

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MAIN BRIDGE
// Connects Java MainActivity to Jetpack Compose Bottom Navigation
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//
// INTEGRATION GUIDE:
// ─────────────────────────────────────────────────────────────────────────────
// In MainActivity.java:
//
//   1. Add ComposeView to layout or create programmatically:
//      ComposeView composeView = new ComposeView(this);
//
//   2. Create state holder:
//      private MainStateHolder stateHolder = new MainStateHolder();
//
//   3. In onCreate():
//      MainBridgeKt.setupComposeUI(this, composeView, stateHolder);
//
//   4. Update selected tab:
//      stateHolder.setSelectedTab(tabIndex);
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a tab in the main navigation bar.
 * Uses Uber-style iconography with filled icons for selected state.
 */
sealed class MainTab(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Map : MainTab(
        index = 0,
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Rides : MainTab(
        index = 1,
        label = "Activity",
        selectedIcon = Icons.Rounded.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )
    
    data object Inbox : MainTab(
        index = 2,
        label = "Messages",
        selectedIcon = Icons.Rounded.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    )
    
    data object Profile : MainTab(
        index = 3,
        label = "Account",
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

/** All main navigation tabs in order */
private val mainTabs = listOf(MainTab.Map, MainTab.Rides, MainTab.Inbox, MainTab.Profile)

/**
 * State holder for main navigation.
 * Manages the currently selected tab index.
 *
 * Java Access Pattern:
 * ```java
 * MainStateHolder stateHolder = new MainStateHolder();
 * 
 * // Reading
 * int currentTab = stateHolder.getSelectedTab();
 * 
 * // Setting
 * stateHolder.setSelectedTab(2);
 * ```
 */
class MainStateHolder {
    
    /** Currently selected tab index (0-3) */
    var selectedTab by mutableStateOf(0)
    
    /** Badge count for messages/inbox tab */
    var inboxBadgeCount by mutableStateOf(0)
    
    /** Whether to show activity indicator on rides tab */
    var hasActiveRide by mutableStateOf(false)
    
    /**
     * Selects a tab by index.
     */
    fun selectTab(index: Int) {
        if (index in 0..3) {
            selectedTab = index
        }
    }
    
    /**
     * Updates the inbox badge count.
     */
    fun updateInboxBadge(count: Int) {
        inboxBadgeCount = maxOf(0, count)
    }
    
    /**
     * Sets whether there's an active ride in progress.
     */
    fun setActiveRide(active: Boolean) {
        hasActiveRide = active
    }
}

/**
 * Sets up the Compose UI for MainActivity's bottom navigation.
 *
 * @receiver The MainActivity instance
 * @param composeView The ComposeView to render the navigation in
 * @param stateHolder The state holder managing navigation state
 */
fun MainActivity.setupComposeUI(
    composeView: ComposeView,
    stateHolder: MainStateHolder
) {
    composeView.setContent {
        SafarTheme(darkTheme = true) {
            SafarBottomNavigation(
                selectedTab = stateHolder.selectedTab,
                inboxBadgeCount = stateHolder.inboxBadgeCount,
                hasActiveRide = stateHolder.hasActiveRide,
                onTabSelected = { tabIndex ->
                    stateHolder.selectTab(tabIndex)
                    onTabSelected(tabIndex)
                }
            )
        }
    }
}

/**
 * Uber-style bottom navigation bar.
 * Features clean iconography, subtle selection indicators, and badge support.
 */
@Composable
private fun SafarBottomNavigation(
    selectedTab: Int,
    inboxBadgeCount: Int,
    hasActiveRide: Boolean,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = SafarBlack,
        contentColor = SafarTextSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        mainTabs.forEach { tab ->
            val selected = selectedTab == tab.index
            val showBadge = tab == MainTab.Inbox && inboxBadgeCount > 0
            val showActivityDot = tab == MainTab.Rides && hasActiveRide
            
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.index) },
                icon = {
                    BadgedBox(
                        badge = {
                            when {
                                showBadge -> {
                                    Badge(
                                        containerColor = SafarGreen,
                                        contentColor = SafarBlack
                                    ) {
                                        Text(
                                            text = if (inboxBadgeCount > 99) "99+" else inboxBadgeCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                showActivityDot -> {
                                    Badge(
                                        containerColor = SafarGreen,
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SafarGreen,
                    selectedTextColor = SafarGreen,
                    unselectedIconColor = SafarTextTertiary,
                    unselectedTextColor = SafarTextTertiary,
                    indicatorColor = SafarGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}
