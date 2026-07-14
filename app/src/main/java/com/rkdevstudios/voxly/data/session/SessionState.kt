package com.rkdevstudios.voxly.data.session

sealed class SessionState {
    object Idle : SessionState()
    object Initializing : SessionState()
    object JoiningAgora : SessionState()
    object WaitingRemoteUser : SessionState()
    object Connected : SessionState()
    object Reconnecting : SessionState()
    object Ending : SessionState()
    object Ended : SessionState()
}
