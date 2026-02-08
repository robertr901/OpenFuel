package com.openfuel.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedIntelligenceServiceTest {
    private val service = RuleBasedIntelligenceService()

    @Test
    fun parseFoodText_singleItem_banana() {
        val intent = service.parseFoodText("banana")

        assertEquals(1, intent.items.size)
        assertEquals("banana", intent.items[0].normalisedName)
        assertEquals(null, intent.items[0].quantity)
    }

    @Test
    fun parseFoodText_multipleItems_withAndSplitter() {
        val intent = service.parseFoodText("2 eggs and banana")

        assertEquals(2, intent.items.size)
        assertEquals("eggs", intent.items[0].normalisedName)
        assertEquals(2.0, intent.items[0].quantity ?: 0.0, 0.0)
        assertEquals("banana", intent.items[1].normalisedName)
    }

    @Test
    fun parseFoodText_multipleItems_withCommas() {
        val intent = service.parseFoodText("oats, milk, honey")

        assertEquals(3, intent.items.size)
        assertEquals("oats", intent.items[0].normalisedName)
        assertEquals("milk", intent.items[1].normalisedName)
        assertEquals("honey", intent.items[2].normalisedName)
    }

    @Test
    fun parseFoodText_extractsUnitsAndQuantities() {
        val intent = service.parseFoodText("200g chicken and 1.5 cups milk and 250 ml orange juice")

        assertEquals(3, intent.items.size)
        assertEquals(200.0, intent.items[0].quantity ?: 0.0, 0.0)
        assertEquals(QuantityUnit.GRAM, intent.items[0].unit)
        assertEquals("chicken", intent.items[0].normalisedName)

        assertEquals(1.5, intent.items[1].quantity ?: 0.0, 0.0)
        assertEquals(QuantityUnit.CUP, intent.items[1].unit)
        assertEquals("milk", intent.items[1].normalisedName)

        assertEquals(250.0, intent.items[2].quantity ?: 0.0, 0.0)
        assertEquals(QuantityUnit.MILLILITRE, intent.items[2].unit)
        assertEquals("orange juice", intent.items[2].normalisedName)
    }

    @Test
    fun parseFoodText_extractsTrailingMultiplier() {
        val intent = service.parseFoodText("banana x2")

        assertEquals(1, intent.items.size)
        assertEquals("banana", intent.items[0].normalisedName)
        assertEquals(2.0, intent.items[0].quantity ?: 0.0, 0.0)
        assertEquals(null, intent.items[0].unit)
    }

    @Test
    fun parseFoodText_handlesNastyInputsWithoutThrowing() {
        val blank = service.parseFoodText("   ")
        val emoji = service.parseFoodText("🍌🍌")
        val partial = service.parseFoodText("2x")
        val separatorsOnly = service.parseFoodText("and, , +")

        assertEquals(0, blank.items.size)
        assertTrue(blank.warnings.isNotEmpty())

        assertEquals(1, emoji.items.size)
        assertEquals("🍌🍌", emoji.items[0].normalisedName)

        assertEquals(1, partial.items.size)
        assertEquals("2x", partial.items[0].normalisedName)

        assertEquals(0, separatorsOnly.items.size)
        assertEquals(Confidence.LOW, separatorsOnly.confidence)
    }

    @Test
    fun normaliseSearchQuery_removesNoiseAndNormalisesWhitespace() {
        val normalized = service.normaliseSearchQuery("  Please   add   BANANA   now  ")

        assertEquals("banana", normalized)
    }
}
