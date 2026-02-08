package com.openfuel.app.data.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.openfuel.app.domain.voice.VoiceTranscribeConfig
import com.openfuel.app.domain.voice.VoiceTranscribeResult
import com.openfuel.app.domain.voice.VoiceTranscriber
import com.openfuel.app.domain.voice.VoiceUnavailableReason
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class RecognizerIntentVoiceTranscriber(
    private val currentActivityProvider: () -> ComponentActivity?,
) : VoiceTranscriber {
    override suspend fun transcribeOnce(config: VoiceTranscribeConfig): VoiceTranscribeResult {
        val maxDurationMs = config.maxDurationMs.coerceIn(1_000L, 10_000L)
        return withContext(Dispatchers.Main.immediate) {
            val activity = currentActivityProvider()
                ?: return@withContext VoiceTranscribeResult.Unavailable(
                    VoiceUnavailableReason.SERVICE_NOT_AVAILABLE,
                )
            if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
                return@withContext VoiceTranscribeResult.Unavailable(
                    VoiceUnavailableReason.SERVICE_NOT_AVAILABLE,
                )
            }
            withTimeoutOrNull(maxDurationMs) {
                launchRecognitionOnce(activity, config)
            } ?: VoiceTranscribeResult.Cancelled
        }
    }

    private suspend fun launchRecognitionOnce(
        activity: ComponentActivity,
        config: VoiceTranscribeConfig,
    ): VoiceTranscribeResult = suspendCancellableCoroutine { continuation ->
        val key = "voice_transcriber_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<Intent>? = null
        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            launcher?.unregister()
            if (!continuation.isActive) {
                return@register
            }
            continuation.resume(
                mapActivityResult(
                    resultCode = result.resultCode,
                    intent = result.data,
                ),
            )
        }
        continuation.invokeOnCancellation {
            launcher?.unregister()
        }
        try {
            launcher?.launch(buildRecognitionIntent(config, maxDurationMs = config.maxDurationMs))
        } catch (_: ActivityNotFoundException) {
            launcher?.unregister()
            if (continuation.isActive) {
                continuation.resume(
                    VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.SERVICE_NOT_AVAILABLE),
                )
            }
        } catch (_: SecurityException) {
            launcher?.unregister()
            if (continuation.isActive) {
                continuation.resume(
                    VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.PERMISSION_DENIED),
                )
            }
        } catch (_: UnsupportedOperationException) {
            launcher?.unregister()
            if (continuation.isActive) {
                continuation.resume(
                    VoiceTranscribeResult.Unavailable(VoiceUnavailableReason.NOT_SUPPORTED),
                )
            }
        } catch (_: Exception) {
            launcher?.unregister()
            if (continuation.isActive) {
                continuation.resume(
                    VoiceTranscribeResult.Failure("Voice input failed. Please try again."),
                )
            }
        }
    }

    private fun buildRecognitionIntent(
        config: VoiceTranscribeConfig,
        maxDurationMs: Long,
    ): Intent {
        val boundedDuration = maxDurationMs.coerceIn(1_000L, 10_000L)
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, boundedDuration)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                minOf(3_000L, boundedDuration),
            )
            config.languageTag?.takeIf { it.isNotBlank() }?.let { language ->
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            }
        }
    }

    private fun mapActivityResult(
        resultCode: Int,
        intent: Intent?,
    ): VoiceTranscribeResult {
        if (resultCode == Activity.RESULT_CANCELED) {
            return VoiceTranscribeResult.Cancelled
        }
        if (resultCode != Activity.RESULT_OK) {
            return VoiceTranscribeResult.Failure("Voice input failed. Please try again.")
        }
        val text = intent
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (text.isBlank()) {
            return VoiceTranscribeResult.Failure("No speech detected. Please try again.")
        }
        return VoiceTranscribeResult.Success(text)
    }
}
