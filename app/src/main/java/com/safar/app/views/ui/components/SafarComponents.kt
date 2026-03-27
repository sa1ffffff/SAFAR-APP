package com.safar.app.views.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.safar.app.views.ui.theme.*

/**
 * SafarComponents - Enterprise-grade reusable UI components
 */

@Composable
fun SafarPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SafarTheme.colors.onBackground,
            contentColor = SafarTheme.colors.background,
            disabledContainerColor = SafarTheme.colors.surface,
            disabledContentColor = SafarTheme.colors.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = SafarTheme.colors.background,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = text,
                    style = SafarTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SafarSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SafarTheme.colors.onBackground
        ),
        border = BorderStroke(1.dp, SafarTheme.colors.border)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = SafarTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SafarIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    backgroundColor: Color = SafarTheme.colors.surface,
    iconColor: Color = SafarTheme.colors.onSurface,
    size: Dp = 48.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
fun SafarCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SafarTheme.colors.surfaceElevated,
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = SafarTheme.typography.headlineSmall,
                color = SafarTheme.colors.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            onNavigateBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = SafarTheme.colors.onBackground
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SafarTheme.colors.background
        )
    )
}

@Composable
fun SafarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = SafarTheme.typography.labelMedium,
                color = SafarTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = SafarTheme.typography.bodyLarge,
                        color = SafarTheme.colors.textTertiary
                    )
                }
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = SafarTheme.colors.textSecondary
                    )
                }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SafarTheme.colors.surface,
                unfocusedContainerColor = SafarTheme.colors.surface,
                focusedBorderColor = SafarTheme.colors.primary,
                unfocusedBorderColor = SafarTheme.colors.border,
                errorBorderColor = SafarTheme.colors.error,
                cursorColor = SafarTheme.colors.primary,
                focusedTextColor = SafarTheme.colors.onSurface,
                unfocusedTextColor = SafarTheme.colors.onSurface
            )
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = SafarTheme.typography.bodySmall,
                color = SafarTheme.colors.error,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun SafarLoadingScreen(
    message: String = "Loading..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SafarTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = SafarTheme.colors.primary,
                strokeWidth = 3.dp
            )
            Text(
                text = message,
                style = SafarTheme.typography.bodyMedium,
                color = SafarTheme.colors.textSecondary
            )
        }
    }
}

@Composable
fun SafarEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    SafarTheme.colors.surface,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SafarTheme.colors.textSecondary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Text(
            text = title,
            style = SafarTheme.typography.headlineSmall,
            color = SafarTheme.colors.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = description,
            style = SafarTheme.typography.bodyMedium,
            color = SafarTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SafarPrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
fun SafarStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = SafarTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SafarDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = SafarTheme.colors.divider
    )
}

@Composable
fun SafarAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 48.dp,
    fontSize: TextUnit = 16.sp,
    onClick: (() -> Unit)? = null,
    showBorder: Boolean = false,
    borderColor: Color = UberGreen
) {
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
    
    val avatarModifier = if (showBorder) {
        modifier
            .size(size)
            .border(2.dp, borderColor, CircleShape)
    } else {
        modifier.size(size)
    }
    
    Surface(
        modifier = avatarModifier,
        shape = CircleShape,
        color = if (imageUrl.isNullOrBlank()) SafarGreen.copy(alpha = 0.15f) else Color.Transparent,
        onClick = onClick ?: {}
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                // Load actual profile picture
                var isLoading by remember { mutableStateOf(true) }
                var isError by remember { mutableStateOf(false) }
                
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "$name profile picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        isLoading = state is AsyncImagePainter.State.Loading
                        isError = state is AsyncImagePainter.State.Error
                    }
                )
                
                // Show initials as fallback if image fails to load
                if (isError) {
                    InitialsContent(initials, fontSize)
                }
                
                // Show loading indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size / 3),
                        color = UberGreen,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                // Show initials when no image URL
                InitialsContent(initials, fontSize)
            }
        }
    }
}

@Composable
private fun InitialsContent(initials: String, fontSize: TextUnit) {
    Text(
        text = initials.ifEmpty { "?" },
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = UberGreen
    )
}

/**
 * Compact profile avatar for use in headers, lists, etc.
 * Shows profile picture with optional online indicator
 */
@Composable
fun ProfileAvatarWithIndicator(
    name: String,
    imageUrl: String? = null,
    size: Dp = 40.dp,
    showOnlineIndicator: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.size(size + 4.dp)
    ) {
        SafarAvatar(
            name = name,
            imageUrl = imageUrl,
            size = size,
            fontSize = (size.value / 3).sp,
            onClick = onClick
        )
        
        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .background(UberGreen, CircleShape)
                    .border(2.dp, UberBlack, CircleShape)
            )
        }
    }
}
