package com.rkdevstudios.voxly.data.routing

sealed class RoutingFailure {
    object RoutingAlreadyActive : RoutingFailure()
    object NoCandidates : RoutingFailure()
    object Timeout : RoutingFailure()
    object Cancelled : RoutingFailure()
    object SpeakerBusy : RoutingFailure()
    object SpeakerOffline : RoutingFailure()
    object NetworkError : RoutingFailure()
    object FirestoreError : RoutingFailure()
    object PermissionDenied : RoutingFailure()
}
