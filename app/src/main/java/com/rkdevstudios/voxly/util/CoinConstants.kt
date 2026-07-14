package com.rkdevstudios.voxly.util

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.rkdevstudios.voxly.data.model.BillingConfig

object CoinConstants {
    const val AUDIO_COINS_PER_MIN = 10
    const val VIDEO_COINS_PER_MIN = 30

    // Deduction Intervals (Seconds)
    // 10 coins/min = 1 coin every 6 seconds
    const val AUDIO_INTERVAL_SECONDS = 6
    
    // 30 coins/min = 1 coin every 2 seconds
    const val VIDEO_INTERVAL_SECONDS = 2

    // Dynamic config holder with safe default fallbacks
    @Volatile
    var currentConfig = BillingConfig()
        private set

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadConfig(db: FirebaseFirestore) {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("system").document("billingConfig")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("CoinConstants", "Error listening to billingConfig: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val audio = snapshot.getLong("audioCoinsPerMinute")?.toInt() ?: 10
                    val video = snapshot.getLong("videoCoinsPerMinute")?.toInt() ?: 30
                    val speaker = snapshot.getDouble("speakerRate") ?: 0.225
                    val platform = snapshot.getDouble("platformRate") ?: 0.525
                    val version = snapshot.getLong("pricingVersion")?.toInt() ?: 1
                    
                    currentConfig = BillingConfig(
                        audioCoinsPerMinute = audio,
                        videoCoinsPerMinute = video,
                        speakerRate = speaker,
                        platformRate = platform,
                        pricingVersion = version
                    )
                    android.util.Log.d("CoinConstants", "Successfully synced billingConfig: $currentConfig")
                }
            }
    }

    // Play Billing Production Product Mappings
    val PRODUCT_ID_TO_COINS = mapOf(
        "coins_100" to 100,
        "coins_250" to 275,
        "coins_500" to 575,
        "coins_1000" to 1200,
        "coins_2500" to 3100,
        "coins_5000" to 6500
    )
}

object CallPricing {
    const val USER_VERIFICATION_TIMEOUT_MS = 5000L

    fun getRequiredMinimum(type: com.rkdevstudios.voxly.data.model.CallType): Double {
        return when (type) {
            com.rkdevstudios.voxly.data.model.CallType.VIDEO -> CoinConstants.VIDEO_COINS_PER_MIN.toDouble()
            com.rkdevstudios.voxly.data.model.CallType.AUDIO -> CoinConstants.AUDIO_COINS_PER_MIN.toDouble()
        }
    }
}
