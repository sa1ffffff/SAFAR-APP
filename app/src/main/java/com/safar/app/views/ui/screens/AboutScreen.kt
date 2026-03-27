package com.safar.app.views.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.safar.app.views.ui.theme.*

/**
 * SAFAR ABOUT SCREEN
 * 
 * Professional about screen with app info and developer details.
 * Follows M3 design with Uber-inspired dark theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appVersion: String = "1.0.0",
    onBack: () -> Unit = {},
    onGitHubClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About SAFAR",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Logo/Icon
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = UberGreen.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = UberGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Name
            Text(
                text = "SAFAR",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
            
            // Tagline
            Text(
                text = "Share rides, save costs, travel together",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            // Version
            Text(
                text = "Version $appVersion",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // About Section
            AboutSection(
                title = "What is SAFAR?",
                content = "SAFAR is Pakistan's premier ride-sharing platform designed to connect travelers heading in the same direction. Whether you're commuting to work, traveling between cities, or heading to university, SAFAR helps you find companions for your journey.\n\n" +
                        "Our mission is to make travel more affordable, sustainable, and social by enabling Pakistanis to share rides and split costs."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features Section
            AboutSection(
                title = "Key Features",
                content = null
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureItem(icon = Icons.Rounded.Map, text = "Find rides near you with map view")
                    FeatureItem(icon = Icons.Rounded.Chat, text = "In-app messaging with riders")
                    FeatureItem(icon = Icons.Rounded.Shield, text = "Verified users for safe travel")
                    FeatureItem(icon = Icons.Rounded.AttachMoney, text = "Share costs and save money")
                    FeatureItem(icon = Icons.Rounded.Notifications, text = "Real-time notifications")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Developer Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEVELOPED BY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Developer Avatar
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = UberGreen.copy(alpha = 0.15f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SW",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = UberGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Saifullah Waseem",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    
                    Text(
                        text = "SEECS'29",
                        fontSize = 14.sp,
                        color = UberGreen,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Contact Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // GitHub Button
                        OutlinedButton(
                            onClick = onGitHubClick,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, UberLightGray),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = "GitHub",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GitHub")
                        }
                        
                        // Email Button
                        Button(
                            onClick = onEmailClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UberGreen,
                                contentColor = UberBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = "Email",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contact")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legal Section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                AboutActionItem(
                    icon = Icons.Rounded.Policy,
                    title = "Privacy Policy",
                    onClick = onPrivacyPolicyClick
                )
                
                AboutActionItem(
                    icon = Icons.Rounded.Description,
                    title = "Terms of Service",
                    onClick = onTermsClick
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer
            Text(
                text = "Made with ❤️ in Pakistan",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "© 2024 SAFAR. All rights reserved.",
                fontSize = 11.sp,
                color = TextMuted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: String?,
    customContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (content != null) {
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
            
            customContent?.invoke()
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = UberGreen
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun AboutActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = TextSecondary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextMuted
            )
        }
    }
}