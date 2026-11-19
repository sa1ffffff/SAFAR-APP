package com.safar.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.delay

/**
 * A small sheet that hands over one piece of information: what it is, the value itself,
 * set large enough to read across a room and copyable with one tap, and an optional action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    title: String,
    body: String,
    label: String,
    value: String,
    note: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val c = Safar.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceRaised,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(title, style = Safar.type.title, color = c.content)
            Spacer(Modifier.height(6.dp))
            Text(body, style = Safar.type.body, color = c.contentSecondary)
            Spacer(Modifier.height(20.dp))
            CopyValue(label, value, note)
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                SafarButton(action, onAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CopyValue(label: String, value: String, note: String?) {
    val c = Safar.colors
    val context = LocalContext.current
    val haptics = com.safar.app.ui.haptics()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    val shape = RoundedCornerShape(Radii.lg)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.border, shape)
            .pressable(remember { MutableInteractionSource() }, onClick = {
                copy(context, label, value); haptics.success(); copied = true
            }, pressedScale = 0.985f)
            .padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(), style = Safar.type.overline, color = c.contentSecondary)
            // Long values (an email) step down a size so they never clip beside the Copy control.
            val style = if (value.length > 16) Safar.type.headline else Safar.type.title
            Text(value, style = style.copy(fontFeatureSettings = "tnum"), color = c.content, maxLines = 1)
            if (note != null) Text(note, style = Safar.type.label, color = c.contentSecondary)
        }
        AnimatedContent(
            copied,
            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(90)) },
            label = "copy",
        ) { done ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, null, Modifier.size(18.dp), tint = c.content)
                Text(if (done) "Copied" else "Copy", style = Safar.type.label, color = c.content)
            }
        }
    }
}

private fun copy(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
