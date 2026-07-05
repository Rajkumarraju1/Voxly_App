package com.rkdevstudios.voxly.ui.speaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.ui.call.ActiveCallScreen
import com.rkdevstudios.voxly.ui.call.IncomingCallScreen
import com.rkdevstudios.voxly.ui.call.SessionSummaryScreen
import com.rkdevstudios.voxly.ui.components.ProfileCard
import com.rkdevstudios.voxly.ui.viewmodel.CallState
import com.rkdevstudios.voxly.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerHomeScreen(
    user: User,
    viewModel: HomeViewModel, 
    onToggleOnline: (Boolean) -> Unit,
    onEarningsClick: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Permission launcher for both Going Online and Accepting Calls
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Retry handling toggle online if that was the intent.
            // Since we don't track the *source* of the request here easily, 
            // we can check if the user is currently offline, and if so, try to go online.
            if (!user.isOnline) {
                 // Try to go online automatically after permission grant
                 // We need to call the logic from handleToggleOnline, but we can't easily access it if it's defined below.
                 // Ideally users will just click again, but let's add a Toast.
                 android.widget.Toast.makeText(context, "Permissions granted. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Wrapper for Toggle Online
    val handleToggleOnline: (Boolean) -> Unit = { isChecked ->
         if (isChecked) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // Start Foreground Service
                 val serviceIntent = android.content.Intent(context, com.rkdevstudios.voxly.service.CallForegroundService::class.java)
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                     context.startForegroundService(serviceIntent)
                 } else {
                     context.startService(serviceIntent)
                 }
                onToggleOnline(true)
            } else {
                permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
            }
        } else {
            // Stop Service
            val serviceIntent = android.content.Intent(context, com.rkdevstudios.voxly.service.CallForegroundService::class.java)
            context.stopService(serviceIntent)
            onToggleOnline(false)
        }
    }

    // Start listening for calls and RESTART SERVICE when screen is active and user is online
    LaunchedEffect(user.isOnline) {
        if (user.isOnline) {
            viewModel.startListeningForCalls()
            
            // Ensure Heartbeat Service is running (in case app was restarted)
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                 val serviceIntent = android.content.Intent(context, com.rkdevstudios.voxly.service.CallForegroundService::class.java)
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                     context.startForegroundService(serviceIntent)
                 } else {
                     context.startService(serviceIntent)
                 }
            }
        }
    }

    // Main Content
    Scaffold(
        topBar = {
            // Only show TopBar if NOT in a full screen call
            if (callState is CallState.Idle) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Speaker Mode", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = onEarningsClick, // Navigate to Earnings
                            label = { 
                                val formattedEarnings = String.format(java.util.Locale.getDefault(), "₹%.2f", user.earnings)
                                Text(formattedEarnings) 
                            },
                            leadingIcon = { Text("💰") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = com.rkdevstudios.voxly.ui.theme.PremiumGold,
                                leadingIconContentColor = com.rkdevstudios.voxly.ui.theme.PremiumGold
                            )
                        )
                    }
                )
            }
        }
        // NO INTERNAL BOTTOM BAR
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Dashboard Content
            SpeakerDashboardContent(
                user = user, 
                onToggleOnline = handleToggleOnline // Pass the wrapped handler
            )

            // Call Overlays (Z-Index higher by placement)
            when (val state = callState) {
                is CallState.Incoming -> {
                    IncomingCallScreen(
                        callerName = state.callerName,
                        callerAvatarUrl = state.callerAvatar,
                        rate = state.rate,
                        onAccept = {
                             val permissions = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
                             if (state.type == "video") {
                                 permissions.add(android.Manifest.permission.CAMERA)
                             }
                             
                             val missingPermissions = permissions.filter { 
                                 androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED 
                             }

                             if (missingPermissions.isEmpty()) {
                                coroutineScope.launch {
                                    viewModel.acceptCall()
                                }
                            } else {
                                permissionLauncher.launch(missingPermissions.toTypedArray())
                            }
                        },
                        onDecline = { viewModel.declineCall() }
                    )
                }
                is CallState.Active -> {
                    // Calculate Other User ID (Caller)
                    val otherUserId = state.callerId.hashCode()
                    
                    ActiveCallScreen(
                        callerName = state.callerName,
                        otherAvatar = state.otherAvatar,
                        ratePerMinute = state.rate,
                        isSpeaker = true,
                        isVideoCall = state.type == "video",
                        otherUserId = otherUserId,
                        onSetupLocalVideo = { view -> viewModel.getAgoraManager().setupLocalVideo(view) },
                        onSetupRemoteVideo = { uid, view -> viewModel.getAgoraManager().setupRemoteVideo(uid, view) },
                        onToggleVideo = { on -> viewModel.toggleVideo(on) },
                        onToggleMute = { isMuted -> viewModel.getAgoraManager().toggleMute(isMuted) }, // Wired up
                        onToggleSpeaker = { on -> viewModel.toggleSpeaker(on) },
                        onEndCall = { viewModel.endCall() }
                    )
                }
                is CallState.Summary -> {
                    SessionSummaryScreen(
                        duration = state.duration,
                        earnings = state.earnings,
                        isSpeaker = true,
                        onDone = { viewModel.closeSummary() },
                        onRate = { _ -> } // Speaker doesn't rate
                    )
                }
                else -> {} // Idle
            }
        }
    }
}

@Composable
fun SpeakerDashboardContent(
    user: User,
    onToggleOnline: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. ONLINE / OFFLINE TOGGLE (Main Feature)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (user.isOnline) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (user.isOnline) "🟢 YOU ARE ONLINE" else "🔴 YOU ARE OFFLINE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (user.isOnline) Color.Green else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Switch(
                    checked = user.isOnline,
                    onCheckedChange = onToggleOnline,
                    modifier = Modifier.scale(1.5f) // Make it bigger
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (user.isOnline) "Receiving calls..." else "Go online to receive calls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

    }
}

// Extension to scale modifier (missing in standard compose but useful here)
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)
