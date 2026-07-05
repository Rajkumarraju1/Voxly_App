package com.voxly.app.ui.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.filled.Close
import com.voxly.app.util.AvatarHelper
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ActiveCallScreen(
    callerName: String,
    otherAvatar: String,
    ratePerMinute: Int,
    isSpeaker: Boolean,
    isVideoCall: Boolean = false,

    otherUserId: Int = 0, // Needed for remote video setup
    videoRequestStatus: String? = null,
    videoRequestBy: String? = null,
    myUserId: String = "",
    onRequestVideo: () -> Unit = {},
    onAcceptVideo: () -> Unit = {},
    onDeclineVideo: () -> Unit = {},
    onSetupLocalVideo: (android.widget.FrameLayout) -> Unit = {},
    onSetupRemoteVideo: (Int, android.widget.FrameLayout) -> Unit = { _, _ -> },
    liveCoins: Double = 0.0,
    remainingSeconds: Long = -1,
    onToggleMute: (Boolean) -> Unit = {}, // Added callback
    onToggleSpeaker: (Boolean) -> Unit = {},
    onToggleVideo: (Boolean) -> Unit = {}, // Added callback
    onEndCall: () -> Unit,
    isRemoteUserSpeaking: Boolean = false,
    onReportUser: (String, String) -> Unit = { _, _ -> },
    onBlockUser: () -> Unit = {}
) {
    // Menu State
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    
    // Report State
    var reportReason by remember { mutableStateOf("Abusive Behavior") }
    var reportDescription by remember { mutableStateOf("") }
    
    // Camera State
    var isCameraEnabled by remember { mutableStateOf(true) } // Moved here for visibility

    // Timer State
    var seconds by remember { mutableStateOf(0L) }
    
    // Earnings State
    val earnings by remember(liveCoins) {
        derivedStateOf { 
            liveCoins * com.voxly.app.util.CoinConstants.currentConfig.speakerRate
        }
    }

    // Timer Loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            seconds++
        }
    }

    val formattedTime = remember(seconds) {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    // Avatar Helper
    val context = LocalContext.current
    val avatarResId = remember(otherAvatar) { AvatarHelper.getDrawableId(context, otherAvatar) }

    // Permission Launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cameraGranted = result[android.Manifest.permission.CAMERA] ?: false
        val audioGranted = result[android.Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) {
             android.util.Log.d("ActiveCallScreen", "Permissions granted via launcher.")
             // Might need to re-trigger video setup here if it failed initially, 
             // but AgoraManager usually handles it if we just call enableVideo again or if the view is recomposed.
             // For now, let's rely on the state update.
             isCameraEnabled = true
        } else {
             android.util.Log.e("ActiveCallScreen", "Permissions DENIED via launcher.")
             // Optionally show rationale or toast
        }
    }

    // Trigger Permission Request on Video Call Start
    LaunchedEffect(isVideoCall) {
        if (isVideoCall) {
            val cameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            val audioPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            
            val toRequest = mutableListOf<String>()
            if (cameraPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) toRequest.add(android.Manifest.permission.CAMERA)
            if (audioPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) toRequest.add(android.Manifest.permission.RECORD_AUDIO)
            
            if (toRequest.isNotEmpty()) {
                android.util.Log.d("ActiveCallScreen", "Requesting permissions: $toRequest")
                permissionLauncher.launch(toRequest.toTypedArray())
            } else {
                 android.util.Log.d("ActiveCallScreen", "All permissions already granted.")
            }
        }
    }



    // Glow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRemoteUserSpeaking) 0.6f else 0f,
        targetValue = if (isRemoteUserSpeaking) 0f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = if (isRemoteUserSpeaking) 1f else 1f,
        targetValue = if (isRemoteUserSpeaking) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background base
    ) {
        
        // --- VIDEO / AUDIO CONTENT LAYER ---
        if (isVideoCall) {
            // 1. Remote Video (Full Screen Background)
            // 1. Remote Video (Full Screen Background)
            // STRICT FIX: Create container ONCE. Never recreate.
            val context = LocalContext.current
            val remoteContainer = remember { android.widget.FrameLayout(context) }
            
            AndroidView(
                factory = { remoteContainer },
                modifier = Modifier.fillMaxSize()
            )

            // STRICT FIX: Setup ONLY when userId changes and container is empty/ready
            LaunchedEffect(otherUserId) {
                if (otherUserId != 0) {
                     // Check if already added to avoid redundant calls if logic elsewhere is loose
                     // But strictly speaking, onSetupRemoteVideo should handle the "if childCount > 0" check now.
                     // We just pass it here.
                     onSetupRemoteVideo(otherUserId, remoteContainer)
                }
            }

            // 2. Local Video (Floating Small Box - Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 16.dp) // Adjusted padding to clear bottom controls
                    .width(100.dp)
                    .height(150.dp)
                    // No compose clip/border here. Native View handles it.
            ) {
                 // The Video View (Always present to keep stream active if needed, or just to hold layout)
                 // Wrapped in CardView for hardware clipping of SurfaceView
                 // The Video View (Always present to keep stream active if needed, or just to hold layout)
                 // Wrapped in CardView for hardware clipping of SurfaceView
                 
                 // STRICT FIX: Native View Hierarchy created ONCE.
                 val localContext = LocalContext.current
                 val localCardView = remember {
                     androidx.cardview.widget.CardView(localContext).apply {
                        radius = 16f * localContext.resources.displayMetrics.density
                        cardElevation = 0f
                        setCardBackgroundColor(android.graphics.Color.BLACK)
                        preventCornerOverlap = false
                        
                        // Container for SurfaceView
                        val frameLayout = android.widget.FrameLayout(localContext)
                        addView(frameLayout, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                     }
                 }

                 AndroidView(
                    factory = { localCardView },
                    modifier = Modifier.fillMaxSize()
                )

                // STRICT FIX: Run setup ONCE.
                LaunchedEffect(Unit) {
                    // Extract the FrameLayout we added above
                    val container = localCardView.getChildAt(0) as android.widget.FrameLayout
                    onSetupLocalVideo(container)
                }

                 // Camera Off Overlay
                 if (!isCameraEnabled) {
                     Box(
                         modifier = Modifier
                             .fillMaxSize()
                             .background(Color.Black)
                             .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                             .clip(RoundedCornerShape(16.dp)), // Compose clip works on normal views
                         contentAlignment = Alignment.Center
                     ) {
                         Icon(
                             imageVector = Icons.Default.VideocamOff,
                             contentDescription = "Camera Off",
                             tint = Color.White.copy(alpha = 0.5f),
                             modifier = Modifier.size(32.dp)
                         )
                     }
                 } else {
                     // Add border overlay when camera ON (since native view handles content)
                     Box(
                         modifier = Modifier
                             .fillMaxSize()
                             .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                     )
                 }
            }
        } else {
             // 3. Audio UI (Gradient Background + Avatar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF121212),
                                Color(0xFF2C2C2C)
                            )
                        )
                    )
            )

             Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 // Duration
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                 // Avatar with Glow
                 Box(contentAlignment = Alignment.Center) {
                     // Blue Glow for Active Speaker
                     if (isRemoteUserSpeaking) {
                         Surface(
                            shape = CircleShape,
                            color = Color(0xFF007AFF).copy(alpha = glowAlpha),
                            modifier = Modifier.size(160.dp * glowScale)
                         ) {}
                     }
                     
                    // Actual Avatar
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(160.dp),
                        color = Color.DarkGray,
                        shadowElevation = 12.dp
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
                                    text = callerName.firstOrNull()?.toString() ?: "?",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                             }
                        }
                    }
                 }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = if (isSpeaker) "Connected • Speaker" else "Connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // --- TOP OVERLAYS ---
        
        // Timer (Video Mode)
        if (isVideoCall) {
             val h = seconds / 3600
             val m = (seconds % 3600) / 60
             val s = seconds % 60
             val timeStr = if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
                           else String.format(Locale.getDefault(), "%02d:%02d", m, s)
             
             Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
             ) {
                 Text(
                     text = timeStr,
                     color = Color.White,
                     style = MaterialTheme.typography.titleMedium,
                     fontWeight = FontWeight.SemiBold
                 )
             }
        }
        
        // Live Earnings (Speaker Only)
        if (isSpeaker) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam, // Placeholder icon or coin
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "₹%.2f", earnings),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD700), // Gold
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- VIDEO REQUEST DIALOG ---
        if (videoRequestStatus == "pending" && videoRequestBy != null && videoRequestBy != myUserId) {
            val shieldIcon = remember {
                ImageVector.Builder(
                    name = "Shield",
                    defaultWidth = 16.dp,
                    defaultHeight = 16.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).path(fill = SolidColor(Color(0xFFC084FC))) {
                    moveTo(12f, 2f)
                    lineTo(4f, 5f)
                    verticalLineTo(11f)
                    quadTo(4f, 16.5f, 12f, 22f)
                    quadTo(20f, 16.5f, 20f, 11f)
                    verticalLineTo(5f)
                    lineTo(12f, 2f)
                    close()
                }.build()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {}, // Block touches
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13131A)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFDE3B75).copy(alpha = 0.8f), Color(0xFF3B82F6).copy(alpha = 0.8f))
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Horizontal decoration + central video icon badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color(0xFF3B82F6).copy(alpha = 0.4f))
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF3B82F6), Color(0xFFDE3B75))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDE3B75))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFDE3B75).copy(alpha = 0.4f), Color.Transparent)
                                        )
                                    )
                            )
                        }

                        // Texts Section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Video Call Request",
                                fontSize = 22.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            val subtitleText = androidx.compose.ui.text.buildAnnotatedString {
                                pushStyle(
                                    androidx.compose.ui.text.SpanStyle(color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                                )
                                append(callerName)
                                pop()
                                append(" wants to switch to video.")
                            }
                            
                            Text(
                                text = subtitleText,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Info Shield Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E24))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = shieldIcon,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You can switch back to audio anytime.",
                                color = Color(0xFFB8B6D5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Accept / Decline CTAs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDeclineVideo,
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Decline",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Button(
                                onClick = onAcceptVideo,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Accept",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTTOM CONTROLS (Floating Pill) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF202020), // Dark Grey Pill
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .height(80.dp) // Fixed height for pill
                    .wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    if (isVideoCall) {
                        IconButton(
                            onClick = { 
                                isCameraEnabled = !isCameraEnabled
                                onToggleVideo(isCameraEnabled)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (isCameraEnabled) Color(0xFF333333) else Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Video",
                                tint = if (isCameraEnabled) Color.White else Color.Black
                            )
                        }
                    } else {
                        // Switch Video Request Button
                        val isRequesting = videoRequestStatus == "pending" && videoRequestBy == myUserId
                         IconButton(
                            onClick = { if (!isRequesting) onRequestVideo() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (isRequesting) Color.Gray else Color(0xFF333333), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isRequesting) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "Video",
                                tint = Color.White
                            )
                        }
                    }

                    // 2. Microphone Toggle
                    var isMuted by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { 
                            isMuted = !isMuted
                            onToggleMute(isMuted)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isMuted) Color.White else Color(0xFF333333), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White
                        )
                    }
                    
                    // 3. Speaker Toggle
                     var isSpeakerPhoneOn by remember { mutableStateOf(!isVideoCall) }
                    IconButton(
                        onClick = { 
                            isSpeakerPhoneOn = !isSpeakerPhoneOn
                            onToggleSpeaker(isSpeakerPhoneOn)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isSpeakerPhoneOn) Color.White else Color(0xFF333333), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSpeakerPhoneOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerPhoneOn) Color.Black else Color.White
                        )
                    }

                    // 4. End Call (Red Circle)
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(56.dp) // Slightly larger
                            .background(Color(0xFFFF3B30), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        // --- TOP RIGHT MENU ---
        Box(
             modifier = Modifier
                 .align(Alignment.TopEnd)
                 .padding(top = 48.dp, end = 16.dp)
        ) {
             IconButton(onClick = { showMenu = true }) {
                 Icon(
                     imageVector = Icons.Default.MoreVert,
                     contentDescription = "More Options",
                     tint = Color.White
                 )
             }
             
             DropdownMenu(
                 expanded = showMenu,
                 onDismissRequest = { showMenu = false }
             ) {
                 DropdownMenuItem(
                     text = { Text("Report User") },
                     onClick = { 
                         showMenu = false 
                         showReportDialog = true
                     },
                     leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                 )
                 DropdownMenuItem(
                     text = { Text("Block User") },
                     onClick = { 
                         showMenu = false
                         showBlockDialog = true
                     },
                     leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                 )
             }
        }
        
        // --- REPORT DIALOG ---
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("Report User") },
                text = {
                    Column {
                        Text("Why are you reporting this user?")
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("Abusive Behavior", "Spam", "Scam", "Inappropriate Content").forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reportReason = reason }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (reportReason == reason),
                                    onClick = { reportReason = reason }
                                )
                                Text(
                                    text = reason,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onReportUser(reportReason, "Reported from Call")
                            showReportDialog = false
                        }
                    ) {
                        Text("Report")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- BLOCK DIALOG ---
        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text("Block User") },
                text = { Text("Are you sure you want to block this user? They will not be able to call you again.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onBlockUser()
                            showBlockDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Block")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
