package com.safar.app.views.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.views.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SAFAR NETFLIX-STYLE SPLASH SCREEN
 * Premium animated splash with Netflix-inspired entrance
 * 
 * Features:
 * - Netflix-style letter-by-letter reveal animation
 * - Powerful scale entrance with elastic bounce
 * - Dramatic fade-to-black conclusion
 * - Ultra-premium timing and easing
 */
@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit = {}
) {
    // Master animation state - controls the entire sequence
    val animationProgress = remember { Animatable(0f) }
    
    // Individual letter animations for Netflix-style reveal
    val letterS = remember { Animatable(0f) }
    val letterA1 = remember { Animatable(0f) }
    val letterF = remember { Animatable(0f) }
    val letterA2 = remember { Animatable(0f) }
    val letterR = remember { Animatable(0f) }
    
    // Master scale for dramatic entrance
    val masterScale = remember { Animatable(0.3f) }
    val masterAlpha = remember { Animatable(0f) }
    
    // Background fade
    val backgroundIntensity = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Dramatic entrance (0-800ms)
        launch {
            masterAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400, easing = EaseOutExpo)
            )
        }
        
        launch {
            masterScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessVeryLow
                )
            )
        }
        
        delay(300)
        
        // Phase 2: Netflix-style letter reveal (300-1100ms)
        // Each letter appears with increasing intensity
        launch {
            letterS.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        
        delay(100)
        
        launch {
            letterA1.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        
        delay(100)
        
        launch {
            letterF.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        
        delay(100)
        
        launch {
            letterA2.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        
        delay(100)
        
        launch {
            letterR.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        
        // Phase 3: Hold and breathe (1600ms)
        delay(1200)
        
        // Phase 4: Dramatic exit (fade to completion)
        backgroundIntensity.animateTo(
            targetValue = 0f,
            animationSpec = tween(400, easing = EaseInExpo)
        )
        
        delay(200)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Professional dark gradient - Uber-inspired
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BgDark,
                        BgElevated
                    )
                )
            )
            .alpha(backgroundIntensity.value),
        contentAlignment = Alignment.Center
    ) {
        // Netflix-style animated title
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .scale(masterScale.value)
                .alpha(masterAlpha.value)
        ) {
            // S - Professional Green
            Text(
                text = "S",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Green,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .scale(letterS.value)
                    .alpha(letterS.value)
            )
            
            // A - Pure White
            Text(
                text = "A",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .scale(letterA1.value)
                    .alpha(letterA1.value)
            )
            
            // F - Professional Green
            Text(
                text = "F",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Green,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .scale(letterF.value)
                    .alpha(letterF.value)
            )
            
            // A - Pure White
            Text(
                text = "A",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .scale(letterA2.value)
                    .alpha(letterA2.value)
            )
            
            // R - Professional Green
            Text(
                text = "R",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Green,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .scale(letterR.value)
                    .alpha(letterR.value)
            )
        }
    }
}
