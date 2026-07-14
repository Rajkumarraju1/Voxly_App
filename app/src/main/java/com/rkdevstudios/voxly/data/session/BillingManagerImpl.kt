package com.rkdevstudios.voxly.data.session

import com.rkdevstudios.voxly.data.repository.CallRepository
import com.rkdevstudios.voxly.data.repository.UserRepository
import com.rkdevstudios.voxly.util.CoinConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManagerImpl @Inject constructor(
    private val sessionManager: CallSessionManager,
    private val userRepository: UserRepository,
    private val callRepository: CallRepository
) : BillingManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var billingJob: Job? = null

    private val _durationSeconds = MutableStateFlow(0L)
    override val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _earnedCoins = MutableStateFlow(0.0)
    override val earnedCoins: StateFlow<Double> = _earnedCoins.asStateFlow()

    private val _coinsSpent = MutableStateFlow(0)
    override val coinsSpent: StateFlow<Int> = _coinsSpent.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Long>(-1)
    override val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _billingSnapshot = MutableStateFlow(BillingSnapshot())
    override val billingSnapshot: StateFlow<BillingSnapshot> = _billingSnapshot.asStateFlow()

    private val closedSegments = mutableListOf<BillingSegment>()
    private var closedCoinsSpent = 0
    private var activeSegmentStartElapsedMs = 0L
    private var currentType: com.rkdevstudios.voxly.data.model.CallType? = null
    private var currentRate: Double = 0.0

    init {
        scope.launch {
            sessionManager.sessionState.collect { state ->
                when (state) {
                    is SessionState.Connected -> {
                        startBilling()
                    }
                    is SessionState.Ending, is SessionState.Ended -> {
                        stopBilling()
                    }
                    is SessionState.Idle, is SessionState.Initializing, is SessionState.JoiningAgora, is SessionState.WaitingRemoteUser -> {
                        resetBillingState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun resetBillingState() {
        closedSegments.clear()
        closedCoinsSpent = 0
        activeSegmentStartElapsedMs = 0L
        currentType = null
        currentRate = 0.0
        _durationSeconds.value = 0L
        _earnedCoins.value = 0.0
        _coinsSpent.value = 0
        _remainingSeconds.value = -1
        _billingSnapshot.value = BillingSnapshot()
    }

    private fun getIntervalSeconds(type: com.rkdevstudios.voxly.data.model.CallType): Int {
        return if (type == com.rkdevstudios.voxly.data.model.CallType.VIDEO) {
            CoinConstants.VIDEO_INTERVAL_SECONDS
        } else {
            CoinConstants.AUDIO_INTERVAL_SECONDS
        }
    }

    private fun switchSegment(call: com.rkdevstudios.voxly.data.model.Call) {
        val callType = com.rkdevstudios.voxly.data.model.CallType.valueOf(call.type.uppercase())
        val rateVal = call.rate.toDouble()

        if (activeSegmentStartElapsedMs > 0L) {
            if (currentType == callType && currentRate == rateVal) {
                return
            }
            // Finalize previous segment
            val endMs = android.os.SystemClock.elapsedRealtime()
            val segment = BillingSegment(
                startElapsedRealtimeMs = activeSegmentStartElapsedMs,
                endElapsedRealtimeMs = endMs,
                type = currentType ?: com.rkdevstudios.voxly.data.model.CallType.AUDIO,
                rate = currentRate
            )
            closedSegments.add(segment)

            val durationSec = (endMs - activeSegmentStartElapsedMs) / 1000
            val interval = getIntervalSeconds(currentType ?: com.rkdevstudios.voxly.data.model.CallType.AUDIO)
            closedCoinsSpent += (durationSec / interval).toInt()
        }

        activeSegmentStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        currentType = callType
        currentRate = rateVal
    }

    private fun startBilling() {
        val call = sessionManager.currentCall.value
        val user = userRepository.currentUserFlow.value
        val role = if (user?.isSpeaker == true) "Speaker" else "Caller"
        android.util.Log.d("BILLING_AUDIT", "BillingManager.startBilling() invoked. instanceHash=${System.identityHashCode(this)} | role=$role | callId=${call?.id}")

        if (billingJob != null && billingJob?.isActive == true) {
            android.util.Log.d("CALL_AUDIT", "startBilling: Billing job is already active. Ignoring duplicate request.")
            return
        }
        billingJob?.cancel()
        resetBillingState()

        val callObj = call ?: return

        val startTime = android.os.SystemClock.elapsedRealtime()

        billingJob = scope.launch {
            android.util.Log.d("BILLING_AUDIT", "startBilling coroutine launched. activeRole=$role | callId=${callObj.id}")
            android.util.Log.d("CALL_AUDIT", "startBilling: Awaiting verified user balance...")
            val result = userRepository.awaitVerifiedUser(com.rkdevstudios.voxly.util.CallPricing.USER_VERIFICATION_TIMEOUT_MS)

            val verifiedUser = when (result) {
                is com.rkdevstudios.voxly.data.repository.VerificationResult.Success -> {
                    result.user
                }
                is com.rkdevstudios.voxly.data.repository.VerificationResult.Timeout -> {
                    android.util.Log.e("CALL_AUDIT", "startBilling: Verification TIMEOUT. Leaving session.")
                    sessionManager.leaveSession()
                    return@launch
                }
                is com.rkdevstudios.voxly.data.repository.VerificationResult.Failure -> {
                    android.util.Log.e("CALL_AUDIT", "startBilling: Verification FAILURE: ${result.error.message}. Leaving session.")
                    sessionManager.leaveSession()
                    return@launch
                }
            }

            val isSpeaker = verifiedUser.isSpeaker

            if (!isSpeaker) {
                val callType = com.rkdevstudios.voxly.data.model.CallType.valueOf(call.type.uppercase())
                val requiredMin = com.rkdevstudios.voxly.util.CallPricing.getRequiredMinimum(callType)
                android.util.Log.d("CALL_AUDIT", "startBilling: Caller verified balance=${verifiedUser.coins}, requiredMin=$requiredMin")
                if (verifiedUser.coins < requiredMin) {
                    android.util.Log.w("CALL_AUDIT", "startBilling: Caller has insufficient balance. Leaving session.")
                    sessionManager.leaveSession()
                    return@launch
                }
            }

            switchSegment(call)

            while (isActive) {
                delay(1000)

                val activeCall = sessionManager.currentCall.value
                if (activeCall == null) {
                    sessionManager.leaveSession()
                    break
                }

                switchSegment(activeCall)

                val now = android.os.SystemClock.elapsedRealtime()
                val totalElapsedSec = (now - startTime) / 1000
                _durationSeconds.value = totalElapsedSec

                val activeDurationSec = (now - activeSegmentStartElapsedMs) / 1000
                val interval = getIntervalSeconds(currentType ?: com.rkdevstudios.voxly.data.model.CallType.AUDIO)
                val activeCoins = (activeDurationSec / interval).toInt()

                val totalCoinsSpent = closedCoinsSpent + activeCoins
                android.util.Log.d("BILLING_AUDIT", "Coin deduction evaluated. role=$role | timestamp=${System.currentTimeMillis()} | coinsSpent=$totalCoinsSpent")
                _coinsSpent.value = totalCoinsSpent
                _earnedCoins.value = totalCoinsSpent.toDouble()

                if (!isSpeaker) {
                    val updatedUser = userRepository.currentUserFlow.value
                    if (updatedUser != null) {
                        val activeCallType = currentType ?: com.rkdevstudios.voxly.data.model.CallType.AUDIO
                        val rateForActive: Double = if (activeCallType == com.rkdevstudios.voxly.data.model.CallType.VIDEO) {
                            CoinConstants.VIDEO_COINS_PER_MIN.toDouble()
                        } else {
                            currentRate
                        }
                        val coinsPerSecond: Double = rateForActive / 60.0
                        if (coinsPerSecond > 0.0) {
                            val maxSeconds = (updatedUser.coins / coinsPerSecond).toLong()
                            val left = maxSeconds - activeDurationSec
                            _remainingSeconds.value = if (left > 0) left else 0

                            if (left <= 0) {
                                android.util.Log.w("CALL_AUDIT", "startBilling: Call duration limit reached. Leaving session.")
                                sessionManager.leaveSession()
                                break
                            }
                        }
                    }
                } else {
                    _remainingSeconds.value = -1
                }

                _billingSnapshot.value = BillingSnapshot(
                    elapsedSeconds = totalElapsedSec,
                    callerCoinsSpent = totalCoinsSpent,
                    speakerCoinsEarned = totalCoinsSpent.toDouble() * (com.rkdevstudios.voxly.util.CoinConstants.currentConfig.speakerRate),
                    activeRate = currentRate,
                    activeCallType = currentType ?: com.rkdevstudios.voxly.data.model.CallType.AUDIO
                )
            }
        }
    }

    private fun stopBilling() {
        val user = userRepository.currentUserFlow.value
        val role = if (user?.isSpeaker == true) "Speaker" else "Caller"
        android.util.Log.d("BILLING_AUDIT", "BillingManager.stopBilling() invoked. instanceHash=${System.identityHashCode(this)} | role=$role")

        billingJob?.cancel()
        billingJob = null

        val call = sessionManager.currentCall.value
        if (call != null) {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isCaller = currentUid == call.callerId

            val snapshot = _billingSnapshot.value
            val duration = snapshot.elapsedSeconds.toInt()
            val spent = snapshot.callerCoinsSpent

            scope.launch {
                try {
                    if (isCaller) {
                        android.util.Log.d("BILLING_AUDIT", "stopBilling: Current user is Caller ($currentUid). Calling endCallSession(). callId=${call.id} | duration=$duration | coinsSpent=$spent")
                        com.rkdevstudios.voxly.util.CallBgAudit.log(
                            component = "BillingManager",
                            event = "stopBilling_cleanup",
                            effect = "Finalizing call session and clearing activeCall for callId=${call.id}",
                            callId = call.id
                        )
                        callRepository.endCallSession(
                            callId = call.id,
                            durationSeconds = duration,
                            coinsDeducted = spent
                        )
                    } else {
                        android.util.Log.d("BILLING_AUDIT", "stopBilling: Current user is Speaker ($currentUid). Skipping endCallSession. callId=${call.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BILLING_AUDIT", "stopBilling: endCallSession failed. role=$role", e)
                    e.printStackTrace()
                }
            }
        } else {
            resetBillingState()
        }
    }
}
