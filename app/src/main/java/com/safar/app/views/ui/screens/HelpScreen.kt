package com.safar.app.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.safar.app.views.activities.FaqCategory
import com.safar.app.views.activities.FaqItem
import com.safar.app.views.ui.theme.*

/**
 * SAFAR HELP & FAQ SCREEN
 * Professional, modern FAQ interface with expandable cards
 * Uber-inspired dark theme with smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    faqsByCategory: Map<FaqCategory, List<FaqItem>> = emptyMap(),
    expandedIndex: Int = -1,
    onToggleFaq: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Support",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header section
            item {
                HelpHeader()
            }

            // Flatten FAQs with category headers
            val sortedCategories = faqsByCategory.keys.sortedBy { it.ordinal }
            var globalIndex = 0

            sortedCategories.forEach { category ->
                val faqs = faqsByCategory[category] ?: emptyList()
                
                // Category header
                item(key = "category_${category.name}") {
                    CategoryHeader(category = category)
                }

                // FAQ items
                itemsIndexed(
                    items = faqs,
                    key = { index, faq -> "faq_${category.name}_$index" }
                ) { _, faq ->
                    val currentIndex = globalIndex++
                    FaqCard(
                        faq = faq,
                        isExpanded = expandedIndex == currentIndex,
                        onClick = { onToggleFaq(currentIndex) }
                    )
                }

                // Spacing after category
                item(key = "spacer_${category.name}") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Contact support section
            item {
                ContactSupportCard()
            }
        }
    }
}

@Composable
private fun HelpHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Help,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Yellow
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Find answers to common questions about SAFAR",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CategoryHeader(category: FaqCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Green
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Green
        )
    }
}

@Composable
private fun FaqCard(
    faq: FaqItem,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "chevron_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Question row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = TextMuted
                )
            }

            // Answer (animated)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(300)
                )
            ) {
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun ContactSupportCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface2
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.HeadsetMic,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Yellow
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Still need help?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Contact our support team",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* Email intent will be handled by activity */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "support@safar.app",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Returns appropriate icon for each FAQ category.
 */
private fun getCategoryIcon(category: FaqCategory): ImageVector {
    return when (category) {
        FaqCategory.GETTING_STARTED -> Icons.Rounded.Rocket
        FaqCategory.POSTING_RIDES -> Icons.Rounded.AddCircle
        FaqCategory.FINDING_RIDES -> Icons.Rounded.Search
        FaqCategory.REQUESTS -> Icons.Rounded.Send
        FaqCategory.CHAT -> Icons.Rounded.Chat
        FaqCategory.SAFETY -> Icons.Rounded.Shield
        FaqCategory.PAYMENTS -> Icons.Rounded.Payments
        FaqCategory.ACCOUNT -> Icons.Rounded.AccountCircle
        FaqCategory.TROUBLESHOOTING -> Icons.Rounded.Build
    }
}
