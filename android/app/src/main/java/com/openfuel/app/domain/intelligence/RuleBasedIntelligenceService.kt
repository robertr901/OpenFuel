package com.openfuel.app.domain.intelligence

import java.util.Locale

class RuleBasedIntelligenceService : IntelligenceService {
    private companion object {
        val splitRegex = Regex("\\s*(?:,|\\+|\\band\\b)\\s*", RegexOption.IGNORE_CASE)
        val leadingQuantityWithUnitRegex =
            Regex("^([0-9]+(?:\\.[0-9]+)?)\\s*(kg|g|ml|l|cups?|cup|tbsp|tsp|servings?|serving|pieces?|piece)\\b\\s*(.+)$")
        val leadingQuantityOnlyRegex = Regex("^([0-9]+(?:\\.[0-9]+)?)\\s+(.+)$")
        val trailingMultiplierRegex = Regex("^(.+?)\\s*[x×]\\s*([0-9]+(?:\\.[0-9]+)?)$")
        val noiseTokens = setOf("please", "today", "now", "add", "log")
    }

    override fun parseFoodText(input: String): FoodTextIntent {
        val chunks = splitIntoChunks(input)
        if (chunks.isEmpty()) {
            return FoodTextIntent(
                items = emptyList(),
                confidence = Confidence.LOW,
                warnings = listOf("No recognizable food items."),
            )
        }

        val warnings = mutableListOf<String>()
        val items = chunks.mapNotNull { chunk ->
            parseItem(chunk, warnings)
        }

        val confidence = deriveConfidence(items)
        if (items.isEmpty()) {
            warnings += "No recognizable food items."
        }
        return FoodTextIntent(
            items = items,
            confidence = confidence,
            warnings = warnings.distinct(),
        )
    }

    override fun normaliseSearchQuery(input: String): String {
        return normalizeTextPreservingMeaning(input)
    }

    private fun splitIntoChunks(input: String): List<String> {
        if (input.isBlank()) {
            return emptyList()
        }
        return input
            .trim()
            .split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun parseItem(
        rawChunk: String,
        warnings: MutableList<String>,
    ): FoodTextItem? {
        val trimmed = rawChunk.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        var quantity: Double? = null
        var unit: QuantityUnit? = null
        var candidateName = trimmed

        val leadingUnitMatch = leadingQuantityWithUnitRegex.matchEntire(trimmed)
        if (leadingUnitMatch != null) {
            quantity = parseDouble(leadingUnitMatch.groupValues[1])
            unit = parseUnit(leadingUnitMatch.groupValues[2])
            candidateName = leadingUnitMatch.groupValues[3]
        } else {
            val trailingMultiplierMatch = trailingMultiplierRegex.matchEntire(trimmed)
            if (trailingMultiplierMatch != null) {
                candidateName = trailingMultiplierMatch.groupValues[1]
                quantity = parseDouble(trailingMultiplierMatch.groupValues[2])
            } else {
                val leadingQuantityMatch = leadingQuantityOnlyRegex.matchEntire(trimmed)
                if (leadingQuantityMatch != null) {
                    quantity = parseDouble(leadingQuantityMatch.groupValues[1])
                    candidateName = leadingQuantityMatch.groupValues[2]
                }
            }
        }

        val normalizedName = normalizeTextPreservingMeaning(candidateName)
        if (normalizedName.isBlank()) {
            warnings += "Ignored ambiguous item: \"$trimmed\"."
            return null
        }

        if (quantity == null && unit == null) {
            warnings += "Missing quantity for \"$normalizedName\"."
        }

        return FoodTextItem(
            rawName = trimmed,
            normalisedName = normalizedName,
            quantity = quantity,
            unit = unit,
            notes = null,
        )
    }

    private fun normalizeTextPreservingMeaning(input: String): String {
        val normalizedWhitespace = input
            .trim()
            .replace("\\s+".toRegex(), " ")
        if (normalizedWhitespace.isBlank()) {
            return ""
        }

        val lowered = normalizedWhitespace.lowercase(Locale.ROOT)
        return lowered
            .split(" ")
            .filter { token -> token.isNotBlank() && token !in noiseTokens }
            .joinToString(" ")
            .trim()
    }

    private fun deriveConfidence(items: List<FoodTextItem>): Confidence {
        if (items.isEmpty()) {
            return Confidence.LOW
        }
        val allHaveNames = items.all { it.normalisedName.isNotBlank() }
        if (!allHaveNames) {
            return Confidence.LOW
        }
        val hasQuantityOrUnit = items.any { it.quantity != null || it.unit != null }
        if (items.size in 1..3 && hasQuantityOrUnit) {
            return Confidence.HIGH
        }
        if (items.size in 1..5) {
            return Confidence.MEDIUM
        }
        return Confidence.LOW
    }

    private fun parseDouble(raw: String): Double? {
        return raw.toDoubleOrNull()
    }

    private fun parseUnit(raw: String): QuantityUnit? {
        return when (raw.lowercase(Locale.ROOT)) {
            "g" -> QuantityUnit.GRAM
            "kg" -> QuantityUnit.KILOGRAM
            "ml" -> QuantityUnit.MILLILITRE
            "l" -> QuantityUnit.LITRE
            "cup", "cups" -> QuantityUnit.CUP
            "tbsp" -> QuantityUnit.TBSP
            "tsp" -> QuantityUnit.TSP
            "piece", "pieces" -> QuantityUnit.PIECE
            "serving", "servings" -> QuantityUnit.SERVING
            else -> null
        }
    }
}
