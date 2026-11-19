package com.safar.app.ui.terms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safar.app.container
import com.safar.app.data.Gate
import com.safar.app.ui.components.ButtonKind
import com.safar.app.ui.components.Divider
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.haptics
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.launch

/** Bump when the terms change materially; every account is asked to agree again. */
const val TERMS_VERSION = 1
private const val EFFECTIVE = "20 November 2026"
const val SUPPORT_EMAIL = "safar.support@gmail.com"

/**
 * Shown once after sign-in, before anything else. The account can't go further
 * without agreeing; declining signs out.
 */
@Composable
fun TermsGateScreen() {
    val app = LocalContext.current.container
    val c = Safar.colors
    val haptics = haptics()
    val scope = rememberCoroutineScope()
    var agreed by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(36.dp))
            Text("TERMS OF USE", style = Safar.type.overline, color = c.contentSecondary)
            Spacer(Modifier.height(10.dp))
            Text("Before your first ride", style = Safar.type.display, color = c.content)
            Spacer(Modifier.height(10.dp))
            Text(
                "Safar connects people who are going the same way. Here's how that works, and what Safar is and isn't responsible for. You'll only be asked once.",
                style = Safar.type.body, color = c.contentSecondary,
            )
            Spacer(Modifier.height(28.dp))
            Summary()
            Spacer(Modifier.height(36.dp))
            FullTerms()
            Spacer(Modifier.height(210.dp))
        }

        // Keeps scrolled text from running under the status bar.
        Box(Modifier.fillMaxWidth().background(c.background).windowInsetsTopHeight(WindowInsets.statusBars))

        // Agreement bar: fixed, so the choice is always in reach while reading.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(c.background)
                .navigationBarsPadding(),
        ) {
            Divider()
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.sm))
                        .toggleable(agreed, role = Role.Checkbox) { agreed = it; haptics.tick() },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Check(agreed, Modifier.padding(top = 1.dp))
                    Text(
                        "I've read and agree to the Terms of Use, including that Safar only connects users and isn't liable for what happens on a ride.",
                        style = Safar.type.label, color = c.content,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SafarButton(
                        "Decline", { app.auth.signOut() },
                        kind = ButtonKind.Ghost, modifier = Modifier.widthIn(min = 112.dp),
                    )
                    SafarButton(
                        "Agree and continue",
                        onClick = {
                            val uid = (app.session.gate.value as? Gate.NeedsTerms)?.uid ?: return@SafarButton
                            saving = true
                            scope.launch {
                                runCatching { app.users.acceptTerms(uid, TERMS_VERSION) }
                                    .onSuccess { haptics.success() }
                                    .onFailure { app.messages.tryEmit("Couldn't save that. Check your connection and try again.") }
                                saving = false
                            }
                        },
                        enabled = agreed, loading = saving, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** The same terms, read-only, from Account. */
@Composable
fun TermsScreen(onBack: () -> Unit) {
    ScreenScaffold("Terms of use", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Summary()
            Spacer(Modifier.height(36.dp))
            FullTerms()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Check(checked: Boolean, modifier: Modifier = Modifier) {
    val c = Safar.colors
    val fill by animateColorAsState(if (checked) c.primary else c.surface, tween(140), label = "check-fill")
    val edge by animateColorAsState(if (checked) c.primary else c.contentTertiary, tween(140), label = "check-edge")
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.size(22.dp).clip(shape).background(fill).border(1.5.dp, edge, shape), contentAlignment = Alignment.Center) {
        AnimatedVisibility(checked, enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.6f), exit = fadeOut(tween(90))) {
            Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = c.onPrimary)
        }
    }
}

private val summary = listOf(
    "Safar connects, it doesn't drive." to
        "We help drivers and riders find each other. We don't provide transport, employ drivers or own vehicles.",
    "Arrangements are between you." to
        "Fares, pickup points and timing are agreed between users. Safar doesn't collect or hold any payment.",
    "You travel at your own risk." to
        "As far as the law allows, Safar isn't responsible for loss, damage, injury or disputes arising from a ride.",
)

@Composable
private fun Summary() {
    val c = Safar.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.lg)).background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(Radii.lg)),
    ) {
        Text(
            "IN SHORT", style = Safar.type.overline, color = c.contentSecondary,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp),
        )
        summary.forEachIndexed { i, (head, body) ->
            if (i > 0) Divider(Modifier.padding(horizontal = 18.dp))
            Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "0${i + 1}", style = Safar.type.label.copy(fontFeatureSettings = "tnum"), color = c.contentTertiary,
                    modifier = Modifier.width(20.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(head, style = Safar.type.bodyStrong, color = c.content)
                    Text(body, style = Safar.type.label, color = c.contentSecondary)
                }
            }
        }
    }
}

@Composable
private fun FullTerms() {
    val c = Safar.colors
    Text("Full terms", style = Safar.type.title, color = c.content)
    Spacer(Modifier.height(4.dp))
    Text("Version $TERMS_VERSION · Effective $EFFECTIVE", style = Safar.type.caption, color = c.contentTertiary)
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        sections.forEachIndexed { i, (head, paragraphs) -> Section(i + 1, head, paragraphs) }
    }
}

@Composable
private fun Section(n: Int, head: String, paragraphs: List<String>) {
    val c = Safar.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$n.", style = Safar.type.bodyStrong.copy(fontFeatureSettings = "tnum"), color = c.contentTertiary)
            Text(head, style = Safar.type.bodyStrong, color = c.content)
        }
        paragraphs.forEach { Paragraph(it) }
    }
}

@Composable
private fun ColumnScope.Paragraph(text: String) {
    val c = Safar.colors
    // A leading "• " marks a list item; it gets a hanging indent so wrapped lines align.
    if (text.startsWith("• ")) {
        Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("•", style = Safar.type.body, color = c.contentTertiary)
            Text(text.removePrefix("• "), style = Safar.type.body, color = c.contentSecondary)
        }
    } else {
        Text(text, style = Safar.type.body, color = c.contentSecondary)
    }
}

private val sections: List<Pair<String, List<String>>> = listOf(
    "About these terms" to listOf(
        "These Terms of Use (\"Terms\") are an agreement between you and Safar (\"Safar\", \"we\", \"us\") and govern your use of the Safar mobile application and related services (the \"Service\"). By creating an account or using the Service, you confirm that you have read, understood and agree to be bound by these Terms. If you do not agree, do not use the Service.",
    ),
    "What Safar is, and isn't" to listOf(
        "Safar is a technology platform that lets people who are travelling the same way find each other and share the cost of a journey. Drivers post seats on journeys they are already making; riders request to join.",
        "Safar is not a transport provider, taxi service, carrier or travel agent. We do not employ, engage or act as agent for any driver, we do not own or operate vehicles, and we do not provide or supervise any journey. Any arrangement to travel is made solely between the users involved, and Safar is not a party to it.",
    ),
    "Eligibility and your account" to listOf(
        "You must be at least 18 years old and legally able to enter into a binding agreement. You agree to give accurate information, including your name and a mobile number you can be reached on, to keep it current, and to keep your account secure. You are responsible for all activity on your account. One person may hold only one account.",
    ),
    "Drivers" to listOf(
        "If you offer seats, you confirm that, for every journey:",
        "• you hold a valid driving licence for the vehicle you drive;",
        "• the vehicle is roadworthy, registered and covered by any insurance required by law;",
        "• you will drive safely and obey all traffic laws;",
        "• you are sharing costs for a journey you would make anyway, not carrying passengers for profit or as a commercial service.",
        "You are solely responsible for the journey, the vehicle and your conduct, and for complying with any laws that apply to you.",
    ),
    "Riders" to listOf(
        "If you join a ride, you agree to be at the agreed pickup point on time, to treat the driver and other passengers with respect, to follow reasonable requests about the vehicle, and to pay any contribution you agreed with the driver.",
    ),
    "Payments" to listOf(
        "Any contribution towards a journey is agreed and paid directly between users. Safar does not set fares, collect, hold, transfer or guarantee any payment, and is not responsible for any payment, non-payment, refund or dispute between users. Never pay in advance to someone you have not met.",
    ),
    "Safety" to listOf(
        "Safar does not carry out background checks, verify identities, driving records or vehicles, or inspect any user. Profiles, ratings and reviews are provided by users and may be inaccurate. You are responsible for deciding whether to travel with someone and for taking reasonable precautions: meet in public places, share your trip with someone you trust, and trust your instincts. In an emergency, contact the police on 15 or Rescue on 1122.",
    ),
    "Your conduct" to listOf(
        "You agree not to use the Service to:",
        "• break any law, or post anything false, misleading, offensive or unlawful;",
        "• harass, threaten, discriminate against or harm anyone;",
        "• run a commercial transport business or advertise unrelated goods or services;",
        "• impersonate anyone, or create accounts by automated means;",
        "• interfere with, copy or attempt to gain unauthorised access to the Service.",
    ),
    "Disclaimer" to listOf(
        "The Service is provided \"as is\" and \"as available\". To the fullest extent permitted by law, Safar makes no warranties of any kind, express or implied, including about the accuracy of any listing, the availability of any ride, or the conduct, identity or reliability of any user. Maps, routes, distances and times are estimates.",
    ),
    "Limitation of liability" to listOf(
        "To the fullest extent permitted by law, Safar, its developers and affiliates shall not be liable for any loss or damage arising from or connected with a journey or your use of the Service, including:",
        "• personal injury, illness or death;",
        "• loss of, theft of or damage to property or vehicles;",
        "• road accidents, delays, cancellations or no-shows;",
        "• the acts, omissions or conduct of any user or third party;",
        "• any payment dispute between users;",
        "• any indirect, incidental, special or consequential loss, including loss of earnings or opportunity.",
        "Where liability cannot lawfully be excluded, Safar's total liability to you for all claims is limited to the amount, if any, you paid Safar in the twelve months before the claim arose. Nothing in these Terms limits liability that cannot be limited under applicable law.",
    ),
    "Indemnity" to listOf(
        "You agree to indemnify and hold harmless Safar and its developers from any claim, loss, liability or expense (including reasonable legal fees) arising from your journeys, your use of the Service, your content, or your breach of these Terms or of any law.",
    ),
    "Your information" to listOf(
        "To run the Service we store your name, email address, mobile number, profile photo, the rides you post or join, your messages, ratings and reviews, and the locations you choose for a ride. Your name, photo, rating and mobile number are visible to other signed-in users so they can arrange a ride with you. We use this information only to operate and improve the Service and to keep it safe, and we do not sell it. You can delete your account at any time from Settings.",
    ),
    "Suspension and termination" to listOf(
        "We may remove content, or suspend or close any account, at our discretion and without notice, including where we believe a user has breached these Terms or put others at risk. You may stop using the Service and delete your account at any time.",
    ),
    "Changes" to listOf(
        "We may update these Terms from time to time. If a change is material, we will ask you to review and accept the new Terms before you continue using the Service.",
    ),
    "Governing law" to listOf(
        "These Terms are governed by the laws of the Islamic Republic of Pakistan. Any dispute shall be subject to the exclusive jurisdiction of the courts at Islamabad.",
    ),
    "Contact" to listOf(
        "Questions about these Terms can be sent to $SUPPORT_EMAIL.",
    ),
)
