package com.openfuel.app.domain.voice

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceTranscriberTest {
    @Test
    fun fakeVoiceTranscriber_returnsSuccess() = runBlocking {
        val fake = FakeVoiceTranscriber()
        fake.enqueue(VoiceTranscribeResult.Success("2 eggs and banana"))

        val result = fake.transcribeOnce(VoiceTranscribeConfig(languageTag = "en-US", maxDurationMs = 10_000))

        assertEquals(VoiceTranscribeResult.Success("2 eggs and banana"), result)
    }

    @Test
    fun fakeVoiceTranscriber_returnsUnavailable() = runBlocking {
        val fake = FakeVoiceTranscriber()
        fake.enqueue(VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.SERVICE_NOT_AVAILABLE))

        val result = fake.transcribeOnce(VoiceTranscribeConfig(languageTag = null, maxDurationMs = 10_000))

        assertEquals(
            VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.SERVICE_NOT_AVAILABLE),
            result,
        )
    }

    @Test
    fun fakeVoiceTranscriber_returnsCancelled() = runBlocking {
        val fake = FakeVoiceTranscriber()
        fake.enqueue(VoiceTranscribeResult.Cancelled)

        val result = fake.transcribeOnce(VoiceTranscribeConfig(languageTag = null, maxDurationMs = 10_000))

        assertEquals(VoiceTranscribeResult.Cancelled, result)
    }

    @Test
    fun fakeVoiceTranscriber_returnsFailure() = runBlocking {
        val fake = FakeVoiceTranscriber()
        fake.enqueue(VoiceTranscribeResult.Failure("Speech unavailable right now."))

        val result = fake.transcribeOnce(VoiceTranscribeConfig(languageTag = "en-US", maxDurationMs = 10_000))

        assertEquals(VoiceTranscribeResult.Failure("Speech unavailable right now."), result)
    }

    @Test
    fun voiceTranscribeMessages_mapToSafeCopy() {
        assertNull(VoiceTranscribeResult.Success("banana").messageOrNull())
        assertEquals("Voice input canceled.", VoiceTranscribeResult.Cancelled.messageOrNull())
        assertEquals("Try again soon.", VoiceTranscribeResult.Failure("Try again soon.").messageOrNull())
        assertEquals(
            "Speech service is not available on this device.",
            VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.SERVICE_NOT_AVAILABLE).messageOrNull(),
        )
        assertEquals(
            "Offline speech language pack is missing.",
            VoiceUnavailableReason.OFFLINE_PACK_MISSING.message(),
        )
    }
}
