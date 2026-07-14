package com.rkdevstudios.voxly.data.session

import android.widget.FrameLayout
import com.rkdevstudios.voxly.data.agora.AgoraManager
import com.rkdevstudios.voxly.data.model.Call
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class CallSessionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val agoraManager: AgoraManager
) : CallSessionManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _audioState = MutableStateFlow(true)
    override val audioState: StateFlow<Boolean> = _audioState.asStateFlow()

    private val _videoState = MutableStateFlow(false)
    override val videoState: StateFlow<Boolean> = _videoState.asStateFlow()

    private val _connectionState = MutableStateFlow("Disconnected")
    override val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _currentCall = MutableStateFlow<Call?>(null)
    override val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    init {
        scope.launch {
            agoraManager.connectionState.collect { connection ->
                _connectionState.value = connection
                val current = _sessionState.value
                if (connection == "Connected" && current is SessionState.JoiningAgora) {
                    _sessionState.value = SessionState.WaitingRemoteUser
                } else if (connection == "User Joined" && (current is SessionState.WaitingRemoteUser || current is SessionState.JoiningAgora)) {
                    _sessionState.value = SessionState.Connected
                } else if (connection == "User Offline" && current is SessionState.Connected) {
                    _sessionState.value = SessionState.WaitingRemoteUser
                } else if (connection.startsWith("Error:") && current != SessionState.Idle) {
                    _sessionState.value = SessionState.Reconnecting
                }
            }
        }
    }

    private fun startService() {
        val intent = Intent(context, com.rkdevstudios.voxly.service.CallForegroundService::class.java).apply {
            putExtra("CALL_ID", _currentCall.value?.id)
        }
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallSessionManager",
            event = "startService",
            effect = "Starting CallForegroundService dynamically",
            callId = _currentCall.value?.id
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService() {
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallSessionManager",
            event = "stopService",
            effect = "Stopping CallForegroundService",
            callId = _currentCall.value?.id
        )
        val intent = Intent(context, com.rkdevstudios.voxly.service.CallForegroundService::class.java)
        context.stopService(intent)
    }

    override fun takeOwnership(call: Call) {
        if (_sessionState.value != SessionState.Idle && _sessionState.value != SessionState.Ended) {
            return
        }
        _currentCall.value = call
        _sessionState.value = SessionState.Initializing
        
        agoraManager.initialize()
        _sessionState.value = SessionState.JoiningAgora
        
        startService()
        
        val channelName = "channel_${call.id}"
        val uid = (java.util.UUID.randomUUID().hashCode() and 0x7FFFFFFF)
        agoraManager.joinChannel(channelName, uid)
    }

    override fun leaveSession() {
        if (_sessionState.value == SessionState.Idle || _sessionState.value == SessionState.Ended) {
            return
        }
        _sessionState.value = SessionState.Ending
        agoraManager.leaveChannel()
        stopService()
        _sessionState.value = SessionState.Ended
    }

    override fun attachLocalRenderer(container: FrameLayout) {
        agoraManager.setupLocalVideo(container)
        _videoState.value = true
    }

    override fun detachLocalRenderer() {
        _videoState.value = false
    }

    override fun attachRemoteRenderer(uid: Int, container: FrameLayout) {
        agoraManager.setupRemoteVideo(uid, container)
    }

    override fun detachRemoteRenderer() {
        // No-op for skeleton
    }

    override fun restore() {
        // Defer recovery logic to Phase 5
    }

    override fun updateCall(call: Call) {
        _currentCall.value = call
    }
}
