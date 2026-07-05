package com.rkdevstudios.voxly.data.routing

import com.rkdevstudios.voxly.data.model.CallType
import com.rkdevstudios.voxly.data.model.User

enum class RoutingPolicy {
    SPECIFIC_FIRST,
    PURE_RANDOM,
    PREFERRED_LANGUAGE,
    PREMIUM_FIRST
}

enum class AttemptResult {
    ACCEPTED,
    TIMEOUT,
    DECLINED,
    BUSY,
    OFFLINE,
    CANCELLED
}

data class RoutingAttempt(
    val speakerId: String,
    val startedAt: Long,
    val endedAt: Long = 0L,
    val result: AttemptResult
)

data class RoutingSession(
    val sessionId: String,
    val routingVersion: Int = 1,
    val policy: RoutingPolicy = RoutingPolicy.SPECIFIC_FIRST,
    val requestedType: CallType,
    val originalSpeakerId: String?,
    val currentSpeakerId: String?,
    val currentCallId: String?,
    val attemptedSpeakerIds: Set<String>,
    val remainingCandidates: List<User>,
    val startedAt: Long,
    val endedAt: Long = 0L,
    val totalRoutingTime: Long = 0L,
    val retryCount: Int,
    val connectedSpeaker: User? = null,
    val fallbackUsed: Boolean = false,
    val attempts: List<RoutingAttempt> = emptyList(),
    val state: RoutingState
)
