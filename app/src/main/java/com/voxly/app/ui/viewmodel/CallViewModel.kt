package com.voxly.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxly.app.data.agora.AgoraManager
import com.voxly.app.data.model.Call
import com.voxly.app.data.repository.CallRepository
import com.voxly.app.data.repository.UserRepository
import com.voxly.app.util.CoinConstants
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
    private val agoraManager: AgoraManager
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

    // Events (Toasts, Navigation)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var callListenerJob: Job? = null
    private var timerJob: Job? = null
    
    // Internal tracking
    private var coinsSpent = 0
    private var audioTickCounter = 0
    private var videoTickCounter = 0
    private var isSpeaker = false
    private var currentUserId = ""

    private var isInitialized = false

    init {
        // initializeCall() // REMOVED: Called explicitly from Activity after permissions
    }

    fun startCall() {
        if (isInitialized) {
            android.util.Log.d("CallViewModel", "startCall: Already initialized. Skipping.")
            return
        }
        isInitialized = true
        
        viewModelScope.launch {
            try {
                android.util.Log.d("CallViewModel", "startCall: Starting initialization")
                val user = userRepository.currentUserFlow.value
                currentUserId = user?.id ?: ""
                isSpeaker = user?.isSpeaker == true
                android.util.Log.d("CallViewModel", "startCall: User info loaded. ID=$currentUserId, isSpeaker=$isSpeaker")

                // Initialize Agora
                try {
                     agoraManager.initialize()
                     android.util.Log.d("CallViewModel", "startCall: Agora initialized")
                } catch (e: Exception) {
                     android.util.Log.e("CallViewModel", "startCall: Agora Init Failed", e)
                }

                val channelName = "channel_$callId"
                // Generate an explicit positive integer UID (from 1 to 2^31-1)
             // Using 0 is valid for wildcard tokens, but generating an explicit UID prevents mapping/routing issues
             // across different devices if the SDK gets confused.
             val uid = (java.util.UUID.randomUUID().hashCode() and 0x7FFFFFFF)
             
             android.util.Log.d("CallViewModel", "Joining Agora Channel: $channelName with explicit UID: $uid")
             agoraManager.joinChannel(channelName, uid)

                // Fail-safe: Ensure call is marked as accepted if we are the receiver
                val isReceiver = currentUserId != _callerId
                if (isReceiver) {
                    callRepository.updateCallStatus(callId, "accepted")
                }

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
                
                // Start Timer
                startCallTimer()
            } catch (e: Exception) {
                android.util.Log.e("CallViewModel", "startCall: Critical Error", e)
                _eventFlow.emit("Call Error: ${e.message}")
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

    private fun startCallTimer() {
        timerJob?.cancel()
        coinsSpent = 0
        audioTickCounter = 0
        videoTickCounter = 0
        _liveCoins.value = 0.0
        
        // We need start time. If we have it in state, use it. Else now.
        val startTime = (_uiState.value as? CallUiState.Active)?.startTime ?: System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _uiState.value
                if (currentState !is CallUiState.Active) break

                val isVideo = currentState.type == "video"
                val currentRate = if (isVideo) CoinConstants.VIDEO_COINS_PER_MIN else currentState.rate

                // Earnings Visualization
                val coinsPerSecond = currentRate / 60.0
                _liveCoins.value += coinsPerSecond

                // Remaining Time (for caller)
                // Note: We need user coins. Assuming we have them or fetched them.
                // For simplicity here, we focus on earnings. 
                // To do proper remaining time, we'd need to observe User flow again.
                // Let's implement basic countdown if not speaker.
                 val user = userRepository.currentUserFlow.value
                 if (user != null && !isSpeaker) {
                     if (coinsPerSecond > 0) {
                         val maxSeconds = (user.coins / coinsPerSecond).toLong()
                         val elapsed = (System.currentTimeMillis() - startTime) / 1000
                         val left = maxSeconds - elapsed
                         _remainingSeconds.value = if (left > 0) left else 0
                         
                         if (left <= 0) {
                             endCallLocally("Insufficient balance")
                             break
                         }
                     }
                 } else {
                     _remainingSeconds.value = -1
                 }

                // Logic for Billing (Integer Coins)
                if (isVideo) {
                     videoTickCounter++
                     if (videoTickCounter >= CoinConstants.VIDEO_INTERVAL_SECONDS) {
                         coinsSpent += 1
                         videoTickCounter = 0
                     }
                     audioTickCounter = 0
                } else {
                    audioTickCounter++
                    if (audioTickCounter >= CoinConstants.AUDIO_INTERVAL_SECONDS) {
                        coinsSpent += 1
                        audioTickCounter = 0
                    }
                    videoTickCounter = 0
                }
            }
        }
    }

    private fun handleCallEnded(call: Call) {
        // Calculate final summary
        // We use the Repo's duration/earnings if available, or local est.
        val durationSeconds = call.duration
        val earnings = if (isSpeaker) call.speakerEarned else call.userPaid
        
        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
        val earningsStr = String.format(Locale.getDefault(), "₹%.2f", earnings)

        _uiState.value = CallUiState.Ended(
            duration = durationStr,
            earnings = earningsStr,
            isSpeaker = isSpeaker,
            speakerId = call.speakerId // Pass speakerId
        )
        
        // Stop everything
        timerJob?.cancel()
        callListenerJob?.cancel()
        agoraManager.leaveChannel()
    }

    fun endCall() {
        viewModelScope.launch {
            // Optimistic local end
            // Call Repo to actually end it
             val currentState = _uiState.value
             if (currentState is CallUiState.Active) {
                 val durationSeconds = ((System.currentTimeMillis() - currentState.startTime) / 1000).toInt()
                 callRepository.endCallSession(
                     callId = callId,
                     durationSeconds = durationSeconds,
                     coinsDeducted = coinsSpent
                 )
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
                val availableCoins = user.coins - coinsSpent
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
                            val available = callerDoc.coins - coinsSpent
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
                val availableCoins = user.coins - coinsSpent
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
        android.util.Log.d("CallViewModel", "onCleared: ViewModel Cleared. Leaving Channel.")
        
        // Ensure remote user is notified if we disappear without ending properly
        val currentState = _uiState.value
        if (currentState is CallUiState.Active) {
            val durationSeconds = ((System.currentTimeMillis() - currentState.startTime) / 1000).toInt()
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    callRepository.endCallSession(
                        callId = currentState.callId,
                        durationSeconds = durationSeconds,
                        coinsDeducted = coinsSpent
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CallViewModel", "onCleared: Failed to end call on backend", e)
                }
            }
        }
        
        agoraManager.leaveChannel()
        timerJob?.cancel()
        callListenerJob?.cancel()
    }
}
