package com.rkdevstudios.voxly.data.session

import kotlinx.coroutines.flow.StateFlow

data class BillingSegment(
    val startElapsedRealtimeMs: Long,
    val endElapsedRealtimeMs: Long,
    val type: com.rkdevstudios.voxly.data.model.CallType,
    val rate: Double
)

data class BillingSnapshot(
    val elapsedSeconds: Long = 0L,
    val callerCoinsSpent: Int = 0,
    val speakerCoinsEarned: Double = 0.0,
    val activeRate: Double = 0.0,
    val activeCallType: com.rkdevstudios.voxly.data.model.CallType = com.rkdevstudios.voxly.data.model.CallType.AUDIO
)

interface BillingManager {
    val durationSeconds: StateFlow<Long>
    val earnedCoins: StateFlow<Double>
    val coinsSpent: StateFlow<Int>
    val remainingSeconds: StateFlow<Long>
    val billingSnapshot: StateFlow<BillingSnapshot>
}
