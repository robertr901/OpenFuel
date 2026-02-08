package com.openfuel.app.domain.service

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderExecutionModelsTest {
    @Test
    fun buildProviderDedupeKey_prefersNormalizedBarcodeWhenPresent() {
        val candidate = candidate(
            barcode = " 0123456789012 ",
            name = "Name ignored when barcode exists",
            brand = "Brand ignored",
            servingSize = "100 g",
        )

        val key = buildProviderDedupeKey(candidate)

        assertEquals("barcode:0123456789012", key)
    }

    @Test
    fun buildProviderDedupeKey_usesNameBrandServingWhenBarcodeMissing() {
        val candidate = candidate(
            barcode = null,
            name = "  Greek   Yogurt ",
            brand = " ACME ",
            servingSize = " 170 g ",
        )

        val key = buildProviderDedupeKey(candidate)

        assertEquals("text:greek yogurt|acme|170 g", key)
    }

    @Test
    fun buildProviderCacheKey_normalizesProviderAndInput() {
        val textKey = buildProviderCacheKey(
            providerId = "  Open_Food_Facts ",
            requestType = ProviderRequestType.TEXT_SEARCH,
            rawInput = "  Oat   Milk ",
        )
        val barcodeKey = buildProviderCacheKey(
            providerId = "Provider-A",
            requestType = ProviderRequestType.BARCODE_LOOKUP,
            rawInput = " 123456 ",
        )

        assertEquals("open_food_facts|TEXT_SEARCH|oat milk", textKey)
        assertEquals("provider-a|BARCODE_LOOKUP|123456", barcodeKey)
    }

    @Test
    fun providerExecutionPolicy_validatesTimeoutOrdering() {
        val exception = runCatching {
            ProviderExecutionPolicy(
                overallTimeout = java.time.Duration.ofSeconds(1),
                perProviderTimeout = java.time.Duration.ofSeconds(2),
            )
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    private fun candidate(
        barcode: String?,
        name: String,
        brand: String?,
        servingSize: String?,
    ): RemoteFoodCandidate {
        return RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = "id",
            barcode = barcode,
            name = name,
            brand = brand,
            caloriesKcalPer100g = null,
            proteinGPer100g = null,
            carbsGPer100g = null,
            fatGPer100g = null,
            servingSize = servingSize,
        )
    }
}
