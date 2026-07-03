package com.voxly.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val coins: Int = 0,
    val description: String = "",
    val status: String = "pending", // success, failed, pending
    @ServerTimestamp
    val timestamp: Date? = null,

    @ServerTimestamp
    val approvedAt: Date? = null,
    
    // Denormalization for History Views (Avoids extra reads)
    val relatedUserId: String = "",
    val relatedUserName: String = "",
    val relatedUserAvatar: String = ""
)
