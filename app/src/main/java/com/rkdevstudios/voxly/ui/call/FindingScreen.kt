package com.rkdevstudios.voxly.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rkdevstudios.voxly.util.AvatarHelper
import kotlinx.coroutines.delay

@Composable
fun FindingScreen(
    userGender: String,
    onCancel: () -> Unit
) {
    // Animation for Scanning Line
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scannerOffsetY by infiniteTransition.animateFloat(
        initialValue = -0.2f, // Start slightly above
        targetValue = 1.2f,   // End slightly below
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scannerOffset"
    )

    // Avatar Cycling Logic
    // Scan takes 1500ms. We change avatar exactly once per scan cycle.
    val avatars = remember(userGender) {
        val isMale = userGender.equals("Male", ignoreCase = true)
        val isFemale = userGender.equals("Female", ignoreCase = true)
        
        if (isMale) {
            // If User is Male, show Female Avatars
            AvatarHelper.getFemaleAvatars()
        } else if (isFemale) {
            // If User is Female, show Male Avatars
            AvatarHelper.getMaleAvatars()
        } else {
            // Fallback: Mix
            AvatarHelper.getMaleAvatars() + AvatarHelper.getFemaleAvatars() 
        }.shuffled() // Shuffle initially
    }
    
    var currentAvatarIndex by remember { mutableStateOf(0) }
    
    // Sound Logic - Optimized
    val toneGenerator = remember {
        try {
            // Lower volume to 70 for less jarring sound
            android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            // Play scanning sound (PIP is a softer, shorter blip than BEEP)
            toneGenerator?.startTone(android.media.ToneGenerator.TONE_SUP_PIP, 150)

            // Sync with scanner animation duration (1500ms)
            delay(1500) 
            currentAvatarIndex = (currentAvatarIndex + 1) % avatars.size
        }
    }
    
    val currentAvatarName = avatars[currentAvatarIndex]
    val context = LocalContext.current
    val avatarResId = remember(currentAvatarName) {
        AvatarHelper.getDrawableId(context, currentAvatarName)
    }

    Surface(
        color = com.rkdevstudios.voxly.ui.theme.PremiumBlack, // Deep Black Background
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Central Scanner Area
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.rkdevstudios.voxly.ui.theme.PremiumSurface), // Brushed Dark Metal
                contentAlignment = Alignment.Center
            ) {
                // Avatar
                if (avatarResId != 0) {
                     Image(
                        painter = painterResource(id = avatarResId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                     Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                     )
                }
                
                // Scanning Line Overlay
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val height = maxHeight
                    val linePosition = scannerOffsetY * height.value
                    
                    // Only draw if within bounds
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .offset(y = linePosition.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        com.rkdevstudios.voxly.ui.theme.PremiumGold, // Gold Scanner
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // Optional: Glow below the line
                    Box(
                         modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .offset(y = linePosition.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        com.rkdevstudios.voxly.ui.theme.PremiumGold.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Finding a match for you...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = com.rkdevstudios.voxly.ui.theme.PremiumSilver
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please wait while we find you someone who is available",
                style = MaterialTheme.typography.bodyMedium,
                color = com.rkdevstudios.voxly.ui.theme.PremiumSilver.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.rkdevstudios.voxly.ui.theme.RoyalBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Cancel", color = com.rkdevstudios.voxly.ui.theme.RoyalBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
