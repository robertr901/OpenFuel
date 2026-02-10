package com.openfuel.sharedcore.normalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreServingNormalizationTest {
    @Test
    fun normalizeServingText_handlesGoldenCorpusDeterministically() {
        val corpus = listOf(
            "330ml" to "330 ml",
            "330 ml" to "330 ml",
            " 330 ml " to "330 ml",
            "0.5l" to "0.5 l",
            "0.5 l" to "0.5 l",
            "1 bottle (500ml)" to "1 bottle (500 ml)",
            "1 can ( 330  ml )" to "1 can (330 ml)",
            "1/2 cup (55g)" to "1/2 cup (55 g)",
            "2 biscuits" to "2 biscuits",
            "per 100g" to "per 100 g",
            "1 bar (40 g)" to "1 bar (40 g)",
            "12 ounces" to "12 oz",
            "2 lbs" to "2 lb",
            "100 grams" to "100 g",
            "250 millilitres" to "250 ml",
            "1 litre" to "1 l",
            "1   bottle( 500 ml )" to "1 bottle (500 ml)",
            "  " to null,
            null to null,
        )

        corpus.forEach { (input, expected) ->
            assertEquals("input=$input", expected, normalizeServingText(input))
            assertEquals("deterministic input=$input", expected, normalizeServingText(input))
        }
    }

    @Test
    fun buildServingText_formatsWithStableFallbacks() {
        assertEquals("1 cup (200 g)", buildServingText(1.0, "cup", 200.0))
        assertEquals("40 g", buildServingText(40.0, "g", 40.0))
        assertEquals("250 g", buildServingText(null, null, 250.0))
        assertEquals("2 biscuits", buildServingText(2.0, "biscuits", null))
        assertEquals("ml", buildServingText(null, "ml", null))
        assertNull(buildServingText(null, null, null))
    }

    @Test
    fun per100EquivalentFromServing_prefersWeightThenConvertibleUnits() {
        assertEquals(
            60.0,
            per100EquivalentFromServing(
                nutrientValue = 120.0,
                nutrientKind = CoreServingNutrientKind.CALORIES,
                servingWeightGrams = 200.0,
                servingQuantity = 1.0,
                servingUnit = "cup",
            ) ?: 0.0,
            0.0001,
        )

        assertEquals(
            10.0,
            per100EquivalentFromServing(
                nutrientValue = 50.0,
                nutrientKind = CoreServingNutrientKind.CALORIES,
                servingWeightGrams = null,
                servingQuantity = 0.5,
                servingUnit = "l",
            ) ?: 0.0,
            0.0001,
        )

        assertEquals(
            220.0,
            per100EquivalentFromServing(
                nutrientValue = 220.0,
                nutrientKind = CoreServingNutrientKind.CALORIES,
                servingWeightGrams = null,
                servingQuantity = 2.0,
                servingUnit = "biscuit",
            ) ?: 0.0,
            0.0001,
        )
    }

    @Test
    fun per100EquivalentFromServing_dropsClearlyInvalidOutputs() {
        assertNull(
            per100EquivalentFromServing(
                nutrientValue = 9_999_999.0,
                nutrientKind = CoreServingNutrientKind.MACRO,
                servingWeightGrams = 40.0,
                servingQuantity = 40.0,
                servingUnit = "g",
            ),
        )
        assertNull(
            per100EquivalentFromServing(
                nutrientValue = -2.0,
                nutrientKind = CoreServingNutrientKind.MACRO,
                servingWeightGrams = 40.0,
                servingQuantity = 40.0,
                servingUnit = "g",
            ),
        )
        assertNull(
            per100EquivalentFromServing(
                nutrientValue = 5_000.0,
                nutrientKind = CoreServingNutrientKind.CALORIES,
                servingWeightGrams = null,
                servingQuantity = 1.0,
                servingUnit = "serving",
            ),
        )
    }
}
