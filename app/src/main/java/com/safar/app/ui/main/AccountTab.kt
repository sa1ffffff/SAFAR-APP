package com.safar.app.ui.main

import com.safar.app.ui.terms.SUPPORT_EMAIL
import com.safar.app.ui.components.DetailSheet
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.safar.app.container
import com.safar.app.ui.Routes
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.Divider
import com.safar.app.ui.components.ListRow
import com.safar.app.ui.components.Pill
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar

@Composable
fun AccountTab(nav: NavHostController, onOpenRides: () -> Unit) {
    val app = LocalContext.current.container
    val c = Safar.colors
    val me by app.session.user.collectAsStateWithLifecycle()
    val rating by app.session.myRating.collectAsStateWithLifecycle()
    val unread by app.session.unreadNotifications.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<AccountSheet?>(null) }
    val user = me ?: return

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState())
            .padding(bottom = TabBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = Safar.type.display, color = c.content, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text(user.phone.ifBlank { user.email }, style = Safar.type.label, color = c.contentSecondary)
                }
                Avatar(user.name, user.photoUrl, size = 72.dp)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).background(c.surfaceMuted).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(if (rating.count == 0L) "—" else "%.1f".format(rating.average), "Rating", showStar = rating.count > 0)
                Stat(rating.count.toString(), if (rating.count == 1L) "Review" else "Reviews")
                Stat(
                    if (user.joinedAt > 0) java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date(user.joinedAt)) else "—",
                    "Joined",
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ListRow("Edit profile", { nav.navigate(Routes.EDIT_PROFILE) }, icon = Icons.Rounded.Edit)
        ListRow("Your rides", onOpenRides, icon = Icons.Rounded.DirectionsCar, subtitle = "Rides you've offered and booked")
        ListRow("Notifications", { nav.navigate(Routes.NOTIFICATIONS) }, icon = Icons.Rounded.Notifications,
            trailing = if (unread > 0) ({ Pill("$unread new", background = c.info, color = androidx.compose.ui.graphics.Color.White) }) else null)
        ListRow("Public profile", { nav.navigate(Routes.user(user.uid)) }, icon = Icons.Rounded.Star, subtitle = "How others see you")
        Divider(Modifier.padding(vertical = 8.dp, horizontal = 20.dp))
        ListRow("Safety", { nav.navigate(Routes.SAFETY) }, icon = Icons.Rounded.Shield)
        ListRow("Settings", { nav.navigate(Routes.SETTINGS) }, icon = Icons.Rounded.Settings)
        ListRow("Help", { nav.navigate(Routes.HELP) }, icon = Icons.AutoMirrored.Rounded.HelpOutline)
        ListRow("Contact support", { sheet = AccountSheet.Support }, icon = Icons.Rounded.MailOutline, subtitle = SUPPORT_EMAIL)
        ListRow("Terms of use", { nav.navigate(Routes.TERMS) }, icon = Icons.Rounded.Gavel)
        ListRow("About Safar", { nav.navigate(Routes.ABOUT) }, icon = Icons.Rounded.Info)
        Divider(Modifier.padding(vertical = 8.dp, horizontal = 20.dp))
        ListRow(
            "Buy the developer a coffee", { sheet = AccountSheet.Coffee },
            icon = Icons.Rounded.LocalCafe, subtitle = "Safar is free. Say thanks on NayaPay.",
        )
        Divider(Modifier.padding(vertical = 8.dp, horizontal = 20.dp))
        ListRow("Sign out", { confirmSignOut = true }, icon = Icons.AutoMirrored.Rounded.Logout, tint = c.negative)
    }

    val context = LocalContext.current
    when (sheet) {
        AccountSheet.Coffee -> DetailSheet(
            title = "Buy the developer a coffee",
            body = "Safar is built and run by one person, and it's free. If it saved you a trip's worth of fuel, a coffee keeps it going.",
            label = "NayaPay",
            value = NAYAPAY,
            note = "Saifullah Waseem",
            onDismiss = { sheet = null },
        )
        AccountSheet.Support -> DetailSheet(
            title = "Contact support",
            body = "Questions, a problem with a ride, or an idea for Safar. Write to us and we'll reply within a day.",
            label = "Email",
            value = SUPPORT_EMAIL,
            action = "Write an email",
            onAction = { sheet = null; emailSupport(context) },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?", style = Safar.type.headline) },
            text = { Text("You'll need your phone or Google account to sign back in.", style = Safar.type.body) },
            confirmButton = { TextButton({ confirmSignOut = false; app.auth.signOut() }) { Text("Sign out", color = c.negative) } },
            dismissButton = { TextButton({ confirmSignOut = false }) { Text("Cancel", color = c.content) } },
            containerColor = c.surfaceRaised,
        )
    }
}

private enum class AccountSheet { Coffee, Support }

private const val NAYAPAY = "0312-3639722"

/** Opens the user's mail app addressed to support, falling back quietly if none is installed. */
fun emailSupport(context: android.content.Context) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:$SUPPORT_EMAIL"))
        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Safar support")
    runCatching { context.startActivity(intent) }
        .onFailure { android.widget.Toast.makeText(context, "No email app found. Write to $SUPPORT_EMAIL", android.widget.Toast.LENGTH_LONG).show() }
}

@Composable
private fun Stat(value: String, label: String, showStar: Boolean = false) {
    val c = Safar.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showStar) Icon(Icons.Rounded.Star, null, Modifier.padding(top = 1.dp), tint = c.content)
            Text(value, style = Safar.type.title, color = c.content)
        }
        Text(label, style = Safar.type.caption, color = c.contentSecondary)
    }
}
