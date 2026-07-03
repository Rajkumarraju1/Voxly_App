package com.voxly.app.data.agora

import android.content.Context
import com.voxly.app.BuildConfig
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
            android.util.Log.d("AgoraManager", "onJoinChannelSuccess: Connected to channel $channel with uid $uid")
            _connectionState.value = "Connected"
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            android.util.Log.d("AgoraManager", "onUserJoined: Remote user $uid joined")
            _remoteUserJoined.value = uid
            _connectionState.value = "User Joined"
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            android.util.Log.d("AgoraManager", "onUserOffline: Remote user $uid offline, reason: $reason")
            _remoteUserJoined.value = null
            _connectionState.value = "User Offline"
            _isRemoteUserSpeaking.value = false
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            android.util.Log.d("AgoraManager", "onLeaveChannel")
            _connectionState.value = "Disconnected"
            _remoteUserJoined.value = null
            _isRemoteUserSpeaking.value = false
        }
        
        override fun onError(err: Int) {
            android.util.Log.e("AgoraManager", "onError: $err")
             _connectionState.value = "Error: $err"
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            if (speakers != null) {
                for (speaker in speakers) {
                    // Check if it's a remote user (uid != 0) and volume is significant
                    if (speaker.uid != 0 && speaker.volume > 10) {
                        _isRemoteUserSpeaking.value = true
                        return
                    }
                }
            }
            // If no remote speaker found with volume > 10
            _isRemoteUserSpeaking.value = false
        }
    }

    fun initialize() {
        if (rtcEngine != null) {
             android.util.Log.d("AgoraManager", "initialize: RtcEngine already initialized.")
             return
        }
        try {
            val config = io.agora.rtc2.RtcEngineConfig()
            config.mContext = context
            config.mAppId = BuildConfig.AGORA_APP_ID
            config.mEventHandler = eventHandler
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            
            // Enable Modules
            android.util.Log.d("AgoraManager", "initialize: Calling enableAudio()")
            rtcEngine?.enableAudio()
            android.util.Log.d("AgoraManager", "initialize: Calling enableVideo()")
            rtcEngine?.enableVideo() 
            
            // Explicitly Enable Local Streams (Critical Fix)
            android.util.Log.d("AgoraManager", "initialize: Calling enableLocalAudio(true)")
            rtcEngine?.enableLocalAudio(true)
            android.util.Log.d("AgoraManager", "initialize: Calling enableLocalVideo(true)")
            rtcEngine?.enableLocalVideo(true)
            
            // Log confirmation
            android.util.Log.d("AgoraManager", "initialize: Audio and Video enabled, Client Role set to BROADCASTER")
            
            rtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_DEFAULT)
            rtcEngine?.setEnableSpeakerphone(true)
            
            // Enable Volume Indication: interval=200ms, smooth=3, report_vad=true
            rtcEngine?.enableAudioVolumeIndication(200, 3, true)
        } catch (e: Exception) {
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
             val tokenBuilder = com.voxly.app.data.agora.token.RtcTokenBuilder2()
             val timestamp = (System.currentTimeMillis() / 1000).toInt()
             val expirationTimeInSeconds = 86400 // 24 hours
             val privilegeExpiredTs = timestamp + expirationTimeInSeconds
             
             tokenBuilder.buildTokenWithUid(
                 appId,
                 certificate,
                 channelId,
                 uid,
                 com.voxly.app.data.agora.token.RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                 expirationTimeInSeconds,
                 expirationTimeInSeconds
             )
        } else {
             android.util.Log.d("AgoraManager", "joinChannel: Certificate empty, using null token.")
             null
        }

        val res = rtcEngine?.joinChannel(token, channelId, null, uid)
        android.util.Log.d("AgoraManager", "joinChannel: Result=$res, Token Used=${token?.take(10)}...")
        android.util.Log.d("AgoraManager", "Join result: $res")
        if (res != 0) {
            _connectionState.value = "Join Failed: $res"
            // Reset state on failure (though likely already false)
            isJoined = false
            currentChannelId = null
        } else {
             isJoined = true
             currentChannelId = channelId
             android.util.Log.d("AgoraManager", "joinChannel: Success. Calling startPreview()")
             rtcEngine?.startPreview()
        }
    }

    fun leaveChannel() {
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        isJoined = false
        currentChannelId = null
        _remoteUserJoined.value = null
    }
    
    fun toggleMute(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun toggleVideo(enabled: Boolean) {
        android.util.Log.d("AgoraManager", "toggleVideo: Calling muteLocalVideoStream(${!enabled})")
        rtcEngine?.muteLocalVideoStream(!enabled)
        if (enabled) {
            android.util.Log.d("AgoraManager", "toggleVideo: Calling startPreview()")
            rtcEngine?.startPreview()
        } else {
            // Optional: stop preview if we want to save battery, but usually we keep it for quick resume
            // rtcEngine?.stopPreview() 
        }
    }

    fun toggleSpeaker(speakerOn: Boolean) {
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
