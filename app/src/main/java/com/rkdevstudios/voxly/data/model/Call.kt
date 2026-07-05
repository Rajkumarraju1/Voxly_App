package com.rkdevstudios.voxly.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Call(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val speakerId: String = "",
    val speakerName: String = "",
    val speakerAvatar: String = "",
    val type: String = "audio",
    val status: String = "ringing",
    val channelId: String = "",
    val rate: Int = 0,
    val duration: Int = 0, // Duration in seconds
    val videoRequestStatus: String? = null, // "pending", "accepted", "declined"
    val videoRequestBy: String? = null, // userId of requester
    val coinsDeducted: Int = 0,
    val userPaid: Double = 0.0,
    val speakerEarned: Double = 0.0,
    val platformEarned: Double = 0.0,
    @ServerTimestamp val createdAt: Date? = null,
    val participants: List<String> = emptyList(), // [callerId, speakerId] for efficient querying
    val expireAt: Date? = null, // For TTL
    val speakerFcmToken: String? = null, // Optimization: Pass token to avoid extra read in Cloud Function
    val version: Int = 1
)
