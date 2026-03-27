package com.safar.app.views.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR Reusable Components Library
// Premium UI components for production-level design
// ══════════════════════════════════════════════════════════════════════════════

// ── Primary Button ────────────────────────────────────────────────────────────
@Composable
fun SafarPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "buttonScale"
    )

    Button(
        onClick = {
            if (!isLoading) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(SafarDimens.buttonHeightLg)
            .scale(scale),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(SafarDimens.radiusLg),
        colors = ButtonDefaults.buttonColors(
            containerColor = SafarColors.primary,
            contentColor = Color(0xFF1A1A1A),
            disabledContainerColor = SafarColors.primary.copy(alpha = 0.4f),
            disabledContentColor = Color(0xFF1A1A1A).copy(alpha = 0.4f)
        ),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF1A1A1A),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                trailingIcon?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Secondary Button ──────────────────────────────────────────────────────────
@Composable
fun SafarSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "buttonScale"
    )

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(SafarDimens.buttonHeightLg)
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(SafarDimens.radiusLg),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SafarColors.textPrimary
        ),
        border = BorderStroke(1.5.dp, SafarColors.divider),
        interactionSource = interactionSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

// ── Text Button ───────────────────────────────────────────────────────────────
@Composable
fun SafarTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SafarColors.primary
) {
    val haptic = LocalHapticFeedback.current
    
    TextButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = color
        )
    }
}

// ── Icon Button ───────────────────────────────────────────────────────────────
@Composable
fun SafarIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = SafarColors.textPrimary,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    backgroundColor: Color = SafarColors.surfaceElevated
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "iconScale"
    )

    Surface(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(SafarDimens.radiusMd),
        color = backgroundColor,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// ── Floating Action Button ────────────────────────────────────────────────────
@Composable
fun SafarFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    extended: Boolean = false,
    text: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "fabScale"
    )

    if (extended && text.isNotEmpty()) {
        ExtendedFloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier.scale(scale),
            containerColor = SafarColors.primary,
            contentColor = Color(0xFF1A1A1A),
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription
                )
            },
            text = {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold
                )
            },
            interactionSource = interactionSource
        )
    } else {
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier.scale(scale),
            containerColor = SafarColors.primary,
            contentColor = Color(0xFF1A1A1A),
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    }
}

// ── Card Component ────────────────────────────────────────────────────────────
@Composable
fun SafarCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cardElevation"
    )

    Surface(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        ),
        shape = RoundedCornerShape(SafarDimens.radiusLg),
        color = SafarColors.surfaceElevated,
        tonalElevation = 2.dp,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(SafarDimens.paddingCard),
            content = content
        )
    }
}

// ── Input Field ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { 
                { Text(it, color = SafarColors.textTertiary) } 
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = SafarColors.textSecondary
                    )
                }
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = SafarColors.textSecondary
                        )
                    }
                }
            },
            isError = isError,
            shape = RoundedCornerShape(SafarDimens.radiusMd),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SafarColors.primary,
                unfocusedBorderColor = SafarColors.divider,
                focusedContainerColor = SafarColors.surfaceElevated,
                unfocusedContainerColor = SafarColors.surfaceElevated,
                errorBorderColor = SafarColors.error
            )
        )
        
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = errorMessage ?: "",
                color = SafarColors.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// ── Search Bar ────────────────────────────────────────────────────────────────
@Composable
fun SafarSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SafarDimens.radiusLg),
        color = SafarColors.surfaceElevated,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = SafarColors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = SafarColors.textTertiary,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    textStyle = LocalTextStyle.current.copy(
                        color = SafarColors.textPrimary,
                        fontSize = 15.sp
                    ),
                    singleLine = true
                )
            }
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = SafarColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Chip Component ────────────────────────────────────────────────────────────
@Composable
fun SafarChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (selected) {
        SafarColors.primary.copy(alpha = 0.15f)
    } else {
        SafarColors.surfaceElevated
    }
    val borderColor = if (selected) {
        SafarColors.primary.copy(alpha = 0.4f)
    } else {
        SafarColors.divider
    }
    val textColor = if (selected) {
        SafarColors.primary
    } else {
        SafarColors.textPrimary
    }

    Surface(
        modifier = modifier
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(SafarDimens.radiusFull),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Avatar Component ──────────────────────────────────────────────────────────
@Composable
fun SafarAvatar(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showBadge: Boolean = false,
    badgeColor: Color = SafarColors.success
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = SafarColors.primary.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // If no image, show initials
                Text(
                    text = name.take(2).uppercase(),
                    color = SafarColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value / 2.5).sp
                )
            }
        }
        
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 4)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(2.dp, SafarColors.surface, CircleShape)
            )
        }
    }
}

// ── Rating Stars ──────────────────────────────────────────────────────────────
@Composable
fun SafarRatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 16.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(maxStars) { index ->
            val starFill = when {
                index < rating.toInt() -> 1f
                index == rating.toInt() -> rating - rating.toInt()
                else -> 0f
            }
            
            Icon(
                imageVector = if (starFill >= 0.5f) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = SafarColors.primary,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

// ── Bottom Sheet Handle ───────────────────────────────────────────────────────
@Composable
fun SafarBottomSheetHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SafarColors.divider)
    )
}

// ── Divider ───────────────────────────────────────────────────────────────────
@Composable
fun SafarDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    if (vertical) {
        VerticalDivider(
            modifier = modifier,
            color = SafarColors.divider
        )
    } else {
        HorizontalDivider(
            modifier = modifier,
            color = SafarColors.divider
        )
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
fun SafarEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SafarColors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SafarColors.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SafarColors.textPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            fontSize = 14.sp,
            color = SafarColors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            SafarPrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

// ── Skeleton Loader ───────────────────────────────────────────────────────────
@Composable
fun SafarSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SafarDimens.radiusSm)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(SafarColors.textTertiary.copy(alpha = alpha))
    )
}

// ── Toast/Snackbar ────────────────────────────────────────────────────────────
@Composable
fun SafarSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    type: SnackbarType = SnackbarType.Default
) {
    val backgroundColor = when (type) {
        SnackbarType.Success -> SafarColors.success
        SnackbarType.Error -> SafarColors.error
        SnackbarType.Warning -> SafarColors.warning
        SnackbarType.Default -> SafarColors.surfaceElevated
    }
    
    val contentColor = when (type) {
        SnackbarType.Default -> SafarColors.textPrimary
        else -> Color.White
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SafarDimens.radiusMd),
        color = backgroundColor,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = contentColor,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = if (type == SnackbarType.Default) SafarColors.primary else contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

enum class SnackbarType {
    Default, Success, Error, Warning
}
