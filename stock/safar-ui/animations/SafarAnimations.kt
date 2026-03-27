package com.safar.app.views.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
// SAFAR - Premium Animation Components
// Polished micro-interactions and loading states
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Pulsing radar animation for searching states
 */
@Composable
fun RadarPulseAnimation(
    modifier: Modifier = Modifier,
    pulseColor: Color = SafarColors.primary,
    centerContent: @Composable () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    // Three concentric pulsing rings
    val rings = listOf(0, 400, 800)
    
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        rings.forEach { delay ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delay, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scale$delay"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha$delay"
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(pulseColor.copy(alpha = alpha))
            )
        }
        
        // Center content
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = SafarColors.surfaceElevated,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                centerContent()
            }
        }
    }
}

/**
 * Animated car moving along a path
 */
@Composable
fun CarDrivingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "carDriving")
    
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 250f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "carOffsetX"
    )
    
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "carBounce"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        // Road
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .background(SafarColors.divider)
        )
        
        // Dashed line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(15) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(SafarColors.primary.copy(alpha = 0.5f))
                )
            }
        }
        
        // Car
        Icon(
            imageVector = Icons.Rounded.DirectionsCar,
            contentDescription = null,
            tint = SafarColors.primary,
            modifier = Modifier
                .size(32.dp)
                .offset(x = offsetX.dp, y = (-bounce).dp)
                .align(Alignment.CenterStart)
        )
    }
}

/**
 * Success checkmark animation
 */
@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
        delay(2000)
        onComplete()
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )
    
    val checkScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, delayMillis = 200),
        label = "checkScale"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Surface(
            modifier = Modifier
                .size(100.dp)
                .scale(scale),
            shape = CircleShape,
            color = SafarColors.success.copy(alpha = 0.15f)
        ) {}
        
        // Inner circle with check
        Surface(
            modifier = Modifier
                .size(70.dp)
                .scale(scale),
            shape = CircleShape,
            color = SafarColors.success
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(checkScale)
                )
            }
        }
    }
}

/**
 * Loading dots animation
 */
@Composable
fun LoadingDotsAnimation(
    modifier: Modifier = Modifier,
    dotColor: Color = SafarColors.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingDots")
    
    val dots = listOf(0, 100, 200)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { delay ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale$delay"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotAlpha$delay"
            )
            
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha))
            )
        }
    }
}

/**
 * Shimmer loading effect
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    widthOfShadowBrush: Int = 500,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1000
) {
    val shimmerColors = listOf(
        SafarColors.surfaceElevated.copy(alpha = 0.3f),
        SafarColors.surfaceElevated.copy(alpha = 0.5f),
        SafarColors.surfaceElevated.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = (durationMillis + widthOfShadowBrush).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - widthOfShadowBrush, 0f),
        end = Offset(translateAnimation, 0f)
    )

    Box(
        modifier = modifier.background(brush)
    )
}

/**
 * Price counter animation
 */
@Composable
fun AnimatedPriceCounter(
    targetPrice: Float,
    currency: String = "₹",
    modifier: Modifier = Modifier
) {
    var currentPrice by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(targetPrice) {
        val startPrice = currentPrice
        val steps = 30
        val stepDelay = 20L
        
        for (i in 1..steps) {
            delay(stepDelay)
            currentPrice = startPrice + (targetPrice - startPrice) * (i.toFloat() / steps)
        }
        currentPrice = targetPrice
    }
    
    Text(
        text = "$currency${String.format("%.2f", currentPrice)}",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = SafarColors.primary,
        modifier = modifier
    )
}

/**
 * ETA countdown animation
 */
@Composable
fun ETACountdown(
    minutes: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eta")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "etaPulse"
    )

    Surface(
        modifier = modifier.scale(pulseScale),
        shape = RoundedCornerShape(12.dp),
        color = SafarColors.primary.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = SafarColors.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$minutes min",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SafarColors.primary
            )
        }
    }
}

/**
 * Route progress indicator
 */
@Composable
fun RouteProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "routeProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(SafarColors.divider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SafarColors.success,
                            SafarColors.primary
                        )
                    )
                )
        )
        
        // Current position marker
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (animatedProgress * 200).dp) // Adjust based on width
                .size(12.dp)
                .clip(CircleShape)
                .background(SafarColors.primary)
        )
    }
}

/**
 * Ride completion celebration animation
 */
@Composable
fun CelebrationAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    
    // Multiple floating particles
    val particles = remember {
        List(12) {
            Triple(
                (Math.random() * 360).toFloat(), // angle
                (Math.random() * 50 + 50).toFloat(), // distance
                (Math.random() * 1000).toInt() // delay
            )
        }
    }
    
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        particles.forEach { (angle, distance, delay) ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -distance,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = delay),
                    repeatMode = RepeatMode.Restart
                ),
                label = "particleY$delay"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = delay),
                    repeatMode = RepeatMode.Restart
                ),
                label = "particleAlpha$delay"
            )
            
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delay),
                    repeatMode = RepeatMode.Restart
                ),
                label = "particleRotation$delay"
            )
            
            val offsetX = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * 30
            
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = SafarColors.primary.copy(alpha = alpha),
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .rotate(rotation)
            )
        }
        
        // Center trophy
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = SafarColors.primary.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Completed",
                    tint = SafarColors.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * Typing indicator animation (for chat)
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SafarColors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(0, 150, 300).forEach { delay ->
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, delayMillis = delay),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "typingDot$delay"
                )
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(SafarColors.textTertiary)
                )
            }
        }
    }
}
