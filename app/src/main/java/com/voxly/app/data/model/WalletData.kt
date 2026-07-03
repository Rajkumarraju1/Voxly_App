package com.voxly.app.data.model

data class CoinPack(
    val coins: Int,
    val price: Int,
    val valuePerCoin: Double,
    val tag: String? = null,
    val discountText: String? = null,
    val category: PackCategory
)

enum class PackCategory {
    SMALL, MEDIUM, LARGE, MEGA
}
