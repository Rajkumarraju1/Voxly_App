package com.rkdevstudios.voxly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.data.model.CallPreferences
import com.rkdevstudios.voxly.data.repository.UserRepository
import com.rkdevstudios.voxly.data.repository.CallRepository
import com.rkdevstudios.voxly.data.repository.WalletRepository
import com.rkdevstudios.voxly.data.repository.PlayBillingRepository
import com.rkdevstudios.voxly.data.agora.AgoraManager
import com.android.billingclient.api.ProductDetails
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.rkdevstudios.voxly.util.RingtoneManagerHelper
import com.rkdevstudios.voxly.data.routing.CallRoutingManager
import com.rkdevstudios.voxly.data.model.CallType
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val callRepository: CallRepository,
    private val walletRepository: WalletRepository,
    private val playBillingRepository: PlayBillingRepository,
    private val agoraManager: AgoraManager,
    private val ringtoneManager: RingtoneManagerHelper,
    private val callRoutingManager: CallRoutingManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private var toneGenerator: android.media.ToneGenerator? = null

    fun getAgoraManager(): AgoraManager = agoraManager

    fun toggleSpeaker(enable: Boolean) {
        agoraManager.toggleSpeaker(enable)
    }

    fun toggleVideo(enable: Boolean) {
        agoraManager.toggleVideo(enable)
    }

    val isRemoteUserSpeaking: StateFlow<Boolean> = agoraManager.isRemoteUserSpeaking

    private fun startRingbackTone() {
        if (toneGenerator == null) {
            try {
                toneGenerator =
                    android.media.ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, 100)
                toneGenerator?.startTone(android.media.ToneGenerator.TONE_SUP_RINGTONE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRingbackTone() {
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRingbackTone()
        ringtoneManager.stopRingtone()
        callTimerJob?.cancel()
        callTimeoutJob?.cancel()
        callListenerJob?.cancel()
        activeCallListenerJob?.cancel()
        // CRITICAL FIX: Do NOT call agoraManager.leaveChannel() here.
        // It kills the singleton Agora session when IncomingCallActivity is destroyed,
        // causing the "Green Dot Disappears" issue.
        // agoraManager.leaveChannel()
    }

    val currentUser: StateFlow<User?> = userRepository.currentUserFlow

    private val _speakers = MutableStateFlow<List<User>>(emptyList())
    val speakers: StateFlow<List<User>> = _speakers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Call Flow State
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var callListenerJob: kotlinx.coroutines.Job? = null
    private var activeCallListenerJob: kotlinx.coroutines.Job? = null
    private var callTimerJob: kotlinx.coroutines.Job? = null
    private var callTimeoutJob: kotlinx.coroutines.Job? = null
    private var lastNavigatedCallId: String? = null

    private fun emitNavigateToActiveCall(call: com.rkdevstudios.voxly.data.model.Call) {
        if (lastNavigatedCallId == call.id) {
            android.util.Log.d("HomeViewModel", "emitNavigateToActiveCall: Already navigated to call ${call.id}. Skipping duplicate.")
            return
        }
        lastNavigatedCallId = call.id
        viewModelScope.launch {
            _navigateToActiveCall.emit(call)
        }
    }
    private var lastVisible: com.google.firebase.firestore.DocumentSnapshot? = null

    // Transaction Pagination State
    private var lastTransactionVisible: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isTransactionEndReached = false
    private val _isTransactionLoading = MutableStateFlow(false)
    val isTransactionLoading: StateFlow<Boolean> = _isTransactionLoading.asStateFlow()

    // Financial State
    private val _coinsSpent = MutableStateFlow(0)
    val coinsSpent: StateFlow<Int> = _coinsSpent.asStateFlow()

    private val _transactions =
        MutableStateFlow<List<com.rkdevstudios.voxly.data.model.Transaction>>(emptyList())
    val transactions: StateFlow<List<com.rkdevstudios.voxly.data.model.Transaction>> =
        _transactions.asStateFlow()

    // Navigation Event for Active Calls (Caller Side)
    private val _navigateToActiveCall = kotlinx.coroutines.flow.MutableSharedFlow<com.rkdevstudios.voxly.data.model.Call>()
    val navigateToActiveCall = _navigateToActiveCall.asSharedFlow()

    private var audioTickCounter = 0

    val currentUserId: String?
        get() = userRepository.getCurrentUserId()

    // Play Billing State
    val coinProducts: StateFlow<List<ProductDetails>> = playBillingRepository.coinProducts

    init {
        // Listen to User changes for profile/coin updates
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    // One-time self-migration for speakers with old rates
                    if (user.isSpeaker && user.videoRate == 60) {
                        viewModelScope.launch {
                            try {
                                val updatedUser = user.copy(videoRate = com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN)
                                userRepository.updateUser(updatedUser)
                                android.util.Log.d("HomeViewModel", "Migrated speaker videoRate from 60 to ${com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN}")
                            } catch (e: Exception) {
                                android.util.Log.e("HomeViewModel", "Failed to migrate speaker videoRate", e)
                            }
                        }
                    }

                    // Start fresh transaction load if empty
                    if (_transactions.value.isEmpty() && !isTransactionEndReached) {
                        loadTransactions(reset = true)
                    }
                }
            }
        }
        
        // Listen to Purchase Events
        viewModelScope.launch {
            playBillingRepository.purchaseEvents.collect { event ->
                when (event) {
                    is com.rkdevstudios.voxly.data.repository.PurchaseEvent.Success -> {
                        verifyPurchase(event.purchase)
                    }
                    is com.rkdevstudios.voxly.data.repository.PurchaseEvent.Error -> {
                        _toastEvent.emit(event.message)
                        playBillingRepository.clearPurchaseEvent()
                    }
                    null -> {}
                }
            }
        }

        // Listen to Routing Events
        viewModelScope.launch {
            callRoutingManager.routingEvents.collect { event ->
                when (event) {
                    is com.rkdevstudios.voxly.data.routing.RoutingEvent.Calling -> {
                        startRingbackTone()
                    }
                    is com.rkdevstudios.voxly.data.routing.RoutingEvent.Timeout -> {
                        stopRingbackTone()
                        _toastEvent.emit("${event.speaker.displayName} isn't available right now. Finding another speaker...")
                    }
                    is com.rkdevstudios.voxly.data.routing.RoutingEvent.Switching -> {
                        stopRingbackTone()
                        _toastEvent.emit("${event.from.displayName} isn't available right now. Finding another speaker...")
                    }
                    is com.rkdevstudios.voxly.data.routing.RoutingEvent.Connected -> {
                        stopRingbackTone()
                    }
                    is com.rkdevstudios.voxly.data.routing.RoutingEvent.Cancelled -> {
                        stopRingbackTone()
                        _toastEvent.emit("Call cancelled.")
                    }
                    is com.rkdevstudios.voxly.data.routing.RoutingEvent.Error -> {
                        stopRingbackTone()
                        val msg = when (event.failure) {
                            is com.rkdevstudios.voxly.data.routing.RoutingFailure.RoutingAlreadyActive -> "Call already in progress"
                            is com.rkdevstudios.voxly.data.routing.RoutingFailure.NoCandidates -> "No speakers available right now. Try again later."
                            is com.rkdevstudios.voxly.data.routing.RoutingFailure.SpeakerOffline -> "Speaker is offline"
                            is com.rkdevstudios.voxly.data.routing.RoutingFailure.SpeakerBusy -> "Speaker is currently busy"
                            else -> "Failed to connect call"
                        }
                        _toastEvent.emit(msg)
                    }
                    else -> {}
                }
            }
        }

        // Listen to Routing Sessions to map to UI state
        viewModelScope.launch {
            callRoutingManager.currentSession.collect { session ->
                if (session == null) {
                    _callState.value = CallState.Idle
                } else {
                    when (val s = session.state) {
                        is com.rkdevstudios.voxly.data.routing.RoutingState.Idle -> {
                            _callState.value = CallState.Idle
                        }
                        is com.rkdevstudios.voxly.data.routing.RoutingState.Finding -> {
                            if (session.originalSpeakerId == null) {
                                _callState.value = CallState.Finding(if (s.type == com.rkdevstudios.voxly.data.model.CallType.VIDEO) "video" else "audio")
                            } else {
                                _callState.value = CallState.Idle
                            }
                        }
                        is com.rkdevstudios.voxly.data.routing.RoutingState.Ringing -> {
                            val candidate = session.remainingCandidates.find { it.id == session.currentSpeakerId }
                            _callState.value = CallState.Outgoing(
                                callId = s.callId,
                                speakerName = s.speakerName,
                                speakerAvatar = candidate?.avatarUrl ?: "",
                                rate = if (s.type == com.rkdevstudios.voxly.data.model.CallType.VIDEO) com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN else com.rkdevstudios.voxly.util.CoinConstants.AUDIO_COINS_PER_MIN,
                                speakerId = session.currentSpeakerId ?: "",
                                type = if (s.type == com.rkdevstudios.voxly.data.model.CallType.VIDEO) "video" else "audio"
                            )
                        }
                        is com.rkdevstudios.voxly.data.routing.RoutingState.Connected -> {
                            _callState.value = CallState.Idle
                            emitNavigateToActiveCall(s.call)
                        }
                        is com.rkdevstudios.voxly.data.routing.RoutingState.Failed -> {
                            _callState.value = CallState.Idle
                        }
                    }
                }
            }
        }

        // Deterministic Recovery Flow
        viewModelScope.launch {
            currentUser.filterNotNull().collect { user ->
                val activeCallRef = user.activeCall
                if (activeCallRef != null && activeCallRef.callId.isNotEmpty()) {
                    try {
                        val call = callRepository.listenToCall(activeCallRef.callId).first()
                        if (call != null) {
                            when (call.status) {
                                "ringing" -> {
                                    if (activeCallRef.role == com.rkdevstudios.voxly.data.model.CallRole.CALLER) {
                                        // Resume Outgoing Screen
                                        _callState.value = CallState.Outgoing(
                                            callId = call.id,
                                            speakerName = call.speakerName,
                                            speakerAvatar = call.speakerAvatar,
                                            rate = call.rate,
                                            speakerId = call.speakerId,
                                            type = call.type
                                        )
                                    }
                                }
                                "accepted", "active" -> {
                                    emitNavigateToActiveCall(call)
                                }
                                "ended", "cancelled", "timeout" -> {
                                    callRepository.updateCallStatus(call.id, call.status)
                                    _callState.value = CallState.Idle
                                }
                            }
                        } else {
                            userRepository.updateUser(user.copy(activeCall = null))
                            _callState.value = CallState.Idle
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    if (_callState.value is CallState.Active || _callState.value is CallState.Outgoing || _callState.value is CallState.Incoming) {
                        _callState.value = CallState.Idle
                    }
                }
            }
        }

        agoraManager.initialize()
        playBillingRepository.startConnection()
        fetchCurrentUser()

        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null && !user.isSpeaker) {
                    fetchSpeakers()
                }
            }
        }
    }

    fun launchBillingFlow(activity: android.app.Activity, productDetails: ProductDetails) {
        playBillingRepository.launchBillingFlow(activity, productDetails)
    }

    private fun verifyPurchase(purchase: com.android.billingclient.api.Purchase) {
        viewModelScope.launch {
            try {
                _toastEvent.emit("Verifying purchase...")
                val functions = FirebaseFunctions.getInstance()
                
                // For MVP, we assume 1 product per purchase
                val productId = purchase.products.firstOrNull() ?: return@launch
                
                val data = hashMapOf(
                    "purchaseToken" to purchase.purchaseToken,
                    "productId" to productId
                )

                val result = functions.getHttpsCallable("verifyPlayPurchase").call(data).await()

                val resultMap = result.data as? Map<String, Any>
                if (resultMap?.get("success") == true) {
                    // Consume the purchase so it can be bought again
                    val consumed = playBillingRepository.verifyAndConsumePurchase(purchase)
                    if (consumed) {
                        _toastEvent.emit("Purchase successful! Coins added.")
                        loadTransactions(reset = true) // Refresh history
                    } else {
                        _toastEvent.emit("Purchase verified but failed to consume. Contact support.")
                    }
                } else {
                     _toastEvent.emit("Purchase verification failed.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvent.emit("Error verifying purchase: ${e.message}")
            } finally {
                playBillingRepository.clearPurchaseEvent()
            }
        }
    }

    fun loadTransactions(reset: Boolean = false) {
        val user = currentUser.value ?: return
        if (_isTransactionLoading.value) return
        if (reset) {
            lastTransactionVisible = null
            isTransactionEndReached = false
            _transactions.value = emptyList()
        }
        if (isTransactionEndReached && !reset) return

        viewModelScope.launch {
            _isTransactionLoading.value = true
            try {
                val result = walletRepository.getTransactionsPaged(user.id, lastTransactionVisible)
                val newTransactions = result.first
                lastTransactionVisible = result.second
                
                if (newTransactions.isEmpty()) {
                    isTransactionEndReached = true
                } else {
                    if (reset) {
                        _transactions.value = newTransactions
                    } else {
                        val current = _transactions.value
                        val combined = (current + newTransactions).distinctBy { it.id }
                        _transactions.value = combined
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTransactionLoading.value = false
            }
        }
    }

    fun submitWithdrawal(amount: Double, method: String, details: String) {
        val userId = currentUser.value?.id ?: return
        
        if (amount < 500) {
            viewModelScope.launch { 
                _toastEvent.emit("Minimum withdrawal amount is ₹500") 
            }
            return
        }

        viewModelScope.launch {
            try {
                walletRepository.requestWithdrawal(userId, amount, method, details)
                _toastEvent.emit("Withdrawal requested successfully")
                loadTransactions(reset = true)
            } catch (e: Exception) {
                _toastEvent.emit("Failed to request withdrawal: ${e.message}")
            }
        }
    }

        fun loadMoreTransactions() {
            loadTransactions(reset = false)
        }

        private val _showWalletEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
        val showWalletEvent = _showWalletEvent.asSharedFlow()

        private val _toastEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
        val toastEvent = _toastEvent.asSharedFlow()

        // --- Call Flow Actions ---

        // 1. User initiates call
        fun initiateCall(speaker: User, type: String = "audio") {
            val caller = currentUser.value ?: return

            // Balance Check
            val callType = if (type == "video") CallType.VIDEO else CallType.AUDIO
            val requiredRate = com.rkdevstudios.voxly.util.CallPricing.getRequiredMinimum(callType)
            if (caller.coins < requiredRate) {
                viewModelScope.launch {
                    _showWalletEvent.emit("Insufficient balance. Please recharge.")
                }
                return
            }

            callRoutingManager.startRouting(speaker.id, if (type == "video") CallType.VIDEO else CallType.AUDIO)
        }


        private var currentActiveCallId: String? = null

        private fun startActiveCallMonitor(callId: String) {
            if (currentActiveCallId == callId && activeCallListenerJob?.isActive == true) {
                 return
            }

            activeCallListenerJob?.cancel()
            currentActiveCallId = callId
            
            activeCallListenerJob = viewModelScope.launch {
                try {
                    callRepository.listenToCall(callId).collect { call ->
                        val currentState = _callState.value
                        
                        // Handle Call Ended/Cancelled/Declined
                        if (call == null || call.status == "ended" || call.status == "declined" || call.status == "cancelled") {
                            stopRingbackTone()
                            // If we were OUTGOING (Ringing), execute cleanup
                            if (currentState is CallState.Outgoing) {
                                _callState.value = CallState.Idle
                                if (call?.status == "declined") {
                                     _toastEvent.emit("Call declined.")
                                } else if (call?.status == "busy") { // Assuming busy status
                                     _toastEvent.emit("User is busy.")
                                }
                            }
                            // Active calls are handled by CallViewModel/ActiveCallActivity now.
                            // We just stop monitoring here.
                            activeCallListenerJob?.cancel()
                            currentActiveCallId = null
                        } 
                        // Handle Call Accepted (Caller Side)
                        else if (call.status == "accepted" && currentState is CallState.Outgoing) {
                            // User side: Call is accepted.
                            stopRingbackTone()
                            callTimeoutJob?.cancel()

                            // Navigate to ActiveCallActivity
                            emitNavigateToActiveCall(call)

                            // IMPORTANT: Transition local state to Idle so we don't interfere.
                            // The ActiveCallActivity will exist independently.
                            // We might want to clear "Outgoing" state.
                            _callState.value = CallState.Idle
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    currentActiveCallId = null
                }
            }
        }

        private val _liveEarnings = MutableStateFlow(0.0)
        val liveEarnings: StateFlow<Double> = _liveEarnings.asStateFlow()

        private val _remainingSeconds = MutableStateFlow<Long>(-1)
        val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

        // Timer removed as it's now handled in CallViewModel

        fun startListeningForCalls() {
            val user = currentUser.value ?: return
            if (!user.isSpeaker) return

            callListenerJob?.cancel()
            callListenerJob = viewModelScope.launch {
                try {
                    callRepository.listenForIncomingCalls(user.id).collect { call ->
                        if (call != null) {
                            if (_callState.value is CallState.Active) {
                                android.util.Log.d(
                                    "HomeViewModel",
                                    "startListeningForCalls: Already in Active call, ignoring incoming update"
                                )
                                return@collect
                            }

                            if (_callState.value !is CallState.Incoming) {
                                ringtoneManager.playRingtone()
                            }
                            _callState.value = CallState.Incoming(
                                callId = call.id,
                                callerId = call.callerId,
                                callerName = call.callerName,
                                callerAvatar = call.callerAvatar,
                                rate = call.rate,
                                type = call.type
                            )
                        } else {
                            // If call stops ringing (cancelled/timeout), revert to Idle if currently Incoming
                            if (_callState.value is CallState.Incoming) {
                                android.util.Log.d(
                                    "HomeViewModel",
                                    "startListeningForCalls: Call stopped ringing, resetting to Idle from Incoming"
                                )
                                ringtoneManager.stopRingtone()
                                _callState.value = CallState.Idle
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun setIncomingCallFromIntent(
            callId: String,
            callerId: String,
            callerName: String,
            callerAvatar: String,
            rate: Int,
            type: String = "audio"
        ) {
            if (_callState.value is CallState.Idle) {
                ringtoneManager.playRingtone()
                _callState.value = CallState.Incoming(
                    callId = callId,
                    callerId = callerId,
                    callerName = callerName,
                    callerAvatar = callerAvatar,
                    rate = rate,
                    type = type
                )
                monitorIncomingCall(callId)
            }
        }


        private var currentMonitoringCallId: String? = null

        private fun monitorIncomingCall(callId: String) {
            if (currentMonitoringCallId == callId && callListenerJob?.isActive == true) {
                 android.util.Log.d("HomeViewModel", "monitorIncomingCall: Already listening for $callId. Skipping.")
                 return
            }
            
            callListenerJob?.cancel()
            currentMonitoringCallId = callId
            
            callListenerJob = viewModelScope.launch {
                try {
                    android.util.Log.d(
                        "HomeViewModel",
                        "monitorIncomingCall: Starting to listen for $callId"
                    )
                    callRepository.listenToCall(callId).collect { call ->
                        android.util.Log.d(
                            "HomeViewModel",
                            "monitorIncomingCall: Update received for $callId. Status=${call?.status}"
                        )
                        if (call == null || call.status == "cancelled" || call.status == "ended") {
                            android.util.Log.d(
                                "HomeViewModel",
                                "monitorIncomingCall: Call ended/cancelled ($call). Resetting to Idle."
                            )
                            ringtoneManager.stopRingtone()
                            _callState.value = CallState.Idle
                            callListenerJob?.cancel()
                            currentMonitoringCallId = null
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "monitorIncomingCall: Error", e)
                    e.printStackTrace()
                    currentMonitoringCallId = null
                }
            }
        }

        fun handleAcceptedCall(
            callId: String,
            callerId: String,
            callerName: String,
            callerAvatar: String,
            rate: Int,
            type: String = "audio"
        ) {
            // This method was previously used to transition state.
            // Now, IncomingCallActivity handles it by launching ActiveCallActivity directly.
            // But if HomeViewModel is used on the caller side, we might need similar logic?
            // Wait, this method is typically called by IncomingCallActivity before launching.
            // Since we decoupled, we can make this a no-op or just ensure cleanup.
            
            // Cleanup any monitoring
            callListenerJob?.cancel()
            currentMonitoringCallId = null
            
            // We do NOT set Active state here anymore. ActiveCallActivity will take over.
            _callState.value = CallState.Idle
        }

        suspend fun acceptCall() {
            val currentState = _callState.value
            var user = currentUser.value
            ringtoneManager.stopRingtone()

            if (currentState is CallState.Incoming) {
                if (user == null) {
                    val uid = userRepository.getCurrentUserId()
                    if (uid != null) {
                        try {
                            android.util.Log.d("HomeViewModel", "acceptCall: User flow is null. Loading user dynamically for UID=$uid")
                            user = userRepository.getUser(uid)
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "acceptCall: Failed to load user dynamically", e)
                        }
                    }
                }

                val displayName = user?.displayName ?: "Speaker"
                val speakerId = user?.id ?: userRepository.getCurrentUserId() ?: ""

                // Optimistically set Active State FIRST (Synchronously) to prevent double-join from MainActivity intent
                val activeState = CallState.Active(
                    callId = currentState.callId,
                    callerId = currentState.callerId,
                    callerName = currentState.callerName,
                    speakerName = displayName, // I am Speaker
                    otherAvatar = currentState.callerAvatar,
                    rate = currentState.rate,
                    startTime = System.currentTimeMillis(),
                    speakerId = speakerId,
                    type = if (currentState.rate >= com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN) "video" else "audio" // Infer type
                )
                _callState.value = activeState

                // CRITICAL: Stop listening for incoming updates
                callListenerJob?.cancel()
                currentMonitoringCallId = null

                // Ensure Firestore update completes synchronously within the caller's coroutine scope
                try {
                    callRepository.updateCallStatus(currentState.callId, "accepted")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Agora joining and monitoring are now exclusively handled by CallViewModel in ActiveCallActivity.
            }
        }

        fun declineCall() {
            val currentState = _callState.value
            ringtoneManager.stopRingtone()

            if (currentState is CallState.Incoming) {
                viewModelScope.launch {
                    android.util.Log.d(
                        "HomeViewModel",
                        "declineCall: Declining call, resetting to Idle"
                    )
                    callRepository.updateCallStatus(currentState.callId, "declined")
                    _callState.value = CallState.Idle
                }
            }
        }

        fun endCall() {
            val currentState = _callState.value
            ringtoneManager.stopRingtone()
            stopRingbackTone()

            // Handle OUTGOING (Ringing) - Cancel the call
            if (currentState is CallState.Outgoing) {
                viewModelScope.launch {
                    try {
                        android.util.Log.d("HomeViewModel", "endCall: Cancelling recovered/active outgoing call=${currentState.callId}")
                        callRepository.updateCallStatus(currentState.callId, "cancelled")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    _callState.value = CallState.Idle
                }
                callRoutingManager.cancelRouting()
                return
            }

            // Handle ACTIVE - Handled by CallViewModel now
            if (currentState is CallState.Active) {
                agoraManager.leaveChannel()
                _callState.value = CallState.Idle
            }
        }

        fun closeSummary() {
            _callState.value = CallState.Idle
        }

        fun fetchCurrentUser() {
            val userId = userRepository.getCurrentUserId() ?: return
            userRepository.listenToUser(userId)
        }

        private fun sanitizeSpeakerList(list: List<User>): List<User> {
            return list.map { speaker ->
                if (speaker.videoRate == 60) {
                    speaker.copy(videoRate = com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN)
                } else {
                    speaker
                }
            }
        }

        fun loadMoreSpeakers() {
            if (_isLoading.value) return
            val user = currentUser.value ?: return

            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val userLanguages = user.languages
                    // Pass lastVisible to get next page
                    val result = userRepository.getSpeakers(userLanguages, lastVisible)
                    val newSpeakers = result.first
                    lastVisible = result.second

                    if (newSpeakers.isNotEmpty()) {
                        val currentList = _speakers.value
                        val distinctNewSpeakers = newSpeakers.filter { new ->
                            currentList.none { existing -> existing.id == new.id }
                        }

                        if (distinctNewSpeakers.isNotEmpty()) {
                            _speakers.value = sanitizeSpeakerList(currentList + distinctNewSpeakers)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun refreshSpeakers() {
            viewModelScope.launch {
                _speakers.value = emptyList() // Clear list (optional, might cause blink)
                fetchSpeakers()
            }
        }

        fun submitDowngradeRequest() {
            val user = currentUser.value ?: return
            viewModelScope.launch {
                val updatedUser = user.copy(downgradeRequest = true)
                try {
                    userRepository.updateUser(updatedUser)
                    _toastEvent.emit("Downgrade request submitted to Admin.")
                } catch (e: Exception) {
                    _toastEvent.emit("Failed to submit request.")
                }
            }
        }

        private suspend fun fetchSpeakers() {
            try {
                _isLoading.value = true
                lastVisible = null // Reset cursor
                val userLanguages = currentUser.value?.languages ?: emptyList()

                // 1. Try to load from Cache first (ignoring errors)
                try {
                    val cacheResult = userRepository.getSpeakers(
                        userLanguages,
                        null,
                        com.google.firebase.firestore.Source.CACHE
                    )
                    if (cacheResult.first.isNotEmpty()) {
                        _speakers.value = sanitizeSpeakerList(cacheResult.first)
                    }
                } catch (e: Exception) {
                    // Cache miss or error - proceed to server
                }

                // 2. Fetch from Server (Fresh Data)
                val result = userRepository.getSpeakers(
                    userLanguages,
                    null,
                    com.google.firebase.firestore.Source.SERVER
                )
                _speakers.value = sanitizeSpeakerList(result.first)
                lastVisible = result.second
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }

        fun toggleOnlineStatus(isOnline: Boolean) {
            if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                android.util.Log.d("ONLINE_TRACE", "WRITER=HomeViewModel.toggleOnlineStatus isOnline=$isOnline")
            }
            val user = currentUser.value
            if (user == null) {
                android.util.Log.e("ONLINE_TRACE", "WRITER=HomeViewModel.toggleOnlineStatus FAILED user is null")
                return
            }
            viewModelScope.launch {
                val updatedUser = user.copy(
                    isOnline = isOnline,
                    presence = if (isOnline) "ONLINE" else "OFFLINE",
                    lastSeen = if (isOnline) System.currentTimeMillis() else user.lastSeen
                )
                try {
                    if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                        android.util.Log.d("ONLINE_TRACE", "WRITER=HomeViewModel.toggleOnlineStatus calling userRepository.updateUser with isOnline=${updatedUser.isOnline} lastSeen=${updatedUser.lastSeen}")
                    }
                    userRepository.updateUser(updatedUser)

                    // Ensure service is started/stopped accordingly
                    val serviceIntent = android.content.Intent(
                        context,
                        com.rkdevstudios.voxly.service.CallForegroundService::class.java
                    )
                    if (isOnline) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.stopService(serviceIntent)
                    }
                } catch (e: Exception) {
                }
            }
        }

        fun updateCallPreferences(prefs: CallPreferences) {
            val user = currentUser.value ?: return
            viewModelScope.launch {
                val updatedUser = user.copy(callPreferences = prefs)
                try {
                    userRepository.updateUser(updatedUser)
                } catch (e: Exception) {
                }
            }
        }

        fun updateAvatar(avatarName: String) {
            val user = currentUser.value ?: return
            viewModelScope.launch {
                val updatedUser = user.copy(avatarUrl = avatarName)
                try {
                    userRepository.updateUser(updatedUser)
                } catch (e: Exception) {
                }
            }
        }

        suspend fun updateFullProfile(
            name: String,
            tagline: String,
            tags: List<String>,
            avatarUrl: String
        ) {
            val user = currentUser.value ?: return
            val updatedUser = user.copy(
                displayName = name,
                tagline = tagline,
                tags = tags,
                avatarUrl = avatarUrl
            )
            try {
                kotlinx.coroutines.delay(1000)
                userRepository.updateUser(updatedUser)
            } catch (e: Exception) {
            }
        }

        fun submitRating(speakerId: String, rating: Int) {
            viewModelScope.launch {
                try {
                    userRepository.rateSpeaker(speakerId, rating)

                    // Optimistically update local list to reflect changes immediately
                    val currentSpeakers = _speakers.value.toMutableList()
                    val index = currentSpeakers.indexOfFirst { it.id == speakerId }
                    if (index != -1) {
                        val speaker = currentSpeakers[index]
                        val updatedSpeaker = speaker.copy(
                            ratingSum = speaker.ratingSum + rating,
                            ratingCount = speaker.ratingCount + 1
                        )
                        currentSpeakers[index] = updatedSpeaker
                        _speakers.value = currentSpeakers
                    }

                    android.util.Log.d(
                        "HomeViewModel",
                        "submitRating: Rating submitted, resetting to Idle"
                    )
                    _callState.value = CallState.Idle
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun submitListenerApplication(
            phoneNumber: String,
            gender: String,
            languages: List<String>
        ) {
            val user = currentUser.value ?: return
            val updatedUser = user.copy(
                phoneNumber = phoneNumber,
                gender = gender,
                languages = languages,
                verificationStatus = "pending"
            )
            viewModelScope.launch {
                try {
                    userRepository.updateUser(updatedUser)
                    // Here we could trigger a backend notification or email
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }



        fun initiateRandomCall(type: String) {
            val user = currentUser.value ?: return
            val callType = if (type == "video") CallType.VIDEO else CallType.AUDIO
            val requiredRate = com.rkdevstudios.voxly.util.CallPricing.getRequiredMinimum(callType)
            if (user.coins < requiredRate) {
                viewModelScope.launch {
                    _showWalletEvent.emit("Insufficient balance. Please recharge.")
                }
                return
            }
            callRoutingManager.startRouting(null, if (type == "video") CallType.VIDEO else CallType.AUDIO)
        }

        fun reportUser(userId: String, reason: String, description: String) {
            viewModelScope.launch {
                try {
                    userRepository.reportUser(userId, reason, description)
                    _toastEvent.emit("User reported successfully.")
                } catch (e: Exception) {
                    _toastEvent.emit("Failed to report user.")
                }
            }
        }

        fun blockUser(userId: String) {
            viewModelScope.launch {
                try {
                    userRepository.blockUser(userId)
                    _toastEvent.emit("User blocked.")
                    // If in a call with this user, end it? Optional but recommended.
                    if (_callState.value is CallState.Active) {
                        val activeCall = _callState.value as CallState.Active
                        if (activeCall.speakerId == userId || activeCall.callerId == userId) {
                            endCall()
                        }
                    }
                } catch (e: Exception) {
                    _toastEvent.emit("Failed to block user.")
                }
            }
        }

        fun initiateSmartRedial(speakerId: String, type: String) {
            val user = currentUser.value ?: return
            val callType = if (type == "video") CallType.VIDEO else CallType.AUDIO
            val requiredRate = com.rkdevstudios.voxly.util.CallPricing.getRequiredMinimum(callType)
            if (user.coins < requiredRate) {
                viewModelScope.launch {
                    _showWalletEvent.emit("Insufficient balance. Please recharge.")
                }
                return
            }
            callRoutingManager.startRouting(speakerId, if (type == "video") CallType.VIDEO else CallType.AUDIO)
        }

        fun cancelFinding() {
            callRoutingManager.cancelRouting()
        }

        fun requestVideoSwitch() {
            val currentState = _callState.value
            if (currentState is CallState.Active) {
                val user = currentUser.value ?: return

                // Only Caller pays. Speaker can request freely (Caller will accept/deny).
                if (!user.isSpeaker) {
                    // Video requires 60 coins/min. Check if they have enough for at least 1 min.
                    val currentSpent = _coinsSpent.value
                    val available = user.coins - currentSpent

                    if (available < com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN) {
                        viewModelScope.launch {
                            _showWalletEvent.emit("Need ${com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN} coins for video. Please recharge.")
                        }
                        return
                    }
                }

                viewModelScope.launch {
                    // Use Auth ID as fallback if User object isn't fully loaded yet
                    val myId = currentUser.value?.id ?: userRepository.getCurrentUserId()

                    if (myId == null) return@launch

                    try {
                        callRepository.sendVideoRequest(currentState.callId, myId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun updateUserLanguage(language: String) {
            val user = currentUser.value ?: return
            viewModelScope.launch {
                val updatedUser = user.copy(languages = listOf(language))
                try {
                    userRepository.updateUser(updatedUser)
                    // Refresh speakers to show new language matches
                    refreshSpeakers()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun updatePaymentDetails(user: com.rkdevstudios.voxly.data.model.User) {
            viewModelScope.launch {
                try {
                    userRepository.updateUser(user)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun logout() {
            userRepository.logout()
            _callState.value = CallState.Idle
        }

        fun deleteAccount() {
            viewModelScope.launch {
                try {
                    userRepository.deleteAccount()
                    _callState.value = CallState.Idle
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun respondToVideoSwitch(accept: Boolean) {
            val currentState = _callState.value
            if (currentState is CallState.Active) {
                val user = currentUser.value ?: return

                // If accepting and I am the Caller (payer), check balance again
                if (accept && !user.isSpeaker) {
                    val currentSpent = _coinsSpent.value
                    val available = user.coins - currentSpent
                    if (available < com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN) {
                        viewModelScope.launch {
                            _showWalletEvent.emit("Insufficient balance to switch to video.")
                            // Optionally decline automatically?
                            callRepository.respondToVideoRequest(currentState.callId, false)
                        }
                        return
                    }
                }

                viewModelScope.launch {
                    callRepository.respondToVideoRequest(currentState.callId, accept)
                }
            }
        }

// End of Class
    }

    sealed class CallState {
        object Idle : CallState()
        data class Finding(val type: String) : CallState()
        data class Incoming(
            val callId: String,
            val callerId: String,
            val callerName: String,
            val callerAvatar: String,
            val rate: Int,
            val type: String
        ) : CallState()

        data class Outgoing(
            val callId: String,
            val speakerId: String,
            val speakerName: String,
            val speakerAvatar: String,
            val rate: Int,
            val type: String
        ) : CallState()

        data class Active(
            val callId: String,
            val callerId: String,
            val callerName: String,
            val speakerName: String, // Added
            val otherAvatar: String,
            val rate: Int,
            val startTime: Long,
            val speakerId: String,
            val type: String,
            val videoRequestStatus: String? = null,
            val videoRequestBy: String? = null
        ) : CallState()

        data class Summary(
            val duration: String,
            val earnings: String,
            val speakerId: String?
        ) : CallState()
    }


