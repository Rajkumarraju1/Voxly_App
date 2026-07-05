package com.rkdevstudios.voxly.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

@IgnoreExtraProperties
data class User(
    val id: String = "",

    val languages: List<String> = emptyList(),
    val gender: String = "",
    val avatarUrl: String = "",
    val displayName: String = "",
    val tags: List<String> = emptyList(),
    val tagline: String = "",
    val phoneNumber: String = "",
    val verificationStatus: String = "none", // none, pending, verified, rejected
    val coins: Double = 0.0,
    // Speaker Fields
    @get:PropertyName("speaker") @set:PropertyName("speaker") @field:PropertyName("speaker") var isSpeaker: Boolean = false,
    @get:PropertyName("online") @set:PropertyName("online") @field:PropertyName("online") var isOnline: Boolean = false,
    @get:PropertyName("busy") @set:PropertyName("busy") @field:PropertyName("busy") var isBusy: Boolean = false,
    @get:PropertyName("presence") @set:PropertyName("presence") @field:PropertyName("presence") var presence: String = "OFFLINE",
    val downgradeRequest: Boolean = false, // Added field
    val blockedUsers: List<String> = emptyList(), // Added field
    val audioRate: Int = 10,
    val videoRate: Int = 30,
    val earnings: Double = 0.0,
    val upiId: String = "",
    val bankAccountNumber: String = "",
    val ifscCode: String = "",
    val bankName: String = "",
    val paymentMethod: String = "", // "UPI" or "Bank"
    // Rating Fields
    val ratingSum: Double = 0.0,
    val ratingCount: Int = 0,
    // Heartbeat
    val lastSeen: Long = 0L,
    val fcmToken: String = "", // Added field for notification optimization
    val activeCall: ActiveCallReference? = null,
    val callPreferences: CallPreferences = CallPreferences()
) {
    @get:PropertyName("isOnline")
    val isOnlineField: Boolean
        get() = isOnline
}

enum class SupportedCallType {
    AUDIO,
    VIDEO
}

enum class IncomingCallPolicy {
    ACCEPT,
    REJECT,
    PREMIUM_ONLY,
    CONTACTS_ONLY
}

@IgnoreExtraProperties
data class CallPreferences(
    val supportedCallTypes: List<String> = listOf("AUDIO", "VIDEO"), // Firebase friendly serialization
    val incomingCallPolicy: String = "ACCEPT",
    val autoAccept: Boolean = false,
    val dndMode: Boolean = false,
    val workingHoursStart: String = "",
    val workingHoursEnd: String = "",
    val premiumOnly: Boolean = false
) {
    // Helper to get typed set
    @Exclude
    fun getTypedSupportedCallTypes(): Set<SupportedCallType> {
        return supportedCallTypes.mapNotNull {
            try { SupportedCallType.valueOf(it.uppercase()) } catch (e: Exception) { null }
        }.toSet()
    }
}
