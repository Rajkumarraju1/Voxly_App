package com.rkdevstudios.voxly.data.routing

import com.rkdevstudios.voxly.data.model.Call
import com.rkdevstudios.voxly.data.model.CallType

sealed class RoutingState {
    object Idle : RoutingState()
    data class Finding(val type: CallType) : RoutingState()
    data class Ringing(val callId: String, val speakerName: String, val type: CallType) : RoutingState()
    data class Connected(val call: Call) : RoutingState()
    data class Failed(val reason: RoutingFailure) : RoutingState()
}
