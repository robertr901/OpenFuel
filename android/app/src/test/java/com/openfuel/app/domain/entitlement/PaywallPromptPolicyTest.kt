package com.openfuel.app.domain.entitlement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallPromptPolicyTest {
    @Test
    fun sessionLimitedUpsell_allowsOnlyFirstPromptPerSession() {
        val policy = PaywallPromptPolicy()

        assertTrue(policy.shouldShowPrompt(PaywallPromptSource.SESSION_LIMITED_UPSELL))
        assertFalse(policy.shouldShowPrompt(PaywallPromptSource.SESSION_LIMITED_UPSELL))
    }

    @Test
    fun gatedFeatureEntry_alwaysAllowedEvenAfterSessionLimitReached() {
        val policy = PaywallPromptPolicy()

        assertTrue(policy.shouldShowPrompt(PaywallPromptSource.SESSION_LIMITED_UPSELL))
        assertFalse(policy.shouldShowPrompt(PaywallPromptSource.SESSION_LIMITED_UPSELL))

        assertTrue(policy.shouldShowPrompt(PaywallPromptSource.GATED_FEATURE_ENTRY))
        assertTrue(policy.shouldShowPrompt(PaywallPromptSource.GATED_FEATURE_ENTRY))
    }
}

