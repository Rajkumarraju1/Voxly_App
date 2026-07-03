package com.voxly.app.data.routing

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingLoggerImpl @Inject constructor() : RoutingLogger {
    override suspend fun logSessionStart(session: RoutingSession) {
        Log.i("RoutingLogger", "Session started: ID=${session.sessionId}, Policy=${session.policy}, Type=${session.requestedType}")
    }

    override suspend fun logAttemptStart(session: RoutingSession, attempt: RoutingAttempt) {
        Log.i("RoutingLogger", "Session ${session.sessionId}: Dialing Speaker=${attempt.speakerId}")
    }

    override suspend fun logAttemptEnd(session: RoutingSession, attempt: RoutingAttempt) {
        Log.i("RoutingLogger", "Session ${session.sessionId}: Attempt ended with Speaker=${attempt.speakerId}, Result=${attempt.result}, Duration=${attempt.endedAt - attempt.startedAt}ms")
    }

    override suspend fun logSessionEnd(session: RoutingSession, failure: RoutingFailure?) {
        if (failure != null) {
            Log.e("RoutingLogger", "Session failed: ID=${session.sessionId}, Reason=${failure.javaClass.simpleName}, TotalTime=${session.totalRoutingTime}ms")
        } else {
            Log.i("RoutingLogger", "Session completed: ID=${session.sessionId}, ConnectedSpeaker=${session.connectedSpeaker?.id}, TotalTime=${session.totalRoutingTime}ms")
        }
    }
}
