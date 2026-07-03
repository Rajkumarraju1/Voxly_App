package com.voxly.app.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.voxly.app.ui.viewmodel.CallState
import com.voxly.app.ui.viewmodel.HomeViewModel

@Composable
fun CallOverlay(
    viewModel: HomeViewModel,
    onNavigateToIncoming: (CallState.Incoming) -> Unit = {}
) {
    val callState by viewModel.callState.collectAsState()
    
    // Check if we need to show the overlay (Any state other than Idle)
    if (callState is CallState.Idle) return

    Surface(
        modifier = Modifier.fillMaxSize().zIndex(100f),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = callState) {
            is CallState.Finding -> {
                val user by viewModel.currentUser.collectAsState()
                FindingScreen(
                    userGender = user?.gender ?: "",
                    onCancel = { viewModel.cancelFinding() }
                )
            }
            is CallState.Incoming -> {
                 // Launch Activity for incoming calls (screen wake etc)
                 // We can either handle it here or let the parent handle the navigation
                 // For consistency with previous logic, we might just show a placeholder 
                 // while the Activity starts.
                 LaunchedEffect(state) {
                     onNavigateToIncoming(state)
                 }
                 Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
            // Outgoing Call Screen
            is CallState.Outgoing -> {
                OutgoingCallScreen(
                    speakerName = state.speakerName,
                    speakerAvatar = state.speakerAvatar,
                    onEndCall = { viewModel.endCall() }
                )
            }
            // Active Call Screen
            is CallState.Active -> {
                val user by viewModel.currentUser.collectAsState()
                val amISpeaker = user?.isSpeaker == true
                
                // Calculate Other User ID for Agora
                val otherUserId = if (amISpeaker) {
                    state.callerId.hashCode()
                } else {
                    state.speakerId?.hashCode() ?: 0
                }
                
                val myId = viewModel.currentUserId ?: user?.id ?: ""
                val isRemoteUserSpeaking by viewModel.isRemoteUserSpeaking.collectAsState()

                // Determine name to display
                val displayName = if (amISpeaker) state.callerName else state.speakerName

                ActiveCallScreen(
                    callerName = displayName,
                    otherAvatar = state.otherAvatar,
                    ratePerMinute = state.rate,
                    isSpeaker = amISpeaker,
                    isVideoCall = state.type == "video",
                    otherUserId = otherUserId,
                    myUserId = myId,
                    liveCoins = viewModel.liveEarnings.collectAsState().value,
                    remainingSeconds = viewModel.remainingSeconds.collectAsState().value,
                    videoRequestStatus = state.videoRequestStatus,
                    videoRequestBy = state.videoRequestBy,
                    onRequestVideo = { viewModel.requestVideoSwitch() },
                    onAcceptVideo = { viewModel.respondToVideoSwitch(true) },
                    onDeclineVideo = { viewModel.respondToVideoSwitch(false) },
                    onSetupLocalVideo = { view -> viewModel.getAgoraManager().setupLocalVideo(view) },
                    onSetupRemoteVideo = { uid, view -> viewModel.getAgoraManager().setupRemoteVideo(uid, view) },
                    onToggleSpeaker = { on -> viewModel.toggleSpeaker(on) },
                    onToggleMute = { isMuted -> viewModel.getAgoraManager().toggleMute(isMuted) }, // Wired up
                    onToggleVideo = { on -> viewModel.toggleVideo(on) },
                    onEndCall = { viewModel.endCall() },
                    isRemoteUserSpeaking = isRemoteUserSpeaking,
                    onReportUser = { reason, desc ->
                        val targetId = if (amISpeaker) state.callerId else state.speakerId
                        if (targetId.isNotEmpty()) {
                            viewModel.reportUser(targetId, reason, desc)
                        }
                    },
                    onBlockUser = {
                        val targetId = if (amISpeaker) state.callerId else state.speakerId
                        if (targetId.isNotEmpty()) {
                            viewModel.blockUser(targetId)
                        }
                    }
                )
            }
            // Summary Screen
            is CallState.Summary -> {
                val user by viewModel.currentUser.collectAsState()
                val amISpeaker = user?.isSpeaker == true
                SessionSummaryScreen(
                    duration = state.duration,
                    earnings = state.earnings,
                    isSpeaker = amISpeaker,
                    onRate = { rating ->
                        state.speakerId?.let { speakerId ->
                            viewModel.submitRating(speakerId, rating)
                        }
                    },
                    onDone = { viewModel.closeSummary() }
                )
            }
            else -> {}
        }
    }
}
