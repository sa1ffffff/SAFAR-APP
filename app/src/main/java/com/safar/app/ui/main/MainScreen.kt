package com.safar.app.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.safar.app.ui.components.LensStyle
import com.safar.app.ui.components.LiquidSelector
import com.safar.app.ui.components.lerpColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.safar.app.container
import com.safar.app.ui.components.Glass
import com.safar.app.ui.components.LocalHaze
import com.safar.app.ui.theme.Safar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Rides("Rides", Icons.Outlined.DirectionsCar, Icons.Rounded.DirectionsCar),
    Inbox("Inbox", Icons.Outlined.ChatBubbleOutline, Icons.Rounded.ChatBubble),
    Account("Account", Icons.Outlined.Person, Icons.Rounded.Person),
}

/** Height the floating tab bar occupies above the navigation inset; tab content pads by this. */
val TabBarSpace = 96.dp

@Composable
fun MainScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var ridesQuery by rememberSaveable { mutableStateOf<String?>(null) }
    val session = LocalContext.current.container.session
    val unread by session.unreadMessages.collectAsStateWithLifecycle()
    val haze = remember { HazeState() }
    AskForNotificationPermission()

    Box(Modifier.fillMaxSize().background(Safar.colors.background)) {
        // The map tab can't be blurred (it draws on its own surface), so only the other tabs feed the glass.
        CompositionLocalProvider(LocalHaze provides if (tab == Tab.Home) null else haze) {
            // Glass inside the blurred content can't itself blur (Haze forbids nesting), so content gets none.
            Box(Modifier.fillMaxSize().then(if (tab == Tab.Home) Modifier else Modifier.haze(haze))) {
                CompositionLocalProvider(LocalHaze provides null) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "tab",
                ) { current ->
                    when (current) {
                        Tab.Home -> HomeTab(nav, onSearch = { ridesQuery = ""; tab = Tab.Rides }, onOpenAccount = { tab = Tab.Account })
                        Tab.Rides -> RidesTab(nav, focusSearch = ridesQuery != null, onSearchFocused = { ridesQuery = null })
                        Tab.Inbox -> InboxTab(nav)
                        Tab.Account -> AccountTab(nav, onOpenRides = { tab = Tab.Rides })
                    }
                }
                }
            }
            TabBar(
                selected = tab,
                onSelect = { tab = it },
                inboxBadge = unread,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit, inboxBadge: Long, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Glass(
        modifier.navigationBarsPadding().padding(bottom = 10.dp).padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        elevation = 18.dp,
    ) {
        LiquidSelector(
            count = Tab.entries.size,
            selected = selected.ordinal,
            onSelect = { onSelect(Tab.entries[it]) },
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(6.dp),
            spacing = 4.dp,
            lens = LensStyle.tint(26.dp),
        ) { i, focus, pressed ->
            val t = Tab.entries[i]
            val fg = lerpColor(c.contentTertiary, c.content, focus)
            // Icons dip slightly under the finger, and lift a touch as the lens arrives.
            val press by animateFloatAsState(if (pressed) 0.88f else 1f, tween(if (pressed) 90 else 200), label = "tab-press")
            Column(
                Modifier.fillMaxSize().graphicsLayer { val s = press * (1f + 0.04f * focus); scaleX = s; scaleY = s },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box {
                    Icon(if (focus > 0.5f) t.selectedIcon else t.icon, t.label, Modifier.size(22.dp), tint = fg)
                        if (t == Tab.Inbox && inboxBadge > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(start = 14.dp).size(8.dp).clip(CircleShape).background(c.info)
                            )
                        }
                    }
                Text(t.label, style = Safar.type.caption, color = fg, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun AskForNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
