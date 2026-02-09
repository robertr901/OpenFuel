package com.openfuel.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAvailabilityPolicyTest {
    @Test
    fun resolveOpenFoodFactsAvailability_whenDeterministicMode_disablesProvider() {
        val availability = resolveOpenFoodFactsAvailability(
            forceDeterministicProvidersOnly = true,
            providerEnabledByFlag = true,
        )

        assertFalse(availability.enabled)
        assertEquals("Disabled in deterministic test mode.", availability.statusReason)
    }

    @Test
    fun resolveOpenFoodFactsAvailability_whenFlagDisabled_disablesProvider() {
        val availability = resolveOpenFoodFactsAvailability(
            forceDeterministicProvidersOnly = false,
            providerEnabledByFlag = false,
        )

        assertFalse(availability.enabled)
        assertEquals("Disabled by local provider flag.", availability.statusReason)
    }

    @Test
    fun resolveUsdaAvailability_whenApiKeyMissing_returnsNeedsSetupReason() {
        val availability = resolveUsdaAvailability(
            forceDeterministicProvidersOnly = false,
            providerEnabledByFlag = true,
            apiKey = "",
        )

        assertFalse(availability.enabled)
        assertEquals("USDA API key missing. Add USDA_API_KEY in local.properties.", availability.statusReason)
    }

    @Test
    fun resolveUsdaAvailability_whenEnabledAndKeyPresent_returnsConfigured() {
        val availability = resolveUsdaAvailability(
            forceDeterministicProvidersOnly = false,
            providerEnabledByFlag = true,
            apiKey = "demo-key",
        )

        assertTrue(availability.enabled)
        assertEquals("Configured.", availability.statusReason)
    }
}
