package com.openfuel.sharedcore.normalization

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreQueryNormalizerTest {
    @Test
    fun normalizeSearchQuery_goldenCorpus_isDeterministic() {
        NORMALIZATION_CASES.forEach { testCase ->
            val first = normalizeSearchQuery(testCase.input)
            val second = normalizeSearchQuery(testCase.input)
            assertEquals("${testCase.name}: normalized value", testCase.expected, first)
            assertEquals("${testCase.name}: deterministic", first, second)
        }
    }

    @Test
    fun buildNormalizedSqlLikePattern_buildsTokenPatternAndEscapesWildcards() {
        val simplePattern = buildNormalizedSqlLikePattern(
            normalizeSearchQuery("Coke-Zero 330 ml"),
        )
        assertEquals("coke%zero%330%ml", simplePattern)

        val escapedPattern = buildNormalizedSqlLikePattern(
            normalizeSearchQuery("100% whey_protein"),
        )
        assertEquals("100\\%%whey%protein", escapedPattern)
    }

    @Test
    fun buildNormalizedSqlLikePattern_blankQuery_returnsEmptyPattern() {
        assertEquals("", buildNormalizedSqlLikePattern(""))
    }
}

private data class NormalizationCase(
    val name: String,
    val input: String,
    val expected: String,
)

private val NORMALIZATION_CASES = listOf(
    NormalizationCase("trim_and_collapse_spaces", "  coke zero  ", "coke zero"),
    NormalizationCase("hyphenated_with_units", "Coke-Zero 330ml", "coke zero 330 ml"),
    NormalizationCase("parenthesized_units", "Coke Zero (330 ml)", "coke zero 330 ml"),
    NormalizationCase("mixed_dash_whitespace", "coke—zero\t330ml\n", "coke zero 330 ml"),
    NormalizationCase("yoghurt_variant", "Greek yoghurt 0%", "greek yogurt 0%"),
    NormalizationCase("percent_word", "Greek yogurt 0 percent", "greek yogurt 0%"),
    NormalizationCase("fraction_preserved", "1/2 cup oats", "1/2 cup oats"),
    NormalizationCase("unit_attached_ml", "500ml milk", "500 ml milk"),
    NormalizationCase("unit_attached_litre", "0.5l milk", "0.5 l milk"),
    NormalizationCase("millilitres_word", "250 Millilitres water", "250 ml water"),
    NormalizationCase("grams_plural", "2 grams salt", "2 g salt"),
    NormalizationCase("grams_singular", "2 gram salt", "2 g salt"),
    NormalizationCase("kilograms_plural", "1 kilograms rice", "1 kg rice"),
    NormalizationCase("ounce_singular", "1 ounce almonds", "1 oz almonds"),
    NormalizationCase("ounce_plural", "2 ounces almonds", "2 oz almonds"),
    NormalizationCase("multiplier_symbol", "Banana ×2", "banana x2"),
    NormalizationCase("plus_separator", "banana + peanut butter", "banana peanut butter"),
    NormalizationCase("smart_apostrophe", "Farmer’s yogurt", "farmer's yogurt"),
    NormalizationCase("tabs_and_newlines", "  tabs\tand\nnewlines ", "tabs and newlines"),
    NormalizationCase("emoji_and_punctuation", "🍌 banana!!!", "🍌 banana"),
    NormalizationCase("multiplier_token", "2x protein bar", "2x protein bar"),
    NormalizationCase("multiplier_spaced", "2 x protein bar", "2 x protein bar"),
    NormalizationCase("unicode_letters", "Café Latte 250ml", "café latte 250 ml"),
    NormalizationCase("underscore_separator", "milk___500ml", "milk 500 ml"),
    NormalizationCase("colon_and_commas", "brand: Chobani, item: Greek Yogurt", "brand chobani item greek yogurt"),
    NormalizationCase("punctuation_only", "...", ""),
    NormalizationCase("blank", "   ", ""),
    NormalizationCase("abbreviated_unit_with_dot", "oz.", "oz"),
    NormalizationCase("capital_litre", "1L cola", "1 l cola"),
    NormalizationCase("casing_normalization", "NUTRITIONIX App ID", "nutritionix app id"),
)
