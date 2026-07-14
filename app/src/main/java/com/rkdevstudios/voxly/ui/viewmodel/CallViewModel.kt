package com.rkdevstudios.voxly.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkdevstudios.voxly.data.agora.AgoraManager
import com.rkdevstudios.voxly.data.model.Call
import com.rkdevstudios.voxly.data.repository.CallRepository
import com.rkdevstudios.voxly.data.repository.toCallOrNull
import com.rkdevstudios.voxly.data.repository.UserRepository
import com.rkdevstudios.voxly.util.CoinConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val agoraManager: AgoraManager,
    private val callSessionManager: com.rkdevstudios.voxly.data.session.CallSessionManager,
    private val billingManager: com.rkdevstudios.voxly.data.session.BillingManager
) : ViewModel() {

    // Retrieve arguments from SavedStateHandle (passed via Intent)
    private val callId: String = checkNotNull(savedStateHandle["CALL_ID"])
    // These might be null if we join existing, but for now we assume passed
    private val _callerId: String = savedStateHandle["CALLER_ID"] ?: "" 
    private val _callerName: String = savedStateHandle["CALLER_NAME"] ?: "Unknown"
    private val _callerAvatar: String = savedStateHandle["CALLER_AVATAR"] ?: ""
    private val _initialRate: Int = savedStateHandle.get<Int>("RATE") ?: 10
    private val _initialType: String = savedStateHandle["TYPE"] ?: "audio"

    // UI State
    sealed class CallUiState {
        object Loading : CallUiState()
        data class Active(
            val callId: String,
            val callerId: String,
            val callerName: String,
            val otherAvatar: String,
            val rate: Int,
            val type: String,
            val startTime: Long,
            val videoRequestStatus: String? = null,
            val videoRequestBy: String? = null,
            val otherUserId: Int = 0, // Agora UID
            val currentUserId: String = "", // Added field
            val isSpeaker: Boolean = false
        ) : CallUiState()
        
        data class Ended(
            val duration: String,
            val earnings: String,
            val isSpeaker: Boolean,
            val speakerId: String? = null, // Added for rating
            val reason: String = "Call Ended"
        ) : CallUiState()
    }

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Loading)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    // Timer & Earnings
    private val _liveCoins = MutableStateFlow(0.0)
    val liveCoins: StateFlow<Double> = _liveCoins.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Long>(-1)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()
    
    // Remote Speaking Indicator
    val isRemoteUserSpeaking: StateFlow<Boolean> = agoraManager.isRemoteUserSpeaking

    // Billing Snapshot
    val billingSnapshot: StateFlow<com.rkdevstudios.voxly.data.session.BillingSnapshot> = billingManager.billingSnapshot

    // Events (Toasts, Navigation)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var callListenerJob: Job? = null
    private var timerJob: Job? = null
    
    // Internal tracking
    private var isSpeaker = false
    private var currentUserId = ""

    private var isInitialized = false

    init {
        viewModelScope.launch {
            billingManager.earnedCoins.collect {
                _liveCoins.value = it
            }
        }
        viewModelScope.launch {
            billingManager.remainingSeconds.collect {
                _remainingSeconds.value = it
                if (isInitialized && it == 0L && !isSpeaker) {
                    endCallLocally("Insufficient balance")
                }
            }
        }
        viewModelScope.launch {
            callSessionManager.sessionState.collect { state ->
                android.util.Log.d("CallViewModel", "sessionState collected: $state")
                if (state is com.rkdevstudios.voxly.data.session.SessionState.Ended) {
                    val durationSeconds = billingManager.durationSeconds.value
                    val durationStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
                    val earnings = if (isSpeaker) {
                        val computedEarnings = billingManager.earnedCoins.value * com.rkdevstudios.voxly.util.CoinConstants.currentConfig.speakerRate
                        String.format(java.util.Locale.getDefault(), "%.2f", computedEarnings)
                    } else {
                        billingManager.coinsSpent.value.toString()
                    }
                    com.rkdevstudios.voxly.util.CallBgAudit.log(
                        component = "CallViewModel",
                        event = "sessionState_Ended_transitioning_UI",
                        effect = "duration=$durationStr | earnings=$earnings",
                        callId = callId
                    )
                    _uiState.value = CallUiState.Ended(
                        duration = durationStr,
                        earnings = earnings,
                        isSpeaker = isSpeaker,
                        reason = "Call Ended"
                    )
                }
            }
        }
    }

    fun startCall() {
        if (isInitialized) {
            android.util.Log.d("CallViewModel", "startCall: Already initialized. Skipping.")
            return
        }
        isInitialized = true
        
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallViewModel",
            event = "startCall",
            effect = "Starting call initialization",
            callId = callId
        )
        viewModelScope.launch {
            try {
                android.util.Log.d("CallViewModel", "startCall: Starting initialization")
                var user = userRepository.currentUserFlow.value
                if (user == null) {
                    val uid = userRepository.getCurrentUserId()
                    if (uid != null) {
                        user = userRepository.getUser(uid)
                    }
                }
                currentUserId = user?.id ?: ""
                
                // Initialize Agora and take ownership of session
                val callResult = callRepository.observeCall(callId).first()
                val call = callResult.toCallOrNull() ?: com.rkdevstudios.voxly.data.model.Call(
                    id = callId,
                    callerId = _callerId,
                    speakerId = "",
                    rate = _initialRate,
                    type = _initialType,
                    status = "ringing"
                )

                isSpeaker = (currentUserId != call.callerId)
                android.util.Log.d("CallViewModel", "startCall: User info loaded. ID=$currentUserId, determined isSpeaker=$isSpeaker (callerId=${call.callerId})")

                android.util.Log.d("CallViewModel", "startCall: Handing over ownership to CallSessionManager")
                callSessionManager.takeOwnership(call)
                 _uiState.value = CallUiState.Active(
                    callId = callId,
                    callerId = _callerId,
                    callerName = _callerName,
                    otherAvatar = _callerAvatar,
                    rate = _initialRate,
                    type = _initialType,
                    startTime = System.currentTimeMillis(),
                    otherUserId = 0, // Will update when they join
                    currentUserId = currentUserId,
                    isSpeaker = isSpeaker
                )
                android.util.Log.d("CallViewModel", "startCall: UI State set to Active")
                isInitialized = true

                // Start Listening to Firestore
                startMonitoringCall()
                
                // Start Listening for Remote User Join
                viewModelScope.launch {
                    agoraManager.remoteUserJoined.collect { remoteUid ->
                        if (remoteUid != null) {
                             android.util.Log.d("CallViewModel", "Remote User Joined: $remoteUid. Updating UI.")
                             val currentState = _uiState.value
                             if (currentState is CallUiState.Active) {
                                 _uiState.value = currentState.copy(otherUserId = remoteUid)
                             }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CallViewModel", "startCall: Critical Error", e)
                _eventFlow.emit("Call Error: ${e.message}")
                try {
                    callRepository.updateCallStatus(callId, "ended")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                _uiState.value = CallUiState.Ended(
                    duration = "0:00",
                    earnings = "0",
                    isSpeaker = isSpeaker,
                    reason = "Connection Error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    private fun startMonitoringCall() {
        callListenerJob?.cancel()
        callListenerJob = viewModelScope.launch {
            callRepository.listenToCall(callId).collect { call ->
                if (call == null) return@collect
                
                android.util.Log.d("CallViewModel", "Call Update: Status=${call.status}")

                if (call.status == "ended" || call.status == "declined" || call.status == "cancelled") {
                     handleCallEnded(call)
                } else if (call.status == "accepted" || call.status == "ringing") { // 'ringing' might happen if we joined too fast
                     val existingCall = callSessionManager.currentCall.value
                     if (existingCall == null || existingCall.type != call.type || existingCall.rate != call.rate) {
                          callSessionManager.updateCall(call)
                     }
                     // Update Active State (e.g. video toggle)
                     val currentState = _uiState.value
                     if (currentState is CallUiState.Active) {
                         _uiState.value = currentState.copy(
                             videoRequestStatus = call.videoRequestStatus,
                             videoRequestBy = call.videoRequestBy,
                             type = call.type,
                             rate = call.rate,
                             // Keep existing otherUserId if set (by Agora event), otherwise might be 0
                             otherUserId = currentState.otherUserId,
                             currentUserId = currentUserId 
                         )
                     }
                }
            }
        }
    }

    private fun handleCallEnded(call: Call) {
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallViewModel",
            event = "handleCallEnded",
            effect = "Finalizing call and updating state to Ended",
            callId = callId
        )
        // Calculate final summary using final local billing manager values to prevent race conditions with Firestore settlement delay
        val durationSeconds = billingManager.durationSeconds.value
        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
        
        val earningsStr = if (isSpeaker) {
            val computedEarnings = billingManager.earnedCoins.value * com.rkdevstudios.voxly.util.CoinConstants.currentConfig.speakerRate
            String.format(java.util.Locale.getDefault(), "₹%.2f", computedEarnings)
        } else {
            billingManager.coinsSpent.value.toString()
        }

        _uiState.value = CallUiState.Ended(
            duration = durationStr,
            earnings = earningsStr,
            isSpeaker = isSpeaker,
            speakerId = call.speakerId // Pass speakerId
        )
        
        // Stop monitoring
        callListenerJob?.cancel()
        callSessionManager.leaveSession()
    }

    fun endCall() {
        viewModelScope.launch {
             val currentState = _uiState.value
             if (currentState is CallUiState.Active) {
                 try {
                     callRepository.updateCallStatus(callId, "ended")
                 } catch (e: Exception) {
                     android.util.Log.e("CallViewModel", "endCall: Failed to update call status to ended", e)
                 }
                 callSessionManager.leaveSession()
             }
        }
    }

    fun submitRating(rating: Int) {
        val currentState = _uiState.value
        if (currentState is CallUiState.Ended && !currentState.isSpeaker) {
            val speakerId = currentState.speakerId
            if (!speakerId.isNullOrEmpty()) {
                viewModelScope.launch {
                    userRepository.rateSpeaker(speakerId, rating)
                    _eventFlow.emit("Rating Submitted")
                }
            }
        }
    }

    private fun endCallLocally(reason: String) {
         viewModelScope.launch {
            _eventFlow.emit(reason)
            endCall()
         }
    }
    
    fun requestVideoSwitch() {
        viewModelScope.launch {
            val user = userRepository.currentUserFlow.value ?: return@launch
            if (!isSpeaker) {
                 // Caller must have enough coins for at least 1 minute of video
                 val availableCoins = user.coins - billingManager.coinsSpent.value
                 if (availableCoins < CoinConstants.VIDEO_COINS_PER_MIN) {
                     _eventFlow.emit("Low coins! Needs at least ${CoinConstants.VIDEO_COINS_PER_MIN} coins.")
                     return@launch
                 }
            } else {
                // Speaker checks caller's current balance
                try {
                    val callSnapshot = callRepository.listenToCall(callId).first()
                    if (callSnapshot != null) {
                        val callerDoc = userRepository.getUser(callSnapshot.callerId)
                        if (callerDoc != null) {
                             val available = callerDoc.coins - billingManager.coinsSpent.value
                             if (available < CoinConstants.VIDEO_COINS_PER_MIN) {
                                 _eventFlow.emit("${callSnapshot.callerName} has low balance")
                                 return@launch
                             }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallViewModel", "Failed to check caller balance", e)
                }
            }
            callRepository.sendVideoRequest(callId, currentUserId)
            // UI state will update via valid firestore listener
        }
    }

    fun acceptVideoSwitch() {
        viewModelScope.launch {
            val user = userRepository.currentUserFlow.value
            if (user != null && !isSpeaker) {
                 // Caller must have enough coins for at least 1 minute of video
                 val availableCoins = user.coins - billingManager.coinsSpent.value
                 if (availableCoins < CoinConstants.VIDEO_COINS_PER_MIN) {
                     _eventFlow.emit("Insufficient balance to accept video call (Needs ${CoinConstants.VIDEO_COINS_PER_MIN} coins)")
                     return@launch
                 }
            }
            callRepository.respondToVideoRequest(callId, true)
            // UI state will update via listener
            toggleVideo(true) // Ensure local video is enabled
        }
    }

    fun declineVideoSwitch() {
        viewModelScope.launch {
            callRepository.respondToVideoRequest(callId, false)
            // UI state will update via listener
        }
    }

    // Agora Wrappers
    fun toggleMic(mute: Boolean) = agoraManager.toggleMute(mute) // Fixed name
    fun toggleVideo(enable: Boolean) = agoraManager.toggleVideo(enable)
    fun toggleSpeaker(enable: Boolean) = agoraManager.toggleSpeaker(enable)
    fun switchCamera() = agoraManager.switchCamera()
    
    fun getAgoraManagerInstance(): AgoraManager = agoraManager

    override fun onCleared() {
        super.onCleared()
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallViewModel",
            event = "onCleared",
            effect = "ViewModel cleared, executing teardown",
            callId = callId
        )
        
        callSessionManager.leaveSession()
        callListenerJob?.cancel()
    }
}
