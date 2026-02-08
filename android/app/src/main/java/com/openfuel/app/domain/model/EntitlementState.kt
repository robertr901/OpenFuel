package com.openfuel.app.domain.model

enum class EntitlementSource {
    DEBUG_OVERRIDE,
    PLAY_BILLING,
}

data class EntitlementState(
    val isPro: Boolean,
    val source: EntitlementSource,
    val canToggleDebugOverride: Boolean,
    val securityPosture: SecurityPosture = SecurityPosture.secure(),
)
