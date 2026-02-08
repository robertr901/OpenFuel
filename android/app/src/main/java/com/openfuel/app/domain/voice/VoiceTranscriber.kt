package com.openfuel.app.domain.voice

interface VoiceTranscriber {
    suspend fun transcribeOnce(config: VoiceTranscribeConfig): VoiceTranscribeResult
}

data class VoiceTranscribeConfig(
    val languageTag: String?,
    val maxDurationMs: Long,
)

sealed interface VoiceTranscribeResult {
    data class Success(val text: String) : VoiceTranscribeResult

    data class Unavailable(val reason: VoiceUnavailableReason) : VoiceTranscribeResult

    data object Cancelled : VoiceTranscribeResult

    /**
     * Sanitized, user-safe message only. Never include stack traces or raw system payloads.
     */
    data class Failure(val message: String) : VoiceTranscribeResult
}

enum class VoiceUnavailableReason {
    OFFLINE_PACK_MISSING,
    SERVICE_NOT_AVAILABLE,
    PERMISSION_DENIED,
    NOT_SUPPORTED,
    UNKNOWN,
}
