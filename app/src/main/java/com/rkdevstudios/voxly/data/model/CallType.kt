package com.rkdevstudios.voxly.data.model

enum class CallType {
    AUDIO,
    VIDEO
}

fun CallType.toSupportedCallType(): SupportedCallType {
    return when (this) {
        CallType.AUDIO -> SupportedCallType.AUDIO
        CallType.VIDEO -> SupportedCallType.VIDEO
    }
}
