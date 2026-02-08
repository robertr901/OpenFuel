package com.openfuel.app.domain.voice

class FakeVoiceTranscriber : VoiceTranscriber {
    private val queuedResults = ArrayDeque<VoiceTranscribeResult>()

    fun enqueue(result: VoiceTranscribeResult) {
        queuedResults.addLast(result)
    }

    override suspend fun transcribeOnce(config: VoiceTranscribeConfig): VoiceTranscribeResult {
        return queuedResults.removeFirstOrNull()
            ?: VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.UNKNOWN)
    }
}
