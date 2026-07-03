package com.voxly.app.data.routing

interface RoutingLogger {
    suspend fun logSessionStart(session: RoutingSession)
    suspend fun logAttemptStart(session: RoutingSession, attempt: RoutingAttempt)
    suspend fun logAttemptEnd(session: RoutingSession, attempt: RoutingAttempt)
    suspend fun logSessionEnd(session: RoutingSession, failure: RoutingFailure? = null)
}
