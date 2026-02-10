package com.openfuel.sharedcore.normalization

import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreProviderNormalizationTest {
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
    fun buildProviderDedupeKey_missingBrandAndServingFallsBackToSourceIdentity() {
        val candidate = candidate(
            sourceId = "off-123",
            barcode = null,
            name = "Protein Bar",
            brand = null,
            servingSize = null,
        )

        val key = buildProviderDedupeKey(candidate)

        assertEquals("source:open_food_facts|off-123|protein bar", key)
    }

    @Test
    fun buildProviderCacheKey_normalizesProviderAndInput() {
        val textKey = buildProviderCacheKey(
            providerId = "  Open_Food_Facts ",
            requestType = CoreProviderRequestType.TEXT_SEARCH,
            rawInput = "  Oat   Milk ",
        )
        val barcodeKey = buildProviderCacheKey(
            providerId = "Provider-A",
            requestType = CoreProviderRequestType.BARCODE_LOOKUP,
            rawInput = " 123456 ",
        )

        assertEquals("open_food_facts|TEXT_SEARCH|oat milk", textKey)
        assertEquals("provider-a|BARCODE_LOOKUP|123456", barcodeKey)
    }

    private fun candidate(
        sourceId: String = "id",
        barcode: String?,
        name: String,
        brand: String?,
        servingSize: String?,
    ): CoreRemoteFoodCandidate {
        return CoreRemoteFoodCandidate(
            source = "OPEN_FOOD_FACTS",
            sourceId = sourceId,
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
