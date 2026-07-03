package com.voxly.app.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import com.voxly.app.util.AvatarHelper

@Composable
fun OutgoingCallScreen(
    speakerName: String,
    speakerAvatar: String,
    onEndCall: () -> Unit
) {
    // Premium Background (Deep Black Gradient)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF121212), // Deep Black
                        Color(0xFF1E1E1E)  // Dark Gray
                    )
                )
            )
    ) {
        val context = LocalContext.current
        val avatarResId = remember(speakerAvatar) {
            AvatarHelper.getDrawableId(context, speakerAvatar)
        }

        // Pulsing Animation
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        
        val pulseAlpha by infiniteTransition.animateFloat(
             initialValue = 0.5f,
             targetValue = 0f,
             animationSpec = infiniteRepeatable(
                 animation = tween(1000, easing = LinearEasing),
                 repeatMode = RepeatMode.Restart
             ),
             label = "pulseAlpha"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 100.dp)
            ) {
                // Avatar with Ripple Effect
                Box(contentAlignment = Alignment.Center) {
                     // Ripple Circle
                     Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(140.dp * pulseScale)
                     ) {}
                     
                    // Avatar Image
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(140.dp),
                        color = com.voxly.app.ui.theme.PremiumSurface
                    ) {
                        if (avatarResId != 0) {
                            Image(
                                painter = painterResource(id = avatarResId),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = speakerName.firstOrNull()?.toString() ?: "?",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = com.voxly.app.ui.theme.PremiumGold
                                )
                             }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = speakerName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ringing...",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Large End Call Button
            Button(
                onClick = onEndCall,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .size(96.dp) // Significantly larger
                    .padding(bottom = 64.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
