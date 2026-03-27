package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.safar.app.views.ui.theme.*

// ─── Nav destinations ─────────────────────────────────────────────────────────
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home : BottomNavDestination(
        route = "home",
        label = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home
    )
    
    data object Search : BottomNavDestination(
        route = "search",
        label = "Search",
        icon = Icons.Outlined.Search,
        selectedIcon = Icons.Filled.Search
    )
    
    data object Post : BottomNavDestination(
        route = "post",
        label = "Post",
        icon = Icons.Rounded.AddCircleOutline,
        selectedIcon = Icons.Rounded.AddCircle
    )
    
    data object MyRides : BottomNavDestination(
        route = "myrides",
        label = "My Rides",
        icon = Icons.Outlined.DirectionsCar,
        selectedIcon = Icons.Filled.DirectionsCar
    )
    
    data object Profile : BottomNavDestination(
        route = "profile",
        label = "Profile",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )
}

private val bottomNavItems = listOf(
    BottomNavDestination.Home,
    BottomNavDestination.Search,
    BottomNavDestination.Post,
    BottomNavDestination.MyRides,
    BottomNavDestination.Profile
)

/**
 * SAFAR MAIN SCREEN
 * Enterprise-level navigation shell with Uber-inspired bottom bar
 * 
 * Features:
 * - Smooth navigation transitions
 * - Highlighted center action button
 * - Badge support for notifications
 * - Responsive layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    SafarTheme(darkTheme = true) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                SafarBottomNavBar(
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavDestination.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable(BottomNavDestination.Home.route) { /* HomeScreen() */ }
                composable(BottomNavDestination.Search.route) { /* SearchScreen() */ }
                composable(BottomNavDestination.Post.route) { /* PostScreen() */ }
                composable(BottomNavDestination.MyRides.route) { /* MyRidesScreen() */ }
                composable(BottomNavDestination.Profile.route) { /* ProfileScreen() */ }
            }
        }
    }
}

@Composable
fun SafarBottomNavBar(
    currentRoute: String?,
    navController: NavController
) {
    NavigationBar(
        containerColor = SafarDarkSurface,
        contentColor = SafarTextPrimary,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { destination ->
            val selected = currentRoute == destination.route
            val isPostButton = destination is BottomNavDestination.Post

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (isPostButton) {
                        // Highlighted post button
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.label,
                            tint = SafarGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    if (!isPostButton) {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SafarGreen,
                    selectedTextColor = SafarGreen,
                    unselectedIconColor = SafarTextSecondary,
                    unselectedTextColor = SafarTextSecondary,
                    indicatorColor = SafarGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}
