package com.rkdevstudios.voxly.data.routing

import android.util.Log
import com.rkdevstudios.voxly.data.model.*
import com.rkdevstudios.voxly.data.repository.CallRepository
import com.rkdevstudios.voxly.data.repository.UserRepository
import com.rkdevstudios.voxly.data.repository.CallSnapshotResult
import com.rkdevstudios.voxly.data.repository.SnapshotSource
import com.rkdevstudios.voxly.util.CoinConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRoutingManagerImpl @Inject constructor(
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val candidateSelector: CandidateSelector,
    private val logger: RoutingLogger
) : CallRoutingManager {

    private val _currentSession = MutableStateFlow<RoutingSession?>(null)
    override val currentSession: StateFlow<RoutingSession?> = _currentSession.asStateFlow()

    private val _routingEvents = MutableSharedFlow<RoutingEvent>(replay = 0)
    override val routingEvents: SharedFlow<RoutingEvent> = _routingEvents.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var activeJob: Job? = null

    @Synchronized
    override fun startRouting(targetSpeakerId: String?, type: CallType) {
        if (_currentSession.value != null) {
            coroutineScope.launch {
                _routingEvents.emit(RoutingEvent.Error(RoutingFailure.RoutingAlreadyActive))
            }
            return
        }

        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            try {
                performRouting(targetSpeakerId, type)
            } catch (e: CancellationException) {
                // Handled in cancelRouting
            } catch (e: Exception) {
                Log.e("CallRoutingManager", "Error in call routing", e)
                failSession(RoutingFailure.NetworkError)
            }
        }
    }

    private suspend fun performRouting(targetSpeakerId: String?, type: CallType) {
        val caller = userRepository.currentUserFlow.value ?: run {
            failSession(RoutingFailure.PermissionDenied)
            return
        }

        val policy = if (targetSpeakerId != null) RoutingPolicy.SPECIFIC_FIRST else RoutingPolicy.PURE_RANDOM
        val sessionId = UUID.randomUUID().toString()

        val initialSession = RoutingSession(
            sessionId = sessionId,
            requestedType = type,
            policy = policy,
            originalSpeakerId = targetSpeakerId,
            currentSpeakerId = targetSpeakerId,
            currentCallId = null,
            attemptedSpeakerIds = emptySet(),
            remainingCandidates = emptyList(),
            startedAt = System.currentTimeMillis(),
            retryCount = 0,
            state = RoutingState.Finding(type)
        )
        _currentSession.value = initialSession
        logger.logSessionStart(initialSession)
        _routingEvents.emit(RoutingEvent.Started)

        // Load candidates
        var candidates = if (targetSpeakerId != null) {
            val target = userRepository.getUser(targetSpeakerId)
            if (target != null) listOf(target) else emptyList()
        } else {
            emptyList()
        }

        if (candidates.isEmpty() && targetSpeakerId != null) {
            failSession(RoutingFailure.SpeakerOffline)
            return
        }

        // If target is empty (e.g. Random call or fallback matching needed)
        if (targetSpeakerId == null) {
            val matchingLanguages = caller.languages
            val allSpeakers = userRepository.getSpeakers(matchingLanguages).first
            candidates = candidateSelector.selectCandidates(
                caller = caller,
                allSpeakers = allSpeakers,
                requestedType = type,
                attemptedIds = emptySet(),
                session = initialSession
            )
        }

        if (candidates.isEmpty()) {
            failSession(RoutingFailure.NoCandidates)
            return
        }

        // Set remaining candidates in session
        _currentSession.value = _currentSession.value?.copy(remainingCandidates = candidates)

        val maxAttempts = Math.min(RoutingConstants.MAX_ATTEMPTS, candidates.size)
        var attemptCount = 0

        while (attemptCount < maxAttempts && currentCoroutineContext().isActive) {
            val session = _currentSession.value ?: break
            val currentCandidate = candidates.getOrNull(attemptCount) ?: break
            attemptCount++

            _currentSession.value = _currentSession.value?.copy(
                currentSpeakerId = currentCandidate.id,
                retryCount = attemptCount
            )

            if (targetSpeakerId == null) {
                _routingEvents.emit(RoutingEvent.Searching(attemptCount, maxAttempts))
                delay(1000L) // UI search buffer
            }

            val attemptStart = System.currentTimeMillis()
            val attempt = RoutingAttempt(
                speakerId = currentCandidate.id,
                startedAt = attemptStart,
                result = AttemptResult.TIMEOUT
            )
            logger.logAttemptStart(_currentSession.value!!, attempt)

            _routingEvents.emit(RoutingEvent.Calling(currentCandidate))

            var callId: String? = null
            try {
                // Pre-generate call document ID
                callId = UUID.randomUUID().toString()
                Log.d("CallRoutingManager", "Generated call.id = $callId")
                val requiredRate = if (type == CallType.VIDEO) CoinConstants.VIDEO_COINS_PER_MIN else CoinConstants.AUDIO_COINS_PER_MIN
                
                val call = Call(
                    id = callId,
                    callerId = caller.id,
                    callerName = caller.displayName,
                    callerAvatar = caller.avatarUrl,
                    speakerId = currentCandidate.id,
                    speakerName = currentCandidate.displayName,
                    speakerAvatar = currentCandidate.avatarUrl,
                    type = type.name.lowercase(),
                    rate = requiredRate,
                    status = "ringing",
                    speakerFcmToken = currentCandidate.fcmToken,
                    version = 1
                )
                
                Log.d("CallRoutingManager", "Immediately before calling: callRepository.createCall(call) with ID $callId")
                // Write call inside atomic transaction
                callRepository.createCall(call)
                _currentSession.value = _currentSession.value?.copy(
                    currentCallId = callId,
                    attemptedSpeakerIds = session.attemptedSpeakerIds + currentCandidate.id,
                    state = RoutingState.Ringing(callId, currentCandidate.displayName, type)
                )

                // Listen to state changes
                val connected = monitorRingingCall(callId, currentCandidate, attempt)
                if (connected) {
                    return
                }
            } catch (e: Exception) {
                Log.e("CallRoutingManager", "Transaction failed for ${currentCandidate.id}", e)
                val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = AttemptResult.BUSY)
                val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                _currentSession.value = _currentSession.value?.copy(
                    attempts = currentAttempts + attemptEnd
                )
                logger.logAttemptEnd(_currentSession.value!!, attemptEnd)
                // Continue to next speaker
            }

            // If we didn't connect, notify switching and delay
            if (currentCoroutineContext().isActive && attemptCount < maxAttempts) {
                _routingEvents.emit(RoutingEvent.Switching(currentCandidate))
                delay(RoutingConstants.SWITCH_DELAY_MS)
            }
        }

        failSession(RoutingFailure.NoCandidates)
    }

    private suspend fun monitorRingingCall(callId: String, candidate: User, attempt: RoutingAttempt): Boolean = coroutineScope {
        var isConnected = false
        val isHandled = java.util.concurrent.atomic.AtomicBoolean(false)
        val timeoutJob = launch {
            delay(RoutingConstants.CALL_TIMEOUT_MS)
            if (currentCoroutineContext().isActive) {
                if (isHandled.compareAndSet(false, true)) {
                    Log.d("CallRoutingManager", "Ringing timeout for $callId")
                    cleanupCallDocument(callId, "timeout")
                    val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = AttemptResult.TIMEOUT)
                    val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                    _currentSession.value = _currentSession.value?.copy(
                        attempts = currentAttempts + attemptEnd
                    )
                    logger.logAttemptEnd(_currentSession.value!!, attemptEnd)
                    _routingEvents.emit(RoutingEvent.Timeout(candidate))
                    this@coroutineScope.cancel()
                }
            }
        }

        val presenceJob = launch {
            userRepository.getUserFlow(candidate.id).collect { speaker ->
                if (speaker != null) {
                    val currentTime = System.currentTimeMillis()
                    val diff = currentTime - speaker.lastSeen
                    val isStale = speaker.lastSeen > 0L && diff > 120000L
                    val isOffline = !speaker.isOnline || isStale
                    val acceptedAnother = speaker.activeCall != null && 
                                          speaker.activeCall.callId.isNotEmpty() && 
                                          speaker.activeCall.callId != callId
                    
                    val requestedType = _currentSession.value?.requestedType ?: CallType.AUDIO
                    val mappedType = requestedType.toSupportedCallType()
                    val disabledType = !speaker.callPreferences.getTypedSupportedCallTypes().contains(mappedType)

                    val abortTriggered = isOffline || acceptedAnother || disabledType
                    Log.d("CALL_AUDIT", "presenceJob: speakerId=${speaker.id}, expectedCallId=$callId, activeCallId=${speaker.activeCall?.callId}, activeCallStatus=${speaker.activeCall?.status}, isOffline=$isOffline (isOnline=${speaker.isOnline}, isStale=$isStale), acceptedAnother=$acceptedAnother, disabledType=$disabledType, AbortTriggered=$abortTriggered")

                    if (abortTriggered) {
                        if (isHandled.compareAndSet(false, true)) {
                            Log.d("CallRoutingManager", "Ringing abort: offline=$isOffline, another=$acceptedAnother, disabled=$disabledType")
                            timeoutJob.cancel()
                            cleanupCallDocument(callId, "timeout")
                            val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = AttemptResult.TIMEOUT)
                            val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                            _currentSession.value = _currentSession.value?.copy(
                                attempts = currentAttempts + attemptEnd
                            )
                            logger.logAttemptEnd(_currentSession.value!!, attemptEnd)
                            _routingEvents.emit(RoutingEvent.Timeout(candidate))
                            this@coroutineScope.cancel()
                        }
                    }
                }
            }
        }

        val cacheTimeoutJob = launch {
            delay(RoutingConstants.CACHE_SYNC_TIMEOUT_MS)
            if (currentCoroutineContext().isActive) {
                if (isHandled.compareAndSet(false, true)) {
                    Log.e("CallRoutingManager", "Cache synchronization timeout for call $callId")
                    timeoutJob.cancel()
                    presenceJob.cancel()
                    cleanupCallDocument(callId, "timeout")
                    val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = AttemptResult.TIMEOUT)
                    val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                    _currentSession.value = _currentSession.value?.copy(
                        attempts = currentAttempts + attemptEnd
                    )
                    logger.logAttemptEnd(_currentSession.value!!, attemptEnd)
                    failSession(RoutingFailure.CacheSyncTimeout)
                    this@coroutineScope.cancel()
                }
            }
        }

        try {
            callRepository.observeCall(callId).collect { result ->
                when (result) {
                    is CallSnapshotResult.Exists -> {
                        cacheTimeoutJob.cancel()
                        val call = result.call
                        if (call.status == "declined" || call.status == "cancelled" || call.status == "timeout") {
                            if (isHandled.compareAndSet(false, true)) {
                                timeoutJob.cancel()
                                presenceJob.cancel()
                                val resultType = when (call.status) {
                                    "declined" -> AttemptResult.DECLINED
                                    "timeout" -> AttemptResult.TIMEOUT
                                    else -> AttemptResult.CANCELLED
                                }
                                val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = resultType)
                                val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                                _currentSession.value = _currentSession.value?.copy(
                                    attempts = currentAttempts + attemptEnd
                                )
                                logger.logAttemptEnd(_currentSession.value!!, attemptEnd)
                                this@coroutineScope.cancel()
                            }
                        } else if (call.status == "accepted") {
                            if (isHandled.compareAndSet(false, true)) {
                                timeoutJob.cancel()
                                presenceJob.cancel()
                                isConnected = true
                                val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = AttemptResult.ACCEPTED)
                                val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                                val finalSession = _currentSession.value?.copy(
                                    attempts = currentAttempts + attemptEnd,
                                    endedAt = System.currentTimeMillis(),
                                    totalRoutingTime = System.currentTimeMillis() - (_currentSession.value?.startedAt ?: 0L),
                                    connectedSpeaker = candidate,
                                    fallbackUsed = (_currentSession.value?.retryCount ?: 0) > 1,
                                    state = RoutingState.Connected(call)
                                )
                                _currentSession.value = finalSession
                                logger.logAttemptEnd(finalSession!!, attemptEnd)
                                logger.logSessionEnd(finalSession)
                                _routingEvents.emit(RoutingEvent.Connected(candidate))
                                _routingEvents.emit(RoutingEvent.Completed)
                                _currentSession.value = null
                                this@coroutineScope.cancel()
                            }
                        }
                    }
                    is CallSnapshotResult.Missing -> {
                        if (result.source == SnapshotSource.SERVER) {
                            cacheTimeoutJob.cancel()
                            if (isHandled.compareAndSet(false, true)) {
                                timeoutJob.cancel()
                                presenceJob.cancel()
                                val attemptEnd = attempt.copy(endedAt = System.currentTimeMillis(), result = AttemptResult.CANCELLED)
                                val currentAttempts = _currentSession.value?.attempts ?: emptyList()
                                _currentSession.value = _currentSession.value?.copy(
                                    attempts = currentAttempts + attemptEnd
                                )
                                logger.logAttemptEnd(_currentSession.value!!, attemptEnd)
                                this@coroutineScope.cancel()
                            }
                        }
                    }
                    is CallSnapshotResult.Error -> {
                        cacheTimeoutJob.cancel()
                        val err = result.throwable
                        if (err is com.google.firebase.firestore.FirebaseFirestoreException) {
                            val code = err.code
                            if (code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                if (isHandled.compareAndSet(false, true)) {
                                    timeoutJob.cancel()
                                    presenceJob.cancel()
                                    failSession(RoutingFailure.PermissionDenied)
                                    this@coroutineScope.cancel()
                                }
                                return@collect
                            }
                        }
                        Log.e("CallRoutingManager", "Non-terminal Firestore error in observeCall", err)
                    }
                }
            }
        } catch (e: CancellationException) {
            // Cancelled
        }
        isConnected
    }

    private suspend fun cleanupCallDocument(callId: String, status: String) {
        withContext(NonCancellable) {
            try {
                callRepository.updateCallStatus(callId, status)
            } catch (e: Exception) {
                Log.e("CallRoutingManager", "Failed to clean up call doc $callId", e)
            }
        }
    }

    override fun cancelRouting() {
        val session = _currentSession.value
        if (session != null) {
            activeJob?.cancel()
            coroutineScope.launch {
                val callId = session.currentCallId
                if (callId != null) {
                    cleanupCallDocument(callId, "cancelled")
                }
                val finalSession = session.copy(
                    endedAt = System.currentTimeMillis(),
                    totalRoutingTime = System.currentTimeMillis() - session.startedAt,
                    state = RoutingState.Failed(RoutingFailure.Cancelled)
                )
                logger.logSessionEnd(finalSession, RoutingFailure.Cancelled)
                _routingEvents.emit(RoutingEvent.Cancelled)
                _currentSession.value = null
            }
        }
    }

    private suspend fun failSession(failure: RoutingFailure) {
        val session = _currentSession.value
        if (session != null) {
            val finalSession = session.copy(
                endedAt = System.currentTimeMillis(),
                totalRoutingTime = System.currentTimeMillis() - session.startedAt,
                state = RoutingState.Failed(failure)
            )
            _currentSession.value = finalSession
            logger.logSessionEnd(finalSession, failure)
            _routingEvents.emit(RoutingEvent.Error(failure))
            _currentSession.value = null
        }
    }
}
