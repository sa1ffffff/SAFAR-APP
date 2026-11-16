package com.safar.app.ui.review

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.safar.app.AppContainer
import com.safar.app.data.model.JoinRequest
import com.safar.app.ui.Messages
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.back
import com.safar.app.ui.components.Avatar
import com.safar.app.ui.components.BottomActionBar
import com.safar.app.ui.components.Field
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.ScreenScaffold
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ReviewViewModel(private val app: AppContainer, requestId: String) : SafarViewModel() {
    val uid = app.session.uid.orEmpty()
    val request: StateFlow<JoinRequest?> = app.requests.observe(requestId).catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val sending = MutableStateFlow(false)

    /** The person being reviewed, loaded from their live profile so both sides see a real name and photo. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val other: StateFlow<com.safar.app.data.model.User?> = request
        .map { it?.otherParty(uid) }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id.isNullOrBlank()) flowOf(null) else app.users.observe(id).catch { emit(null) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun submit(rating: Int, comment: String, onDone: () -> Unit) {
        val r = request.value ?: return
        sending.value = true
        action(onDone = { sending.value = false; cheer(big = true); onDone() }, onError = { sending.value = false }) {
            app.reviews.submit(r, app.session.requireUser(), rating, comment)
        }
    }
}

private val labels = listOf("", "Poor", "Not great", "Okay", "Good", "Excellent")

@Composable
fun ReviewScreen(requestId: String, nav: NavHostController) {
    val vm = appViewModel(key = "review-$requestId") { ReviewViewModel(it, requestId) }
    Messages(vm)
    val c = Safar.colors
    val request by vm.request.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    var rating by remember { mutableIntStateOf(0) }
    val haptics = com.safar.app.ui.haptics()
    var comment by remember { mutableStateOf("") }
    val r = request
    val other by vm.other.collectAsStateWithLifecycle()
    val isDriver = r?.adCreatorUid == vm.uid
    val otherName = other?.firstName?.takeIf { other?.name?.isNotBlank() == true }
        ?: if (isDriver) r?.requestorName?.substringBefore(' ').orEmpty() else "your driver"

    ScreenScaffold(
        "Rate your ride", onBack = { nav.back() },
        bottomBar = {
            BottomActionBar {
                SafarButton(
                    "Submit review", { vm.submit(rating, comment) { nav.back() } },
                    enabled = rating > 0 && r?.canReview(vm.uid) == true, loading = sending, modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Avatar(other?.name ?: r?.requestorName.orEmpty(), other?.photoUrl, size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text("How was riding with $otherName?", style = Safar.type.title, color = c.content, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (r != null && r.routeLabel.isNotBlank()) Text(
                com.safar.app.ui.shortRoute(r.routeLabel),
                style = Safar.type.label, color = c.contentSecondary,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { i ->
                    val on = i <= rating
                    // Each star pops slightly as it fills: the one moment in the flow worth celebrating.
                    val scale by animateFloatAsState(if (on) 1f else 0.9f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), label = "star")
                    Icon(
                        if (on) Icons.Rounded.Star else Icons.Rounded.StarOutline, "$i stars",
                        tint = if (on) c.content else c.contentTertiary,
                        modifier = Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(remember { MutableInteractionSource() }, indication = null) { haptics.rating(i); rating = i },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(labels[rating], style = Safar.type.bodyStrong, color = c.content, modifier = Modifier.height(22.dp))
            Spacer(Modifier.height(24.dp))
            Field(
                comment, { comment = it.take(500) }, label = "Add a comment (optional)",
                placeholder = "On time, friendly, smooth drive…", singleLine = false, minLines = 3, textStyle = Safar.type.body,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (r != null && !r.canReview(vm.uid)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (!r.isCompleted) "You can review once both of you mark the ride complete." else "You've already reviewed this ride.",
                    style = Safar.type.label, color = c.contentSecondary,
                )
            }
        }
    }
}
