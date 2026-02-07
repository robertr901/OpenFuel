package com.openfuel.app.data.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.openfuel.app.BuildConfig
import com.openfuel.app.domain.model.SecurityPosture
import com.openfuel.app.domain.security.SecurityPostureProvider
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LocalSecurityPostureProvider(
    context: Context,
) : SecurityPostureProvider {
    private val postureState = MutableStateFlow(computePosture(context))

    override fun posture(): Flow<SecurityPosture> = postureState

    private fun computePosture(context: Context): SecurityPosture {
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val fingerprint = Build.FINGERPRINT.orEmpty().lowercase(Locale.US)
        val model = Build.MODEL.orEmpty().lowercase(Locale.US)
        val product = Build.PRODUCT.orEmpty().lowercase(Locale.US)
        val tags = Build.TAGS.orEmpty().lowercase(Locale.US)

        val isEmulator = fingerprint.contains("generic") ||
            model.contains("emulator") ||
            product.contains("sdk")
        val hasTestKeys = tags.contains("test-keys")
        val isLikelyTampered = !BuildConfig.DEBUG && (isDebuggable || hasTestKeys)

        return SecurityPosture(
            isDebuggable = isDebuggable,
            isEmulator = isEmulator,
            hasTestKeys = hasTestKeys,
            isLikelyTampered = isLikelyTampered,
        )
    }
}
