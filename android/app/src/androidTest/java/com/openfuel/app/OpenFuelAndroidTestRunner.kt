package com.openfuel.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.openfuel.app.domain.voice.VoiceTranscribeConfig
import com.openfuel.app.domain.voice.VoiceTranscribeResult
import com.openfuel.app.domain.voice.VoiceTranscriber

class OpenFuelAndroidTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context,
    ): Application {
        OpenFuelApp.containerFactoryOverride = { appContext ->
            AppContainer(
                context = appContext,
                forceDeterministicProvidersOnly = true,
                voiceTranscriberOverride = object : VoiceTranscriber {
                    override suspend fun transcribeOnce(config: VoiceTranscribeConfig): VoiceTranscribeResult {
                        return VoiceTranscribeResult.Success("2 eggs and banana")
                    }
                },
            )
        }
        return super.newApplication(
            cl,
            OpenFuelApp::class.java.name,
            context,
        )
    }

    override fun finish(resultCode: Int, results: android.os.Bundle?) {
        OpenFuelApp.containerFactoryOverride = null
        super.finish(resultCode, results)
    }
}
