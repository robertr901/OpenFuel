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

    @Test
    fun resolveUsdaAvailability_whenDeterministicModeAndKeyPresent_disablesProvider() {
        val availability = resolveUsdaAvailability(
            forceDeterministicProvidersOnly = true,
            providerEnabledByFlag = true,
            apiKey = "demo-key",
        )

        assertFalse(availability.enabled)
        assertEquals("Disabled in deterministic test mode.", availability.statusReason)
    }

    @Test
    fun resolveNutritionixAvailability_whenCredentialsMissing_returnsNeedsSetupReason() {
        val availability = resolveNutritionixAvailability(
            forceDeterministicProvidersOnly = false,
            providerEnabledByFlag = true,
            appId = "",
            apiKey = "",
        )

        assertFalse(availability.enabled)
        assertEquals(
            "Nutritionix credentials missing. Add NUTRITIONIX_APP_ID and NUTRITIONIX_API_KEY in local.properties.",
            availability.statusReason,
        )
    }

    @Test
    fun resolveNutritionixAvailability_whenEnabledAndConfigured_returnsConfigured() {
        val availability = resolveNutritionixAvailability(
            forceDeterministicProvidersOnly = false,
            providerEnabledByFlag = true,
            appId = "demo-app-id",
            apiKey = "demo-app-key",
        )

        assertTrue(availability.enabled)
        assertEquals("Configured.", availability.statusReason)
    }

    @Test
    fun resolveNutritionixAvailability_whenFlagDisabled_returnsDisabledReason() {
        val availability = resolveNutritionixAvailability(
            forceDeterministicProvidersOnly = false,
            providerEnabledByFlag = false,
            appId = "demo-app-id",
            apiKey = "demo-app-key",
        )

        assertFalse(availability.enabled)
        assertEquals("Disabled by local provider flag.", availability.statusReason)
    }

    @Test
    fun resolveNutritionixAvailability_whenDeterministicModeAndConfigured_disablesProvider() {
        val availability = resolveNutritionixAvailability(
            forceDeterministicProvidersOnly = true,
            providerEnabledByFlag = true,
            appId = "demo-app-id",
            apiKey = "demo-app-key",
        )

        assertFalse(availability.enabled)
        assertEquals("Disabled in deterministic test mode.", availability.statusReason)
    }
}
