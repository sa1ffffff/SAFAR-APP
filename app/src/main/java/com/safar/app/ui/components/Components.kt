package com.safar.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar

/**
 * Press feedback: a quick scale to 0.97 on press, eased back on release.
 * Short and subtle so it reads as tactile, never as an animation to watch.
 */
@Composable
fun Modifier.pressable(
    interaction: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit,
    role: Role = Role.Button,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = tween(durationMillis = if (pressed) 90 else 160),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interaction, indication = ripple(), enabled = enabled, role = role, onClick = onClick)
}

enum class ButtonKind { Primary, Secondary, Ghost, Danger }

@Composable
fun SafarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val c = Safar.colors
    val (bg, fg, border) = when (kind) {
        ButtonKind.Primary -> Triple(c.primary, c.onPrimary, null)
        ButtonKind.Secondary -> Triple(c.surfaceMuted, c.content, null)
        ButtonKind.Ghost -> Triple(Color.Transparent, c.content, BorderStroke(1.dp, c.border))
        ButtonKind.Danger -> Triple(c.negativeMuted, c.negative, null)
    }
    val interaction = remember { MutableInteractionSource() }
    val active = enabled && !loading
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(Radii.md))
            .background(if (enabled) bg else c.surfaceMuted)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(Radii.md)) else Modifier)
            .pressable(interaction, enabled = active, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "button-loading",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (leading != null) Icon(leading, null, Modifier.size(20.dp), tint = if (enabled) fg else c.contentTertiary)
                    Text(text, style = Safar.type.bodyStrong, color = if (enabled) fg else c.contentTertiary, maxLines = 1)
                }
            }
        }
    }
}

/** 44dp round icon button. Pass [background] = Color.Transparent for a bare icon. */
@Composable
fun IconCircle(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Safar.colors.surfaceMuted,
    tint: Color = Safar.colors.content,
    size: Dp = 44.dp,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .pressable(interaction, enabled = enabled, onClick = onClick, pressedScale = 0.92f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = if (enabled) tint else Safar.colors.contentTertiary)
    }
}

@Composable
fun Avatar(name: String, photoUrl: String?, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Box(
        modifier.size(size).clip(CircleShape).background(c.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name),
            style = Safar.type.bodyStrong.copy(fontSize = (size.value * 0.36f).let { androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp) }),
            color = c.content,
        )
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        }
    }
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/** Pickup ring → line → drop-off square, the route motif used across the app. */
@Composable
fun RouteMarks(gap: Dp, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Column(modifier.width(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(11.dp).border(2.5.dp, c.content, CircleShape))
        Box(Modifier.width(1.5.dp).height(gap).background(c.border))
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c.content))
    }
}

@Composable
fun RouteStops(
    from: String, to: String,
    fromDetail: String? = null, toDetail: String? = null,
    modifier: Modifier = Modifier,
    style: TextStyle = Safar.type.bodyStrong,
) {
    val c = Safar.colors
    val hasDetail = fromDetail != null
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        RouteMarks(gap = if (hasDetail) 34.dp else 18.dp)
        Column(verticalArrangement = Arrangement.spacedBy(if (hasDetail) 14.dp else 10.dp)) {
            Column {
                Text(from, style = style, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (fromDetail != null) Text(fromDetail, style = Safar.type.label, color = c.contentSecondary)
            }
            Column {
                Text(to, style = style, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (toDetail != null) Text(toDetail, style = Safar.type.label, color = c.contentSecondary)
            }
        }
    }
}

@Composable
fun RatingLabel(average: Double, count: Long, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = c.content)
        Text(
            if (count == 0L) "New" else "%.1f".format(average),
            style = Safar.type.label, color = c.contentSecondary,
        )
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val c = Safar.colors
    val shape = RoundedCornerShape(Radii.lg)
    val base = modifier
        .clip(shape)
        .background(c.surface)
        .border(1.dp, c.border, shape)
    val interaction = remember { MutableInteractionSource() }
    Box(
        (if (onClick != null) base.pressable(interaction, onClick = onClick, pressedScale = 0.985f) else base)
            .padding(padding)
    ) { content() }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Safar.colors.surfaceMuted,
    color: Color = Safar.colors.content,
    icon: ImageVector? = null,
) {
    Row(
        modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(13.dp), tint = color)
        Text(text, style = Safar.type.caption, color = color, maxLines = 1)
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Safar.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) c.primary else c.surface)
            .border(1.dp, if (selected) c.primary else c.border, CircleShape)
            .pressable(interaction, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = Safar.type.label, color = if (selected) c.onPrimary else c.content)
    }
}

/** Segmented control on a muted track; the selected segment is a raised surface. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Safar.colors
    LiquidSelector(
        count = options.size,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier.clip(RoundedCornerShape(Radii.md)).background(c.surfaceMuted).padding(4.dp).height(38.dp),
        spacing = 4.dp,
        lens = LensStyle.raised(Radii.sm),
    ) { i, focus, _ ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(options[i], style = Safar.type.label, color = lerpColor(c.contentSecondary, c.content, focus), maxLines = 1)
        }
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, range: IntRange, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconCircle(Icons.Rounded.Remove, "Fewer", { onChange(value - 1) }, size = 36.dp, enabled = value > range.first)
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(80)) },
            label = "stepper",
        ) { Text("$it", style = Safar.type.title, color = c.content, modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        IconCircle(Icons.Rounded.Add, "More", { onChange(value + 1) }, size = 36.dp, enabled = value < range.last)
    }
}

/** Filled text field with a floating label above the value, matching the design's form rows. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    error: String? = null,
    textStyle: TextStyle = Safar.type.bodyStrong,
) {
    val c = Safar.colors
    val shape = RoundedCornerShape(Radii.md)
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clip(shape)
                .background(c.surfaceMuted)
                .border(1.dp, if (error != null) c.negative else Color.Transparent, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f)) {
                Text(label, style = Safar.type.caption, color = c.contentSecondary)
                Spacer(Modifier.height(2.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = textStyle.copy(color = c.content),
                    cursorBrush = SolidColor(c.content),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, style = textStyle, color = c.contentTertiary)
                            }
                            inner()
                        }
                    },
                )
            }
            trailing?.invoke()
        }
        if (error != null) {
            Text(error, style = Safar.type.caption, color = c.negative, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
    }
}

/** A tappable read-only row in the same visual language as [Field] (pickers, selectors). */
@Composable
fun FieldButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = Safar.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(Radii.md))
            .background(c.surfaceMuted)
            .pressable(interaction, onClick = onClick, pressedScale = 0.985f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(label, style = Safar.type.caption, color = c.contentSecondary)
            Spacer(Modifier.height(2.dp))
            Text(
                value, style = Safar.type.bodyStrong,
                color = if (placeholder) c.contentTertiary else c.content,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke(this)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Safar.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Safar.type.headline, color = c.content, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action, style = Safar.type.label, color = c.contentSecondary,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Safar.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = c.content)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = Safar.type.headline, color = c.content, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = Safar.type.body, color = c.contentSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            SafarButton(action, onAction, kind = ButtonKind.Secondary, height = 46.dp)
        }
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Safar.colors.border))
}

/** Shimmer-free skeleton block: a calm muted shape, no animation competing for attention. */
@Composable
fun Skeleton(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(Radii.sm)).background(Safar.colors.surfaceMuted))
}

@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    tint: Color = Safar.colors.content,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = Safar.colors
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = tint)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Safar.type.bodyStrong, color = tint)
            if (subtitle != null) Text(subtitle, style = Safar.type.label, color = c.contentSecondary)
        }
        trailing?.invoke()
    }
}
