package com.rkdevstudios.voxly.data.model

enum class SpeakerStatus {
    OFFLINE,
    AVAILABLE,
    UNAVAILABLE,      // Online, Idle, but supportedCallTypes is empty
    RINGING,
    CONNECTING,
    IN_CALL
}

data class SpeakerAvailabilityUi(
    val status: SpeakerStatus,
    val audioEnabled: Boolean,
    val videoEnabled: Boolean
)

fun User.deriveAvailability(currentTime: Long = System.currentTimeMillis()): SpeakerAvailabilityUi {
    val isStale = this.lastSeen > 0L && (currentTime - this.lastSeen) > 120000L
    val isOnline = this.isOnline && !isStale

    if (!isOnline) {
        return SpeakerAvailabilityUi(
            status = SpeakerStatus.OFFLINE,
            audioEnabled = false,
            videoEnabled = false
        )
    }

    val activeCall = this.activeCall
    if (activeCall != null && activeCall.callId.isNotEmpty()) {
        val status = when (activeCall.status.lowercase()) {
            "ringing" -> SpeakerStatus.RINGING
            "connecting" -> SpeakerStatus.CONNECTING
            else -> SpeakerStatus.IN_CALL
        }
        return SpeakerAvailabilityUi(
            status = status,
            audioEnabled = false,
            videoEnabled = false
        )
    }

    val hasAudio = this.callPreferences.getTypedSupportedCallTypes().contains(SupportedCallType.AUDIO)
    val hasVideo = this.callPreferences.getTypedSupportedCallTypes().contains(SupportedCallType.VIDEO)

    val status = if (!hasAudio && !hasVideo) SpeakerStatus.UNAVAILABLE else SpeakerStatus.AVAILABLE

    return SpeakerAvailabilityUi(
        status = status,
        audioEnabled = hasAudio,
        videoEnabled = hasVideo
    )
}
