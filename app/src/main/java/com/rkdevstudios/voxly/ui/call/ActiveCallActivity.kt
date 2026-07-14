package com.rkdevstudios.voxly.ui.call

import android.os.Bundle
import android.util.Log
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.rkdevstudios.voxly.data.agora.AgoraManager
import com.rkdevstudios.voxly.ui.theme.VoxlyTheme
import com.rkdevstudios.voxly.ui.viewmodel.CallViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActiveCallActivity : ComponentActivity() {

    @Inject
    lateinit var agoraManager: AgoraManager // Still inject for direct setup needed by ActiveCallScreen which takes callbacks with container

    private val callViewModel: CallViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
            
            if (audioGranted && cameraGranted) {
                Log.d("ActiveCallActivity", "Permissions granted via callback. Starting Call.")
                callViewModel.startCall()
            } else {
                Log.w("ActiveCallActivity", "Permissions denied. Cannot start call.")
                // Optionally show a dialog or finish
                android.widget.Toast.makeText(this, "Permissions required for call", android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ActiveCallActivity", "onCreate: Starting ActiveCallActivity with CallViewModel")
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "ActiveCallActivity",
            event = "onCreate",
            effect = "Activity created",
            callId = callId
        )

        // Check Permissions First
        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasAudio && hasCamera) {
             Log.d("ActiveCallActivity", "Permissions already granted. Starting Call.")
             callViewModel.startCall()
        } else {
             Log.d("ActiveCallActivity", "Requesting Permissions...")
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                 requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.CAMERA
                    )
                )
            }
        }

        setContent {
            VoxlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by callViewModel.uiState.collectAsStateWithLifecycle()
                    val billingSnapshot by callViewModel.billingSnapshot.collectAsStateWithLifecycle()
                    val remainingSeconds by callViewModel.remainingSeconds.collectAsStateWithLifecycle()
                    val isRemoteUserSpeaking by callViewModel.isRemoteUserSpeaking.collectAsStateWithLifecycle()
                    
                    when (val state = uiState) {
                        is CallViewModel.CallUiState.Loading -> {
                             // Show generic loader
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                        is CallViewModel.CallUiState.Active -> {
                            val isSpeaker = state.isSpeaker
                            
                            ActiveCallScreen(
                                callerName = state.callerName,
                                otherAvatar = state.otherAvatar,
                                ratePerMinute = state.rate,
                                isSpeaker = isSpeaker,
                                isVideoCall = state.type == "video",
                                otherUserId = state.otherUserId,
                                videoRequestStatus = state.videoRequestStatus,
                                videoRequestBy = state.videoRequestBy,
                                myUserId = state.currentUserId,
                                
                                onRequestVideo = { callViewModel.requestVideoSwitch() },
                                onAcceptVideo = { callViewModel.acceptVideoSwitch() },
                                onDeclineVideo = { callViewModel.declineVideoSwitch() },
                                
                                onSetupLocalVideo = { container -> callViewModel.getAgoraManagerInstance().setupLocalVideo(container) },
                                onSetupRemoteVideo = { uid, container -> callViewModel.getAgoraManagerInstance().setupRemoteVideo(uid, container) },
                                
                                liveCoins = billingSnapshot.callerCoinsSpent.toDouble(),
                                remainingSeconds = remainingSeconds,
                                elapsedSeconds = billingSnapshot.elapsedSeconds,
                                
                                onToggleMute = { isMuted -> callViewModel.toggleMic(isMuted) }, // Note: check parameter meaning. toggleMic(mute) matches.
                                onToggleSpeaker = { isSpeakerOn -> callViewModel.toggleSpeaker(isSpeakerOn) },
                                onToggleVideo = { isVideoOn -> callViewModel.toggleVideo(isVideoOn) },
                                onEndCall = { callViewModel.endCall() },
                                isRemoteUserSpeaking = isRemoteUserSpeaking
                            )
                        }
                        is CallViewModel.CallUiState.Ended -> {
                            SessionSummaryScreen(
                                duration = state.duration,
                                earnings = state.earnings,
                                isSpeaker = state.isSpeaker,
                                onRate = { rating -> callViewModel.submitRating(rating) },
                                onDone = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Lifecycle logs
    override fun onStart() {
        super.onStart()
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        com.rkdevstudios.voxly.util.CallBgAudit.log("ActiveCallActivity", "onStart", "Activity started", callId)
    }

    override fun onResume() {
        super.onResume()
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        com.rkdevstudios.voxly.util.CallBgAudit.log("ActiveCallActivity", "onResume", "Activity resumed", callId)
    }
    
    override fun onPause() {
        super.onPause()
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        com.rkdevstudios.voxly.util.CallBgAudit.log("ActiveCallActivity", "onPause", "Activity paused", callId)
    }

    override fun onStop() {
        super.onStop()
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        com.rkdevstudios.voxly.util.CallBgAudit.log("ActiveCallActivity", "onStop", "Activity stopped", callId)
    }

    override fun onDestroy() {
        super.onDestroy()
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        com.rkdevstudios.voxly.util.CallBgAudit.log("ActiveCallActivity", "onDestroy", "Activity destroyed", callId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val oldCallId = getIntent()?.getStringExtra("CALL_ID")
        val newCallId = intent.getStringExtra("CALL_ID")
        android.util.Log.d("ActiveCallActivity", "onNewIntent: oldCallId=$oldCallId | newCallId=$newCallId")
        if (newCallId != null && newCallId != oldCallId) {
            setIntent(intent)
            finish()
            startActivity(intent)
        }
    }
}
