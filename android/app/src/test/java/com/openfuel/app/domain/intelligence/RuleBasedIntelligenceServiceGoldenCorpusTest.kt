package com.openfuel.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleBasedIntelligenceServiceGoldenCorpusTest {
    private val service = RuleBasedIntelligenceService()

    @Test
    fun parseFoodText_goldenCorpus() {
        GOLDEN_PARSE_CASES.forEach { testCase ->
            val actual = service.parseFoodText(testCase.input)

            assertEquals(
                "${testCase.name}: item count",
                testCase.expectedItems.size,
                actual.items.size,
            )
            assertEquals(
                "${testCase.name}: confidence",
                testCase.expectedConfidence,
                actual.confidence,
            )
            assertEquals(
                "${testCase.name}: warnings",
                testCase.expectedWarnings,
                actual.warnings,
            )

            testCase.expectedItems.forEachIndexed { index, expected ->
                val item = actual.items[index]
                assertEquals("${testCase.name}: item[$index].rawName", expected.rawName, item.rawName)
                assertEquals(
                    "${testCase.name}: item[$index].normalisedName",
                    expected.normalisedName,
                    item.normalisedName,
                )
                assertEquals("${testCase.name}: item[$index].quantity", expected.quantity, item.quantity)
                assertEquals("${testCase.name}: item[$index].unit", expected.unit, item.unit)
                assertEquals("${testCase.name}: item[$index].notes", null, item.notes)
            }
        }
    }

    @Test
    fun normaliseSearchQuery_goldenCorpus() {
        GOLDEN_NORMALISE_CASES.forEach { testCase ->
            val actual = service.normaliseSearchQuery(testCase.input)
            assertEquals(testCase.expected, actual)
        }
    }
}

private data class ParseGoldenCase(
    val name: String,
    val input: String,
    val expectedItems: List<ExpectedFoodTextItem>,
    val expectedConfidence: Confidence,
    val expectedWarnings: List<String>,
)

private data class ExpectedFoodTextItem(
    val rawName: String,
    val normalisedName: String,
    val quantity: Double?,
    val unit: QuantityUnit?,
)

private data class NormaliseGoldenCase(
    val input: String,
    val expected: String,
)

private val GOLDEN_PARSE_CASES = listOf(
    ParseGoldenCase(
        name = "single_item_without_quantity",
        input = "banana",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "banana",
                normalisedName = "banana",
                quantity = null,
                unit = null,
            ),
        ),
        expectedConfidence = Confidence.MEDIUM,
        expectedWarnings = listOf("Missing quantity for \"banana\"."),
    ),
    ParseGoldenCase(
        name = "mixed_quantity_and_plain_item",
        input = "2 eggs and banana",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "2 eggs",
                normalisedName = "eggs",
                quantity = 2.0,
                unit = null,
            ),
            ExpectedFoodTextItem(
                rawName = "banana",
                normalisedName = "banana",
                quantity = null,
                unit = null,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = listOf("Missing quantity for \"banana\"."),
    ),
    ParseGoldenCase(
        name = "comma_separated_plain_items",
        input = "oats, milk, honey",
        expectedItems = listOf(
            ExpectedFoodTextItem(rawName = "oats", normalisedName = "oats", quantity = null, unit = null),
            ExpectedFoodTextItem(rawName = "milk", normalisedName = "milk", quantity = null, unit = null),
            ExpectedFoodTextItem(rawName = "honey", normalisedName = "honey", quantity = null, unit = null),
        ),
        expectedConfidence = Confidence.MEDIUM,
        expectedWarnings = listOf(
            "Missing quantity for \"oats\".",
            "Missing quantity for \"milk\".",
            "Missing quantity for \"honey\".",
        ),
    ),
    ParseGoldenCase(
        name = "units_and_decimals",
        input = "200g chicken + 1.5 cups milk",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "200g chicken",
                normalisedName = "chicken",
                quantity = 200.0,
                unit = QuantityUnit.GRAM,
            ),
            ExpectedFoodTextItem(
                rawName = "1.5 cups milk",
                normalisedName = "milk",
                quantity = 1.5,
                unit = QuantityUnit.CUP,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = emptyList(),
    ),
    ParseGoldenCase(
        name = "brand_and_punctuation",
        input = "2 tbsp Jif peanut butter, Chobani Greek Yogurt",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "2 tbsp Jif peanut butter",
                normalisedName = "jif peanut butter",
                quantity = 2.0,
                unit = QuantityUnit.TBSP,
            ),
            ExpectedFoodTextItem(
                rawName = "Chobani Greek Yogurt",
                normalisedName = "chobani greek yogurt",
                quantity = null,
                unit = null,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = listOf("Missing quantity for \"chobani greek yogurt\"."),
    ),
    ParseGoldenCase(
        name = "trailing_multiplier",
        input = "banana x2",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "banana x2",
                normalisedName = "banana",
                quantity = 2.0,
                unit = null,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = emptyList(),
    ),
    ParseGoldenCase(
        name = "plural_unit_pieces",
        input = "3 pieces crackers",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "3 pieces crackers",
                normalisedName = "crackers",
                quantity = 3.0,
                unit = QuantityUnit.PIECE,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = emptyList(),
    ),
    ParseGoldenCase(
        name = "unknown_unit_falls_back_to_quantity_only",
        input = "2 scoops protein powder",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "2 scoops protein powder",
                normalisedName = "scoops protein powder",
                quantity = 2.0,
                unit = null,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = emptyList(),
    ),
    ParseGoldenCase(
        name = "mixed_noise_tokens",
        input = "Add 250 ml orange juice and 1 serving greek yogurt now",
        expectedItems = listOf(
            ExpectedFoodTextItem(
                rawName = "Add 250 ml orange juice",
                normalisedName = "250 ml orange juice",
                quantity = null,
                unit = null,
            ),
            ExpectedFoodTextItem(
                rawName = "1 serving greek yogurt now",
                normalisedName = "greek yogurt",
                quantity = 1.0,
                unit = QuantityUnit.SERVING,
            ),
        ),
        expectedConfidence = Confidence.HIGH,
        expectedWarnings = listOf("Missing quantity for \"250 ml orange juice\"."),
    ),
    ParseGoldenCase(
        name = "garbage_input_separators_only",
        input = "and, , +",
        expectedItems = emptyList(),
        expectedConfidence = Confidence.LOW,
        expectedWarnings = listOf("No recognizable food items."),
    ),
    ParseGoldenCase(
        name = "garbage_input_blank",
        input = "   ",
        expectedItems = emptyList(),
        expectedConfidence = Confidence.LOW,
        expectedWarnings = listOf("No recognizable food items."),
    ),
)

private val GOLDEN_NORMALISE_CASES = listOf(
    NormaliseGoldenCase(
        input = "  Please   add   BANANA   now  ",
        expected = "banana",
    ),
    NormaliseGoldenCase(
        input = "Log Chobani Greek Yogurt today",
        expected = "chobani greek yogurt",
    ),
    NormaliseGoldenCase(
        input = "2 Eggs + banana",
        expected = "2 eggs + banana",
    ),
    NormaliseGoldenCase(
        input = "  ",
        expected = "",
    ),
)
