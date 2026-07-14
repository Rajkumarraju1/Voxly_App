package com.rkdevstudios.voxly.data.agora

import android.content.Context
import com.rkdevstudios.voxly.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgoraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var rtcEngine: RtcEngine? = null

    private val _remoteUserJoined = MutableStateFlow<Int?>(null)
    val remoteUserJoined: StateFlow<Int?> = _remoteUserJoined.asStateFlow()

    private val _isRemoteUserSpeaking = MutableStateFlow(false)
    val isRemoteUserSpeaking: StateFlow<Boolean> = _isRemoteUserSpeaking.asStateFlow()

    private val _connectionState = MutableStateFlow<String>("Disconnected")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private var isJoined = false
    private var currentChannelId: String? = null



    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onJoinChannelSuccess",
                effect = "Connected to channel $channel with uid $uid",
                callId = currentChannelId
            )
            _connectionState.value = "Connected"
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onRejoinChannelSuccess",
                effect = "Reconnected to channel $channel with uid $uid",
                callId = currentChannelId
            )
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onUserJoined",
                effect = "Remote user $uid joined",
                callId = currentChannelId
            )
            _remoteUserJoined.value = uid
            _connectionState.value = "User Joined"
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onUserOffline",
                effect = "Remote user $uid offline, reason=$reason",
                callId = currentChannelId
            )
            _remoteUserJoined.value = null
            _connectionState.value = "User Offline"
            _isRemoteUserSpeaking.value = false
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onLeaveChannel",
                effect = "Channel left successfully",
                callId = currentChannelId
            )
            _connectionState.value = "Disconnected"
            _remoteUserJoined.value = null
            _isRemoteUserSpeaking.value = false
        }
        
        override fun onError(err: Int) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onError",
                effect = "Error code $err",
                callId = currentChannelId
            )
            _connectionState.value = "Error: $err"
        }

        override fun onLocalAudioStateChanged(state: Int, error: Int) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "onLocalAudioStateChanged",
                effect = "state=$state | error=$error",
                callId = currentChannelId
            )
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            if (speakers != null) {
                for (speaker in speakers) {
                    if (speaker.uid != 0 && speaker.volume > 10) {
                        _isRemoteUserSpeaking.value = true
                        return
                    }
                }
            }
            _isRemoteUserSpeaking.value = false
        }
    }

    fun initialize() {
        if (rtcEngine != null) {
             android.util.Log.d("AgoraManager", "initialize: RtcEngine already initialized.")
             return
        }
        com.rkdevstudios.voxly.util.CallBgAudit.log("AgoraManager", "initialize", "Creating RtcEngine instance")
        try {
            val config = io.agora.rtc2.RtcEngineConfig()
            config.mContext = context
            config.mAppId = BuildConfig.AGORA_APP_ID
            config.mEventHandler = eventHandler
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            
            // Enable Modules
            com.rkdevstudios.voxly.util.CallBgAudit.log("AgoraManager", "initialize", "enableAudio()", currentChannelId)
            rtcEngine?.enableAudio()
            com.rkdevstudios.voxly.util.CallBgAudit.log("AgoraManager", "initialize", "enableVideo()", currentChannelId)
            rtcEngine?.enableVideo() 
            
            // Explicitly Enable Local Streams (Critical Fix)
            com.rkdevstudios.voxly.util.CallBgAudit.log("AgoraManager", "initialize", "enableLocalAudio(true)", currentChannelId)
            rtcEngine?.enableLocalAudio(true)
            com.rkdevstudios.voxly.util.CallBgAudit.log("AgoraManager", "initialize", "enableLocalVideo(true)", currentChannelId)
            rtcEngine?.enableLocalVideo(true)
            
            rtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_DEFAULT)
            rtcEngine?.setEnableSpeakerphone(true)
            
            rtcEngine?.enableAudioVolumeIndication(200, 3, true)
        } catch (e: Exception) {
            com.rkdevstudios.voxly.util.CallBgAudit.log("AgoraManager", "initialize_exception", "${e.message}", currentChannelId)
            e.printStackTrace()
        }
    }

    fun joinChannel(channelId: String, uid: Int) {
        if (isJoined && currentChannelId == channelId) {
            android.util.Log.d("AgoraManager", "Already joined this channel ($channelId). Ignoring.")
            return
        }
        if (isJoined && currentChannelId == channelId) {
            android.util.Log.d("AgoraManager", "Already joined this channel ($channelId). Ignoring.")
            return
        }


        val appId = BuildConfig.AGORA_APP_ID
        val certificate = BuildConfig.AGORA_CERTIFICATE
        android.util.Log.d("AgoraManager", "Certificate value = '$certificate'")
        
        // Generate Token if certificate is present
        val token = if (certificate.isNotEmpty()) {
             android.util.Log.d("AgoraManager", "joinChannel: Generating token with Certificate.")
             val tokenBuilder = com.rkdevstudios.voxly.data.agora.token.RtcTokenBuilder2()
             val timestamp = (System.currentTimeMillis() / 1000).toInt()
             val expirationTimeInSeconds = 86400 // 24 hours
              val privilegeExpiredTs = timestamp + expirationTimeInSeconds
             
             tokenBuilder.buildTokenWithUid(
                 appId,
                 certificate,
                 channelId,
                 uid,
                 com.rkdevstudios.voxly.data.agora.token.RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                 expirationTimeInSeconds,
                 expirationTimeInSeconds
             )
        } else {
             android.util.Log.d("AgoraManager", "joinChannel: Certificate empty, using null token.")
             null
        }

        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "AgoraManager",
            event = "joinChannel",
            effect = "Calling joinChannel(channelId=$channelId, uid=$uid)",
            callId = channelId
        )
        val res = rtcEngine?.joinChannel(token, channelId, null, uid)
        if (res != 0) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "AgoraManager",
                event = "joinChannel_failed",
                effect = "Result code $res",
                callId = channelId
            )
            _connectionState.value = "Join Failed: $res"
            isJoined = false
            currentChannelId = null
        } else {
             isJoined = true
             currentChannelId = channelId
             com.rkdevstudios.voxly.util.CallBgAudit.log(
                 component = "AgoraManager",
                 event = "joinChannel_success_launching_preview",
                 effect = "Calling startPreview()",
                 callId = channelId
             )
             rtcEngine?.startPreview()
        }
    }

    fun leaveChannel() {
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "AgoraManager",
            event = "leaveChannel",
            effect = "Stopping preview and leaving channel",
            callId = currentChannelId
        )
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        isJoined = false
        currentChannelId = null
        _remoteUserJoined.value = null
    }
    
    fun toggleMute(muted: Boolean) {
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "AgoraManager",
            event = "toggleMute",
            effect = "Calling muteLocalAudioStream($muted)",
            callId = currentChannelId
        )
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun toggleVideo(enabled: Boolean) {
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "AgoraManager",
            event = "toggleVideo",
            effect = "Calling muteLocalVideoStream(${!enabled}) | startPreviewIfEnabled=$enabled",
            callId = currentChannelId
        )
        rtcEngine?.muteLocalVideoStream(!enabled)
        if (enabled) {
            rtcEngine?.startPreview()
        }
    }

    fun toggleSpeaker(speakerOn: Boolean) {
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "AgoraManager",
            event = "toggleSpeaker",
            effect = "Calling setEnableSpeakerphone($speakerOn)",
            callId = currentChannelId
        )
        rtcEngine?.setEnableSpeakerphone(speakerOn)
    }

    fun setupLocalVideo(container: android.widget.FrameLayout) {
        android.util.Log.d("AgoraManager", "setupLocalVideo: Called. container.childCount = ${container.childCount}")
        if (rtcEngine == null) {
            android.util.Log.e("AgoraManager", "setupLocalVideo: RtcEngine is null!")
            return
        }
        
        // STRICT FIX: If we already have a child, DO NOT recreate it.
        if (container.childCount > 0) {
            android.util.Log.d("AgoraManager", "setupLocalVideo: View already exists. Skipping.")
            return
        }
        
        android.util.Log.d("AgoraManager", "setupLocalVideo: Creating TextureView")
        // Create TextureView (better for Compose/Animations than SurfaceView)
        val surfaceView = android.view.TextureView(container.context)
        
        container.addView(surfaceView, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // Set up local video on the SDK
        rtcEngine?.setupLocalVideo(io.agora.rtc2.video.VideoCanvas(
            surfaceView, 
            io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN, 
            0
        ))
        
        // Start Preview is now handled in joinChannel or initialize logic
        // val result = rtcEngine?.startPreview()
        // android.util.Log.d("AgoraManager", "setupLocalVideo: startPreview result: $result")
    }

    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    fun setupRemoteVideo(uid: Int, container: android.widget.FrameLayout) {
        android.util.Log.d("AgoraManager", "setupRemoteVideo: Called for UID: $uid. container.childCount = ${container.childCount}")
        
        // STRICT FIX: If we already have a child, DO NOT recreate.
        if (container.childCount > 0) {
             android.util.Log.d("AgoraManager", "setupRemoteVideo: View already exists for uid: $uid. Skipping.")
             return
        }

        android.util.Log.d("AgoraManager", "setupRemoteVideo: Creating SurfaceView for UID: $uid")
        val surfaceView = android.view.SurfaceView(container.context)
        container.addView(surfaceView, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        rtcEngine?.setupRemoteVideo(io.agora.rtc2.video.VideoCanvas(surfaceView, io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    fun destroy() {
        RtcEngine.destroy()
        rtcEngine = null
    }
}
