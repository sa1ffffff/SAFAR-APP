package com.safar.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.safar.app.R
import com.safar.app.container
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar

private data class Guideline(val icon: ImageVector, val title: String, val body: String)

private val guidelines = listOf(
    Guideline(Icons.Rounded.LocationOn, "Meet somewhere public", "Pick up and drop off at busy, well-lit places, at least the first time."),
    Guideline(Icons.Rounded.Payments, "Agree on the cost first", "Settle the fuel share before you leave. Never pay in advance to someone you haven't met."),
    Guideline(Icons.Rounded.VerifiedUser, "Check who you're riding with", "Look at their profile and reviews, and confirm their name at pickup."),
    Guideline(Icons.Rounded.Handshake, "Share only what's needed", "Keep personal details to yourself until you're comfortable. Chat stays in the app."),
)

@Composable
fun WelcomeScreen() {
    val prefs = LocalContext.current.container.preferences
    val c = Safar.colors
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(40.dp))
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.primary), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(72.dp),
                    colorFilter = ColorFilter.tint(c.onPrimary),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text("Share the road.", style = Safar.type.display, color = c.content)
            Spacer(Modifier.height(10.dp))
            Text(
                "Safar connects drivers with empty seats to people heading the same way. Before you start, a few ground rules that keep everyone safe.",
                style = Safar.type.body, color = c.contentSecondary,
            )
            Spacer(Modifier.height(32.dp))
            guidelines.forEachIndexed { i, g ->
                AnimatedVisibility(
                    visible = shown,
                    enter = fadeIn(tween(260, delayMillis = 80 + i * 60)) +
                        slideInVertically(tween(260, delayMillis = 80 + i * 60)) { it / 6 },
                ) {
                    GuidelineRow(g)
                }
            }
        }
        Text(
            "By continuing you agree to follow these community guidelines.",
            style = Safar.type.caption, color = c.contentTertiary,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        SafarButton("Get started", onClick = prefs::setOnboarded, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GuidelineRow(g: Guideline) {
    val c = Safar.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
            Icon(g.icon, null, Modifier.size(22.dp), tint = c.content)
        }
        Column(Modifier.weight(1f)) {
            Text(g.title, style = Safar.type.bodyStrong, color = c.content)
            Spacer(Modifier.height(2.dp))
            Text(g.body, style = Safar.type.label, color = c.contentSecondary)
        }
    }
}

/** The same guidelines, reachable any time from Account. */
@Composable
fun SafetyScreen(onDone: () -> Unit) {
    val c = Safar.colors
    ScreenScaffold("Safety", onBack = onDone) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Ride with confidence", style = Safar.type.title, color = c.content)
            Spacer(Modifier.height(8.dp))
            Text(
                "Safar is a community. These habits keep it a good one.",
                style = Safar.type.body, color = c.contentSecondary,
            )
            Spacer(Modifier.height(16.dp))
            guidelines.forEach { GuidelineRow(it) }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).background(c.surfaceMuted).padding(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Groups, null, tint = c.content)
                    Text(
                        "If someone makes you feel unsafe, leave the ride and report them from their profile. In an emergency, call 15.",
                        style = Safar.type.label, color = c.content,
                    )
                }
            }
        }
    }
}
