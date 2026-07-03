package com.voxly.app.data.routing

import com.voxly.app.data.model.CallType
import com.voxly.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

interface CandidateSelector {
    fun selectCandidates(
        caller: User,
        allSpeakers: List<User>,
        requestedType: CallType,
        attemptedIds: Set<String>,
        session: RoutingSession
    ): List<User>
}

@Singleton
class CandidateSelectorImpl @Inject constructor() : CandidateSelector {
    override fun selectCandidates(
        caller: User,
        allSpeakers: List<User>,
        requestedType: CallType,
        attemptedIds: Set<String>,
        session: RoutingSession
    ): List<User> {
        val filters = listOf(
            ExcludeCallerFilter(caller.id),
            AttemptFilter(),
            OnlineFilter(),
            CallTypeFilter(requestedType),
            AvailabilityFilter(),
            LanguageFilter(caller.languages)
        )
        var filteredList = allSpeakers
        for (filter in filters) {
            filteredList = filter.filter(filteredList, session)
        }
        return filteredList.shuffled()
    }
}
