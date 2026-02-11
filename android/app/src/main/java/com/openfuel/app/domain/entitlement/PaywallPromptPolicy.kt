package com.openfuel.app.domain.entitlement

enum class PaywallPromptSource {
    SESSION_LIMITED_UPSELL,
    GATED_FEATURE_ENTRY,
}

/**
 * Session-scoped prompt guardrail:
 * - generic upsell prompts are limited to once per session
 * - gated feature entry can always open paywall
 */
class PaywallPromptPolicy {
    private var sessionLimitedPromptShown: Boolean = false

    @Synchronized
    fun shouldShowPrompt(source: PaywallPromptSource): Boolean {
        return when (source) {
            PaywallPromptSource.GATED_FEATURE_ENTRY -> true
            PaywallPromptSource.SESSION_LIMITED_UPSELL -> {
                if (sessionLimitedPromptShown) {
                    false
                } else {
                    sessionLimitedPromptShown = true
                    true
                }
            }
        }
    }

    @Synchronized
    fun resetSessionForTesting() {
        sessionLimitedPromptShown = false
    }
}

