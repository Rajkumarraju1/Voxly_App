package com.voxly.app.data.routing

import com.voxly.app.data.model.User
import com.voxly.app.data.model.toSupportedCallType

interface CandidateFilter {
    fun filter(candidates: List<User>, session: RoutingSession): List<User>
}

class ExcludeCallerFilter(private val currentUserId: String) : CandidateFilter {
    override fun filter(candidates: List<User>, session: RoutingSession): List<User> {
        return candidates.filter { it.id != currentUserId }
    }
}

class AttemptFilter : CandidateFilter {
    override fun filter(candidates: List<User>, session: RoutingSession): List<User> {
        return candidates.filter { it.id !in session.attemptedSpeakerIds }
    }
}

class OnlineFilter(private val staleThresholdMs: Long = 120000L) : CandidateFilter {
    override fun filter(candidates: List<User>, session: RoutingSession): List<User> {
        val now = System.currentTimeMillis()
        return candidates.filter { it.isOnline && (now - it.lastSeen) < staleThresholdMs }
    }
}

class AvailabilityFilter : CandidateFilter {
    override fun filter(candidates: List<User>, session: RoutingSession): List<User> {
        return candidates.filter { it.activeCall == null }
    }
}

class LanguageFilter(private val callerLanguages: List<String>) : CandidateFilter {
    override fun filter(candidates: List<User>, session: RoutingSession): List<User> {
        if (callerLanguages.isEmpty()) return candidates
        return candidates.filter { speaker ->
            speaker.languages.any { callerLanguages.contains(it) }
        }
    }
}

class CallTypeFilter(private val requestedType: com.voxly.app.data.model.CallType) : CandidateFilter {
    override fun filter(candidates: List<User>, session: RoutingSession): List<User> {
        val mappedType = requestedType.toSupportedCallType()
        return candidates.filter { speaker ->
            speaker.callPreferences.getTypedSupportedCallTypes().contains(mappedType)
        }
    }
}
