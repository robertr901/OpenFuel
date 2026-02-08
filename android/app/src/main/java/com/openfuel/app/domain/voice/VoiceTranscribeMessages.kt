package com.openfuel.app.domain.voice

fun VoiceTranscribeResult.messageOrNull(): String? {
    return when (this) {
        is VoiceTranscribeResult.Success -> null
        is VoiceTranscribeResult.Cancelled -> "Voice input canceled."
        is VoiceTranscribeResult.Failure -> message
        is VoiceTranscribeResult.Unavailable -> reason.message()
    }
}

fun VoiceUnavailableReason.message(): String {
    return when (this) {
        VoiceUnavailableReason.OFFLINE_PACK_MISSING -> "Offline speech language pack is missing."
        VoiceUnavailableReason.SERVICE_NOT_AVAILABLE -> "Speech service is not available on this device."
        VoiceUnavailableReason.PERMISSION_DENIED -> "Microphone permission is required for voice input."
        VoiceUnavailableReason.NOT_SUPPORTED -> "Voice input is not supported on this device."
        VoiceUnavailableReason.UNKNOWN -> "Voice input is unavailable right now."
    }
}
