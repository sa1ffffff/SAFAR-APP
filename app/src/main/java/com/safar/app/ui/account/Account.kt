package com.safar.app.ui.account

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.safar.app.AppContainer
import com.safar.app.BuildConfig
import com.safar.app.R
import com.safar.app.container
import com.safar.app.data.ThemeMode
import com.safar.app.ui.Messages
import com.safar.app.ui.Routes
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.BottomActionBar
import com.safar.app.ui.components.Divider
import com.safar.app.ui.components.Field
import com.safar.app.ui.components.ListRow
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.components.Segmented
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.flow.MutableStateFlow

// ─── Settings ────────────────────────────────────────────────────────────────

class SettingsViewModel(private val app: AppContainer) : SafarViewModel() {
    val deleting = MutableStateFlow(false)

    fun deleteAccount() {
        val uid = app.session.uid ?: return
        deleting.value = true
        action(onError = { deleting.value = false }) {
            // Firebase refuses to delete a stale sign-in. Check that before wiping data,
            // so a refused delete never leaves an account with no profile.
            if (!app.auth.signedInRecently()) throw com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException(
                "ERROR_REQUIRES_RECENT_LOGIN", "Sign out and back in, then try again.",
            )
            app.users.deleteAllData(uid)
            app.auth.deleteAccount()
        }
    }
}

@Composable
fun SettingsScreen(nav: NavHostController) {
    val vm = appViewModel { SettingsViewModel(it) }
    Messages(vm)
    val app = LocalContext.current.container
    val context = LocalContext.current
    val c = Safar.colors
    val mode by app.preferences.themeMode.collectAsStateWithLifecycle()
    val deleting by vm.deleting.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold("Settings", onBack = { nav.back() }) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Appearance", style = Safar.type.headline, color = c.content)
                Spacer(Modifier.height(12.dp))
                Segmented(
                    listOf("System", "Light", "Dark"),
                    selected = ThemeMode.entries.indexOf(mode),
                    onSelect = { app.preferences.setThemeMode(ThemeMode.entries[it]) },
                )
            }
            Spacer(Modifier.height(16.dp))
            Divider(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            ListRow(
                "Notifications", icon = Icons.Rounded.Notifications, subtitle = "Choose what Safar can alert you about",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                },
                trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = c.contentTertiary) },
            )
            ListRow("Safety guidelines", { nav.navigate(Routes.SAFETY) }, icon = Icons.Rounded.Shield)
            Spacer(Modifier.height(8.dp))
            Divider(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            ListRow(
                "Delete account", { confirmDelete = true }, icon = Icons.Rounded.DeleteForever, tint = c.negative,
                subtitle = "Permanently removes your profile and rides",
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = false },
            title = { Text("Delete your account?", style = Safar.type.headline) },
            text = {
                Text(
                    "This permanently deletes your profile, photo, the rides you posted and your notifications, and signs you out. Messages you sent stay visible to the people you sent them to. This can't be undone.",
                    style = Safar.type.body,
                )
            },
            confirmButton = { TextButton({ confirmDelete = false; vm.deleteAccount() }, enabled = !deleting) { Text("Delete", color = c.negative) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel", color = c.content) } },
            containerColor = c.surfaceRaised,
        )
    }
}

// ─── Edit profile ────────────────────────────────────────────────────────────

class EditProfileViewModel(private val app: AppContainer) : SafarViewModel() {
    private val me = app.session.requireUser()
    val name = MutableStateFlow(me.name)
    val bio = MutableStateFlow(me.bio)
    val phone = MutableStateFlow(if (me.hasCallablePhone) me.phone else "")
    val photo = MutableStateFlow<Any?>(me.photoUrl)
    val saving = MutableStateFlow(false)

    fun save(onDone: () -> Unit) {
        val n = name.value.trim()
        if (n.length < 2) { say("Your name needs at least 2 letters."); return }
        val e164 = com.safar.app.ui.auth.normalizePhone("+92", phone.value.removePrefix("+92").trim())
            ?: phone.value.trim().takeIf { it.startsWith("+") && it.count(Char::isDigit) in 8..15 }
        if (e164 == null) { say("Enter a mobile number like 0300 1234567, so ride partners can call you."); return }
        saving.value = true
        action(onDone = { saving.value = false; onDone() }, onError = { saving.value = false }) {
            val updated = app.session.requireUser().copy(name = n, bio = bio.value.trim(), phone = e164)
            // Details first, so a failed photo upload never loses the rest of the edit.
            app.users.save(updated)
            (photo.value as? Uri)?.let { uri ->
                runCatching { app.users.uploadAvatar(updated.uid, uri) }.onFailure {
                    app.messages.tryEmit("Profile saved, but your photo couldn't upload. Try again later.")
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(nav: NavHostController) {
    val vm = appViewModel { EditProfileViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val name by vm.name.collectAsStateWithLifecycle()
    val bio by vm.bio.collectAsStateWithLifecycle()
    val phone by vm.phone.collectAsStateWithLifecycle()
    val photo by vm.photo.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.photo.value = uri }

    ScreenScaffold(
        "Edit profile", onBack = { nav.back() },
        bottomBar = { BottomActionBar { SafarButton("Save", { vm.save { nav.back() } }, loading = saving, modifier = Modifier.weight(1f)) } },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp).size(104.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            ) {
                if (photo != null) AsyncImage(photo, null, Modifier.size(104.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                else Avatar(name, null, size = 104.dp)
                Box(
                    Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(c.primary).border(3.dp, c.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PhotoCamera, "Change photo", Modifier.size(16.dp), tint = c.onPrimary) }
            }
            Field(name, { vm.name.value = it.take(40) }, label = "Full name",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            Field(
                phone, { vm.phone.value = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(16) },
                label = "Mobile number · riders can call or WhatsApp you from your rides",
                placeholder = "+92 300 1234567",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Field(bio, { vm.bio.value = it.take(160) }, label = "About you", placeholder = "Student at NUST, usually heading to Saddar",
                singleLine = false, minLines = 3, textStyle = Safar.type.body,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        }
    }
}

// ─── Help ────────────────────────────────────────────────────────────────────

private val faqs = listOf(
    "How does Safar work?" to "Drivers post rides with empty seats. Riders going the same way request a seat, the driver accepts, and you coordinate pickup in chat. Safar never handles money.",
    "How much should I pay?" to "Drivers can suggest a price per seat, or leave it to split fuel. Agree on the amount in chat before the ride and pay in person after pickup.",
    "Can I cancel a request?" to "Yes. Open the ride and tap Cancel request while it's pending. If the driver already accepted, message them so they can free your seat.",
    "When can I leave a review?" to "After the departure time, both of you mark the ride complete. Then each of you can rate the other once.",
    "Why can't I see someone's phone number?" to "Phone numbers are only shown once a driver accepts your request, to protect everyone's privacy.",
    "Someone made me feel unsafe" to "Leave the situation first. Then report them from their profile or the ride's menu. In an emergency, call 15.",
    "How do I delete my account?" to "Go to Account → Settings → Delete account. Your profile and photo are removed permanently.",
)

@Composable
fun HelpScreen(nav: NavHostController) {
    val c = Safar.colors
    val context = LocalContext.current
    var open by remember { mutableStateOf<Int?>(0) }
    ScreenScaffold("Help", onBack = { nav.back() }) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp)) {
            Text("Questions people ask", style = Safar.type.title, color = c.content, modifier = Modifier.padding(vertical = 12.dp))
            faqs.forEachIndexed { i, (q, a) ->
                val expanded = open == i
                val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "chevron")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.md)).clickable { open = if (expanded) null else i }.padding(vertical = 14.dp, horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q, style = Safar.type.bodyStrong, color = c.content, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ExpandMore, null, Modifier.graphicsLayer { rotationZ = rotation }, tint = c.contentSecondary)
                    }
                    AnimatedVisibility(expanded, enter = expandVertically(tween(220)) + fadeIn(tween(180)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
                        Text(a, style = Safar.type.body, color = c.contentSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Divider()
            }
            Spacer(Modifier.height(16.dp))
            ListRow(
                "Email support", icon = Icons.Rounded.Mail, subtitle = "We usually reply within a day",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "Safar support"))
                },
            )
        }
    }
}

// ─── About ───────────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(nav: NavHostController) {
    val c = Safar.colors
    ScreenScaffold("About", onBack = { nav.back() }) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(22.dp)).background(c.primary), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(104.dp), colorFilter = ColorFilter.tint(c.onPrimary))
            }
            Spacer(Modifier.height(16.dp))
            Text("Safar", style = Safar.type.title, color = c.content)
            Text("Version ${BuildConfig.VERSION_NAME}", style = Safar.type.label, color = c.contentSecondary)
            Spacer(Modifier.height(24.dp))
            Text(
                "Safar (سفر, \"journey\") is a community carpool app for Pakistan. It connects drivers who have empty seats with people heading the same way, so trips cost less and roads carry fewer cars.",
                style = Safar.type.body, color = c.content, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Built by Saifullah Waseem at SEECS, NUST.",
                style = Safar.type.label, color = c.contentSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "Inter typeface © The Inter Project Authors, SIL Open Font License 1.1. Map data © Google.",
                style = Safar.type.caption, color = c.contentTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

