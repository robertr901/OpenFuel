package com.openfuel.app.domain.analytics

enum class ProductEventName(val wireName: String) {
    RETENTION_FASTLOG_REMINDER_SHOWN("retention_fastlog_reminder_shown"),
    RETENTION_FASTLOG_REMINDER_DISMISSED("retention_fastlog_reminder_dismissed"),
    RETENTION_FASTLOG_REMINDER_ACTED("retention_fastlog_reminder_acted"),
    LOGGING_STARTED("logging_started"),
    LOGGING_COMPLETED("logging_completed"),
    ACTIVATION_REACHED("activation_reached"),
    AHA_REACHED("aha_reached"),
    PAYWALL_PROMPT_SHOWN("paywall_prompt_shown"),
    PAYWALL_UPGRADE_TAPPED("paywall_upgrade_tapped"),
    PAYWALL_RESTORE_TAPPED("paywall_restore_tapped"),
    PAYWALL_CANCELLED("paywall_cancelled"),
}

data class ProductEvent(
    val name: ProductEventName,
    val properties: Map<String, String>,
    val occurredAtEpochMs: Long,
)

