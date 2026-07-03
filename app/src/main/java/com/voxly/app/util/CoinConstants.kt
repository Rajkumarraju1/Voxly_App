package com.voxly.app.util

object CoinConstants {
    // Value of 1 coin in INR
    const val COIN_VALUE_INR = 0.75

    // Coins deducted per minute
    const val AUDIO_COINS_PER_MIN = 10
    const val VIDEO_COINS_PER_MIN = 30

    // Deduction Intervals (Seconds)
    // 10 coins/min = 1 coin every 6 seconds
    const val AUDIO_INTERVAL_SECONDS = 6
    
    // 30 coins/min = 1 coin every 2 seconds
    const val VIDEO_INTERVAL_SECONDS = 2

    // Revenue Split
    const val SPEAKER_SHARE = 0.30
    const val PLATFORM_SHARE = 0.70

    // Play Billing Product Mappings
    val PRODUCT_ID_TO_COINS = mapOf(
        "coins_80" to 80,
        "coins_300" to 300,
        "coins_450" to 450,
        "coins_1100" to 1100,
        "coins_1800" to 1800,
        "coins_3500" to 3500,
        "coins_5000" to 5000,
        "coins_9000" to 9000,
        "coins_20000" to 20000
    )

    val PRODUCT_ID_TO_PRICE_INR = mapOf(
        "coins_80" to 59,
        "coins_300" to 149,
        "coins_450" to 251,
        "coins_1100" to 550,
        "coins_1800" to 1055,
        "coins_3500" to 1599,
        "coins_5000" to 599,
        "coins_9000" to 2651,
        "coins_20000" to 5000
    )
}
