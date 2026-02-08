package com.openfuel.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

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
