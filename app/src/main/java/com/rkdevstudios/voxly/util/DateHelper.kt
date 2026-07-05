package com.rkdevstudios.voxly.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {
    fun formatHistoryDate(timestamp: Date?): String {
        if (timestamp == null) return "Unknown"
        return getDateHeader(timestamp.time)
    }

    fun getDateHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val callTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, callTime) -> "Today"
            isYesterday(now, callTime) -> "Yesterday"
            else -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(callTime.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, callTime: Calendar): Boolean {
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, callTime)
    }
}
