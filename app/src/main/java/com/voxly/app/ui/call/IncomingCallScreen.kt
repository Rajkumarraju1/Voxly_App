package com.voxly.app.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxly.app.util.AvatarHelper
import java.util.Locale

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatarUrl: String,
    rate: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Determine Call Type based on Rate (Basic heuristic since type isn't passed directly, 
    // but usually 60 = Video, 10 or custom = Audio. 
    // Ideally 'type' should be passed, but based on rate we can infer context for user)
    val isVideo = rate >= com.voxly.app.util.CoinConstants.VIDEO_COINS_PER_MIN
    val callType = if (isVideo) "Incoming Video Call" else "Incoming Audio Call"
    
    // Calculate Earnings Prediction
    // Rate (coins/min) * Value (0.75) * Share (0.40)
    val earningsPerMin = rate * com.voxly.app.util.CoinConstants.COIN_VALUE_INR * com.voxly.app.util.CoinConstants.SPEAKER_SHARE
    val earningsStr = String.format(Locale.getDefault(), "You'll earn approx ₹%.2f/min", earningsPerMin)

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Background Gradient (Subtle)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A1A),
                                Color.Black
                            )
                        )
                    )
            )

            // Ripple Animation behind Avatar
            RippleEffect(modifier = Modifier.align(Alignment.Center).offset(y = (-50).dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Caller Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = callType.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    // Avatar Zone
                    val context = LocalContext.current
                    val avatarResId = remember(callerAvatarUrl) {
                        AvatarHelper.getDrawableId(context, callerAvatarUrl)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp)
                    ) {
                         Surface(
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize(),
                            color = com.voxly.app.ui.theme.PremiumSurface,
                            border = androidx.compose.foundation.BorderStroke(2.dp, com.voxly.app.ui.theme.PremiumGold)
                        ) {
                            if (avatarResId != 0) {
                                Image(
                                    painter = painterResource(id = avatarResId),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = callerName.firstOrNull()?.toString() ?: "?",
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = com.voxly.app.ui.theme.PremiumGold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = callerName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Earnings Display
                    Surface(
                        color = com.voxly.app.ui.theme.PremiumGold.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = earningsStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.voxly.app.ui.theme.PremiumGold,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 60.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ActionIcon(
                            icon = Icons.Default.CallEnd,
                            color = Color(0xFFEF5350), // Red
                            onClick = onDecline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", color = Color.White.copy(alpha = 0.8f))
                    }

                    // Accept Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Pulsing Call Button
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        ActionIcon(
                            icon = Icons.Default.Call,
                            color = Color(0xFF66BB6A), // Green
                            onClick = onAccept,
                            modifier = Modifier.scale(scale)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Accept", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Renamed to ActionIcon locally for clarity in this file
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .background(color, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .background(color, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}


@Composable
fun RippleEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Create 3 circles with staggered delays
    Box(modifier = modifier) {
        RippleCircle(infiniteTransition, 0)
        RippleCircle(infiniteTransition, 500)
        RippleCircle(infiniteTransition, 1000)
    }
}

@Composable
fun RippleCircle(
    transition: InfiniteTransition,
    delayMillis: Int
) {
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.size(100.dp).graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }) {
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}
