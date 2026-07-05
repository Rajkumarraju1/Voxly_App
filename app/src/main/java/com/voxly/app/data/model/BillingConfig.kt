package com.voxly.app.data.model

data class BillingConfig(
    val audioCoinsPerMinute: Int = 10,
    val videoCoinsPerMinute: Int = 30,
    val speakerRate: Double = 0.225,
    val platformRate: Double = 0.525,
    val pricingVersion: Int = 1
)
