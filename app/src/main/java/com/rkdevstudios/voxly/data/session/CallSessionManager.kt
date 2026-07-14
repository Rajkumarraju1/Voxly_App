package com.rkdevstudios.voxly.data.session

import android.widget.FrameLayout
import com.rkdevstudios.voxly.data.model.Call
import kotlinx.coroutines.flow.StateFlow

interface CallSessionManager {
    val sessionState: StateFlow<SessionState>
    val audioState: StateFlow<Boolean>
    val videoState: StateFlow<Boolean>
    val connectionState: StateFlow<String>
    val currentCall: StateFlow<Call?>
    
    fun takeOwnership(call: Call)
    fun leaveSession()
    fun attachLocalRenderer(container: FrameLayout)
    fun detachLocalRenderer()
    fun attachRemoteRenderer(uid: Int, container: FrameLayout)
    fun detachRemoteRenderer()
    fun restore()
    fun updateCall(call: Call)
}
