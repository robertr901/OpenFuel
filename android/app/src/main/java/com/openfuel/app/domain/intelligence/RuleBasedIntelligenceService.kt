package com.openfuel.app.domain.intelligence

import java.util.Locale

class RuleBasedIntelligenceService : IntelligenceService {
    private companion object {
        const val WARNING_NO_RECOGNIZABLE_ITEMS = "No recognizable food items."
        const val DECIMAL_COMMA_PLACEHOLDER = "__DECIMAL_COMMA__"

        val itemSeparatorRegex = Regex("\\s*(?:,|\\+|\\band\\b)\\s*", RegexOption.IGNORE_CASE)
        val decimalCommaRegex = Regex("(?<=\\d),(?=\\d)")
        val leadingQuantityWithUnitRegex =
            Regex(
                "^([0-9]+(?:[\\.,][0-9]+)?)\\s*(kg|g|ml|l|cups?|cup|tbsp|tablespoons?|tsp|teaspoons?|servings?|serving|pieces?|piece)\\b\\s*(.+)$",
                RegexOption.IGNORE_CASE,
            )
        val leadingQuantityOnlyRegex = Regex("^([0-9]+(?:[\\.,][0-9]+)?)\\s+(.+)$")
        val trailingMultiplierRegex = Regex("^(.+?)\\s*[x×]\\s*([0-9]+(?:[\\.,][0-9]+)?)$")
        val noiseTokens = setOf("please", "today", "now", "add", "log")
        val trimEdgePunctuation = setOf('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}')
    }

    override fun parseFoodText(input: String): FoodTextIntent {
        val chunks = splitIntoChunks(input)
        if (chunks.isEmpty()) {
            return noRecognizableItemsIntent()
        }

        val warnings = mutableListOf<String>()
        val items = chunks.mapNotNull { chunk ->
            parseItem(chunk, warnings)
        }

        val confidence = deriveConfidence(items)
        if (items.isEmpty()) {
            warnings += WARNING_NO_RECOGNIZABLE_ITEMS
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
            .replace(decimalCommaRegex, DECIMAL_COMMA_PLACEHOLDER)
            .split(itemSeparatorRegex)
            .map { it.replace(DECIMAL_COMMA_PLACEHOLDER, ",").trim() }
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

        val parseCandidate = dropLeadingNoiseTokens(trimmed)
        var quantity: Double? = null
        var unit: QuantityUnit? = null
        var candidateName = parseCandidate

        val leadingUnitMatch = leadingQuantityWithUnitRegex.matchEntire(parseCandidate)
        if (leadingUnitMatch != null) {
            quantity = parseDouble(leadingUnitMatch.groupValues[1])
            unit = parseUnit(leadingUnitMatch.groupValues[2])
            candidateName = leadingUnitMatch.groupValues[3]
        } else {
            val trailingMultiplierMatch = trailingMultiplierRegex.matchEntire(parseCandidate)
            if (trailingMultiplierMatch != null) {
                candidateName = trailingMultiplierMatch.groupValues[1]
                quantity = parseDouble(trailingMultiplierMatch.groupValues[2])
            } else {
                val leadingQuantityMatch = leadingQuantityOnlyRegex.matchEntire(parseCandidate)
                if (leadingQuantityMatch != null) {
                    quantity = parseDouble(leadingQuantityMatch.groupValues[1])
                    candidateName = leadingQuantityMatch.groupValues[2]
                }
            }
        }

        val normalizedName = normalizeTextPreservingMeaning(candidateName)
        if (normalizedName.isBlank()) {
            warnings += ambiguousItemWarning(trimmed)
            return null
        }

        if (quantity == null && unit == null) {
            warnings += missingQuantityWarning(normalizedName)
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

        return normalizedWhitespace
            .split(" ")
            .map { token -> normalizeToken(token) }
            .filter { token -> token.isNotBlank() && token !in noiseTokens }
            .joinToString(" ")
            .trim()
    }

    private fun normalizeToken(input: String): String {
        var value = input.trim()
        while (value.isNotEmpty() && value.first() in trimEdgePunctuation) {
            value = value.drop(1)
        }
        while (value.isNotEmpty() && value.last() in trimEdgePunctuation) {
            value = value.dropLast(1)
        }
        return value.lowercase(Locale.ROOT)
    }

    private fun dropLeadingNoiseTokens(input: String): String {
        val tokens = input.trim()
            .split("\\s+".toRegex())
            .toMutableList()
        while (tokens.isNotEmpty()) {
            val normalizedHead = normalizeToken(tokens.first())
            if (normalizedHead in noiseTokens) {
                tokens.removeAt(0)
            } else {
                break
            }
        }
        return tokens.joinToString(" ").trim()
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
        val normalized = raw.trim()
        if (normalized.contains(',') && !normalized.contains('.')) {
            return normalized.replace(',', '.').toDoubleOrNull()
        }
        return normalized.toDoubleOrNull()
    }

    private fun parseUnit(raw: String): QuantityUnit? {
        return when (raw.lowercase(Locale.ROOT)) {
            "g" -> QuantityUnit.GRAM
            "kg" -> QuantityUnit.KILOGRAM
            "ml" -> QuantityUnit.MILLILITRE
            "l" -> QuantityUnit.LITRE
            "cup", "cups" -> QuantityUnit.CUP
            "tbsp", "tablespoon", "tablespoons" -> QuantityUnit.TBSP
            "tsp", "teaspoon", "teaspoons" -> QuantityUnit.TSP
            "piece", "pieces" -> QuantityUnit.PIECE
            "serving", "servings" -> QuantityUnit.SERVING
            else -> null
        }
    }

    private fun noRecognizableItemsIntent(): FoodTextIntent {
        return FoodTextIntent(
            items = emptyList(),
            confidence = Confidence.LOW,
            warnings = listOf(WARNING_NO_RECOGNIZABLE_ITEMS),
        )
    }

    private fun missingQuantityWarning(normalizedName: String): String {
        return "Missing quantity for \"$normalizedName\"."
    }

    private fun ambiguousItemWarning(rawName: String): String {
        return "Ignored ambiguous item: \"$rawName\"."
    }
}
