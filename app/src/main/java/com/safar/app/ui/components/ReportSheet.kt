package com.safar.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safar.app.ui.theme.Safar

private val reasons = listOf("Unsafe driving", "Harassment", "No-show", "Fake profile", "Scam or payment", "Something else")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportSheet(title: String, onDismiss: () -> Unit, onSubmit: (reason: String, details: String) -> Unit) {
    val c = Safar.colors
    var reason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceRaised,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(title, style = Safar.type.title, color = c.content)
            Spacer(Modifier.height(6.dp))
            Text("Reports are private. Our team reviews every one.", style = Safar.type.label, color = c.contentSecondary)
            Spacer(Modifier.height(20.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { r -> ChoiceChip(r, reason == r, { reason = r }) }
            }
            Spacer(Modifier.height(16.dp))
            Field(details, { details = it.take(1000) }, label = "What happened? (optional)", singleLine = false, minLines = 3, textStyle = Safar.type.body)
            Spacer(Modifier.height(20.dp))
            SafarButton(
                "Send report", onClick = { reason?.let { onSubmit(it, details) } }, enabled = reason != null,
                kind = ButtonKind.Danger, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
