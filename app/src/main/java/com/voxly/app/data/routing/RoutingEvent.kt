package com.voxly.app.data.routing

import com.voxly.app.data.model.User

sealed class RoutingEvent {
    object Started : RoutingEvent()
    data class Searching(val attempt: Int, val totalCandidates: Int) : RoutingEvent()
    data class Calling(val speaker: User) : RoutingEvent()
    data class Timeout(val speaker: User) : RoutingEvent()
    data class Switching(val from: User) : RoutingEvent()
    data class Connected(val speaker: User) : RoutingEvent()
    object Cancelled : RoutingEvent()
    object Completed : RoutingEvent()
    data class Error(val failure: RoutingFailure) : RoutingEvent()
}
