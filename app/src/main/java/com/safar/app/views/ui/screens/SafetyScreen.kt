package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.safar.app.views.activities.SafetyGuideline
import com.safar.app.views.ui.theme.*

// ─── Internal guideline item with ImageVector icon ────────────────────────────
private data class SafetyGuidelineItem(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private val guidelineItems = listOf(
    SafetyGuidelineItem(
        Icons.Rounded.VerifiedUser,
        "Verified Community",
        "All users are verified via phone or Google. Share rides only with verified members."
    ),
    SafetyGuidelineItem(
        Icons.Rounded.Chat,
        "Communicate Clearly",
        "Use the in-app chat to coordinate. Never share personal financial details."
    ),
    SafetyGuidelineItem(
        Icons.Rounded.ReportProblem,
        "Report Issues",
        "Use the report button if you feel unsafe. Our team reviews all reports within 24 hours."
    ),
    SafetyGuidelineItem(
        Icons.Rounded.Groups,
        "Respect Others",
        "Treat fellow riders with respect. Harassment of any kind will result in account removal."
    ),
    SafetyGuidelineItem(
        Icons.Rounded.LocationOn,
        "Share Your Route",
        "Let someone you trust know your travel plans when carpooling with new people."
    ),
    SafetyGuidelineItem(
        Icons.Rounded.Shield,
        "Trust Your Instincts",
        "If something feels wrong, don't proceed. Your safety is always the top priority."
    )
)

/**
 * SAFAR SAFETY SCREEN
 * Enterprise-level safety guidelines with Uber-inspired design
 *
 * Signature matches SafetyBridge.kt's call:
 *   SafetyScreen(guidelines, isChecked, hasScrolledToBottom,
 *                onCheckedChange, onScrolledToBottom, onIUnderstand, onBack)
 */
@Composable
fun SafetyScreen(
    guidelines: List<SafetyGuideline> = emptyList(),
    isChecked: Boolean = false,
    hasScrolledToBottom: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onScrolledToBottom: () -> Unit = {},
    onIUnderstand: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    // Use internal guideline items with ImageVector icons for rendering
    val displayItems = if (guidelines.isNotEmpty()) {
        guidelines.map { g ->
            SafetyGuidelineItem(
                icon = mapIconName(g.icon),
                title = g.title,
                body = g.description
            )
        }
    } else {
        guidelineItems
    }

    SafarTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SafarBlack)
        ) {
            // ── Top Bar ────────────────────────────────────────────────────────
            Surface(
                color = SafarDarkSurface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SafarTextPrimary
                        )
                    }
                }
            }

            // ── Header ────────────────────────────────────────────────────────
            Surface(
                color = SafarDarkSurface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shield Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SafarGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            null,
                            tint = SafarGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Community Safety",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = SafarTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Please read before your first ride",
                        fontSize = 14.sp,
                        color = SafarTextSecondary
                    )
                }
            }

            // ── Guidelines List ───────────────────────────────────────────────
            val scrollState = rememberScrollState()

            // Track scroll to bottom
            LaunchedEffect(scrollState.value) {
                if (!hasScrolledToBottom &&
                    scrollState.maxValue > 0 &&
                    scrollState.value >= scrollState.maxValue - 50
                ) {
                    onScrolledToBottom()
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                displayItems.forEachIndexed { index, item ->
                    SafetyGuidelineCard(
                        item = item,
                        index = index + 1
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Bottom Section ────────────────────────────────────────────────
            Surface(
                color = SafarDarkSurface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    // Checkbox Row
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SafarDarkCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCheckedChange(!isChecked) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = onCheckedChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SafarGreen,
                                    checkmarkColor = SafarBlack,
                                    uncheckedColor = SafarTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "I understand and agree to follow the community safety guidelines",
                                color = SafarTextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Continue Button
                    Button(
                        onClick = onIUnderstand,
                        enabled = isChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafarGreen,
                            contentColor = SafarBlack,
                            disabledContainerColor = SafarGreen.copy(alpha = 0.25f),
                            disabledContentColor = SafarBlack.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.ArrowForward,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Maps string icon name to ImageVector
 */
private fun mapIconName(name: String): ImageVector {
    return when (name) {
        "shield_check", "shield" -> Icons.Rounded.Shield
        "share" -> Icons.Rounded.Share
        "visibility" -> Icons.Rounded.Visibility
        "emergency" -> Icons.Rounded.Warning
        "airline_seat_recline_normal" -> Icons.Rounded.EventSeat
        "verified_user" -> Icons.Rounded.VerifiedUser
        "chat" -> Icons.Rounded.Chat
        "report_problem" -> Icons.Rounded.ReportProblem
        "groups" -> Icons.Rounded.Groups
        "location_on" -> Icons.Rounded.LocationOn
        else -> Icons.Rounded.Info
    }
}

@Composable
private fun SafetyGuidelineCard(
    item: SafetyGuidelineItem,
    index: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafarDarkCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon with number
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SafarGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    null,
                    tint = SafarGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SafarTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    item.body,
                    fontSize = 13.sp,
                    color = SafarTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
