package com.openfuel.sharedcore.normalization

import java.util.Locale
import kotlin.math.abs

enum class CoreServingNutrientKind(
    val maxPer100: Double,
) {
    CALORIES(maxPer100 = 900.0),
    MACRO(maxPer100 = 100.0),
}

private const val OUNCE_TO_GRAMS = 28.349523125
private const val POUND_TO_GRAMS = 453.59237

private val repeatedWhitespaceRegex = "\\s+".toRegex()
private val openParenWhitespaceRegex = "\\(\\s+".toRegex()
private val closeParenWhitespaceRegex = "\\s+\\)".toRegex()
private val beforeOpenParenRegex = "(\\S)\\(".toRegex()
private val numberUnitRegex = "(\\d+(?:[\\.,]\\d+)?)(?:\\s*)(mg|milligrams?|g|grams?|kg|kilograms?|ml|millilit(?:er|re)s?|l|lit(?:er|re)s?|oz|ounces?|lb|lbs|pounds?)\\b"
    .toRegex(RegexOption.IGNORE_CASE)

fun normalizeServingText(raw: String?): String? {
    val trimmed = raw
        ?.replace('\u00A0', ' ')
        ?.replace('\u202F', ' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    var normalized = trimmed
        .replace(openParenWhitespaceRegex, "(")
        .replace(closeParenWhitespaceRegex, ")")
        .replace(beforeOpenParenRegex, "$1 (")

    normalized = numberUnitRegex.replace(normalized) { match ->
        val quantity = match.groupValues[1]
        val normalizedUnit = normalizeServingUnit(match.groupValues[2]) ?: match.groupValues[2]
        "$quantity $normalizedUnit"
    }

    normalized = normalized
        .replace(repeatedWhitespaceRegex, " ")
        .trim()

    return normalized.takeIf { it.isNotEmpty() }
}

fun normalizeServingUnit(rawUnit: String?): String? {
    val normalized = rawUnit
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.replace(".", "")
        ?.lowercase(Locale.ROOT)
        ?.replace(repeatedWhitespaceRegex, " ")
        ?: return null

    return when (normalized) {
        "mg", "milligram", "milligrams" -> "mg"
        "g", "gram", "grams" -> "g"
        "kg", "kilogram", "kilograms" -> "kg"
        "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> "ml"
        "l", "liter", "liters", "litre", "litres" -> "l"
        "oz", "ounce", "ounces" -> "oz"
        "lb", "lbs", "pound", "pounds" -> "lb"
        else -> normalized
    }
}

fun buildServingText(
    servingQuantity: Double?,
    servingUnit: String?,
    servingWeightGrams: Double? = null,
): String? {
    val quantity = normalizeServingQuantity(servingQuantity)
    val unit = normalizeServingUnit(servingUnit)
    val grams = normalizeServingQuantity(servingWeightGrams)

    val raw = when {
        quantity != null && unit != null && grams != null &&
            !(unit == "g" && abs(quantity - grams) < 0.0001) -> {
            "${quantity.trimmedForDisplay()} $unit (${grams.trimmedForDisplay()} g)"
        }
        quantity != null && unit != null -> "${quantity.trimmedForDisplay()} $unit"
        grams != null -> "${grams.trimmedForDisplay()} g"
        unit != null -> unit
        else -> null
    }

    return normalizeServingText(raw)
}

fun per100EquivalentFromServing(
    nutrientValue: Double?,
    nutrientKind: CoreServingNutrientKind,
    servingWeightGrams: Double?,
    servingQuantity: Double?,
    servingUnit: String?,
): Double? {
    val raw = nutrientValue.sanitizeRawNutrient() ?: return null
    val servingBaseAmount = normalizeServingQuantity(servingWeightGrams)
        ?: servingAmountInBaseUnits(
            quantity = servingQuantity,
            unit = servingUnit,
        )
    val normalized = if (servingBaseAmount != null) {
        raw * (100.0 / servingBaseAmount)
    } else {
        raw
    }
    return sanitizePer100Nutrient(
        value = normalized,
        kind = nutrientKind,
    )
}

fun sanitizePer100Nutrient(
    value: Double?,
    kind: CoreServingNutrientKind,
): Double? {
    val sanitized = value.sanitizeRawNutrient() ?: return null
    return sanitized.takeIf { it <= kind.maxPer100 }
}

private fun servingAmountInBaseUnits(
    quantity: Double?,
    unit: String?,
): Double? {
    val normalizedQuantity = normalizeServingQuantity(quantity) ?: return null
    return when (normalizeServingUnit(unit)) {
        "mg" -> normalizedQuantity / 1_000.0
        "g" -> normalizedQuantity
        "kg" -> normalizedQuantity * 1_000.0
        "ml" -> normalizedQuantity
        "l" -> normalizedQuantity * 1_000.0
        "oz" -> normalizedQuantity * OUNCE_TO_GRAMS
        "lb" -> normalizedQuantity * POUND_TO_GRAMS
        else -> null
    }
}

private fun normalizeServingQuantity(quantity: Double?): Double? {
    return quantity?.takeIf { it.isFinite() && it > 0.0 }
}

private fun Double?.sanitizeRawNutrient(): Double? {
    val value = this ?: return null
    return value.takeIf { it.isFinite() && it >= 0.0 }
}

fun Double.trimmedForDisplay(): String {
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        this.toString()
    }
}
