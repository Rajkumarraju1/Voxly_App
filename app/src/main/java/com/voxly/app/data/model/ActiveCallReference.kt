package com.voxly.app.data.model

data class ActiveCallReference(
    val callId: String = "",
    val role: CallRole = CallRole.CALLER,
    val updatedAt: Long = 0L,
    val status: String = "ringing"
)
