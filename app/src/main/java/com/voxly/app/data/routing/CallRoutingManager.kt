package com.voxly.app.data.routing

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.voxly.app.data.model.CallType

interface CallRoutingManager {
    val currentSession: StateFlow<RoutingSession?>
    val routingEvents: SharedFlow<RoutingEvent>
    fun startRouting(targetSpeakerId: String?, type: CallType)
    fun cancelRouting()
}
