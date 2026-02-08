package com.openfuel.app.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteFoodCandidateMappersTest {
    @Test
    fun toLocalFoodItem_trimsAndAppliesSafeDefaults() {
        val candidate = RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = "off-1",
            barcode = " 123456 ",
            name = "   ",
            brand = "  ",
            caloriesKcalPer100g = null,
            proteinGPer100g = null,
            carbsGPer100g = null,
            fatGPer100g = null,
            servingSize = null,
        )

        val mapped = candidate.toLocalFoodItem(
            id = "saved-1",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

        assertEquals("saved-1", mapped.id)
        assertEquals("Imported food", mapped.name)
        assertNull(mapped.brand)
        assertEquals("123456", mapped.barcode)
        assertEquals(0.0, mapped.caloriesKcal, 0.0)
        assertEquals(0.0, mapped.proteinG, 0.0)
        assertEquals(0.0, mapped.carbsG, 0.0)
        assertEquals(0.0, mapped.fatG, 0.0)
    }

    @Test
    fun toLocalFoodItem_clampsOutOfRangeNutrients() {
        val candidate = RemoteFoodCandidate(
            source = RemoteFoodSource.STATIC_SAMPLE,
            sourceId = "sample-1",
            barcode = null,
            name = "Sample",
            brand = "Brand",
            caloriesKcalPer100g = 99_999.0,
            proteinGPer100g = 5_000.0,
            carbsGPer100g = 4_000.0,
            fatGPer100g = 3_000.0,
            servingSize = "100 g",
        )

        val mapped = candidate.toLocalFoodItem(
            id = "saved-2",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

        assertEquals(10_000.0, mapped.caloriesKcal, 0.0)
        assertEquals(1_000.0, mapped.proteinG, 0.0)
        assertEquals(1_000.0, mapped.carbsG, 0.0)
        assertEquals(1_000.0, mapped.fatG, 0.0)
    }
}
