package com.voxly.app.util

import kotlin.math.roundToLong

object CoinFormatter {
    fun format(value: Number?): String {
        if (value == null) return "0"
        return value.toDouble().roundToLong().toString()
    }
}
