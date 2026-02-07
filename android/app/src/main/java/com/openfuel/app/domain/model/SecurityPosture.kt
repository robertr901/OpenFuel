package com.openfuel.app.domain.model

data class SecurityPosture(
    val isDebuggable: Boolean,
    val isEmulator: Boolean,
    val hasTestKeys: Boolean,
    val isLikelyTampered: Boolean,
) {
    companion object {
        fun secure(): SecurityPosture {
            return SecurityPosture(
                isDebuggable = false,
                isEmulator = false,
                hasTestKeys = false,
                isLikelyTampered = false,
            )
        }
    }
}
