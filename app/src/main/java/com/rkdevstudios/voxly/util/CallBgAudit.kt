package com.rkdevstudios.voxly.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallBgAudit {
    const val CALL_BG_AUDIT_ENABLED = true
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun log(component: String, event: String, effect: String, callId: String? = null) {
        if (!CALL_BG_AUDIT_ENABLED) return
        val timestamp = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val cid = callId ?: "N/A"
        Log.d("CALL_BG_AUDIT", "time=$timestamp | thread=$threadName | callId=$cid | component=$component | event=$event | effect=$effect")
    }
}
