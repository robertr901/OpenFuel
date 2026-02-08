package com.openfuel.app

import android.app.Application
import android.content.Context

class OpenFuelApp : Application() {
    companion object {
        @Volatile
        var containerFactoryOverride: ((Context) -> AppContainer)? = null
    }

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = containerFactoryOverride?.invoke(this) ?: AppContainer(this)
    }
}
