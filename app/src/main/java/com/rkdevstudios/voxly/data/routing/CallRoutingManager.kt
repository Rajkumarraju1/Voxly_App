package com.rkdevstudios.voxly.data.routing

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.rkdevstudios.voxly.data.model.CallType

interface CallRoutingManager {
    val currentSession: StateFlow<RoutingSession?>
    val routingEvents: SharedFlow<RoutingEvent>
    fun startRouting(targetSpeakerId: String?, type: CallType)
    fun cancelRouting()
}
