package com.openfuel.app.data.remote

import com.openfuel.sharedcore.normalization.CoreServingNutrientKind
import com.openfuel.sharedcore.normalization.buildServingText as coreBuildServingText
import com.openfuel.sharedcore.normalization.normalizeServingText as coreNormalizeServingText
import com.openfuel.sharedcore.normalization.normalizeServingUnit as coreNormalizeServingUnit
import com.openfuel.sharedcore.normalization.per100EquivalentFromServing as corePer100EquivalentFromServing
import com.openfuel.sharedcore.normalization.sanitizePer100Nutrient as coreSanitizePer100Nutrient
import com.openfuel.sharedcore.normalization.trimmedForDisplay as coreTrimmedForDisplay

internal enum class ServingNutrientKind(
    val maxPer100: Double,
) {
    CALORIES(maxPer100 = 900.0),
    MACRO(maxPer100 = 100.0),
}

internal fun normalizeServingText(raw: String?): String? {
    return coreNormalizeServingText(raw)
}

internal fun normalizeServingUnit(rawUnit: String?): String? {
    return coreNormalizeServingUnit(rawUnit)
}

internal fun buildServingText(
    servingQuantity: Double?,
    servingUnit: String?,
    servingWeightGrams: Double? = null,
): String? {
    return coreBuildServingText(
        servingQuantity = servingQuantity,
        servingUnit = servingUnit,
        servingWeightGrams = servingWeightGrams,
    )
}

internal fun per100EquivalentFromServing(
    nutrientValue: Double?,
    nutrientKind: ServingNutrientKind,
    servingWeightGrams: Double?,
    servingQuantity: Double?,
    servingUnit: String?,
): Double? {
    return corePer100EquivalentFromServing(
        nutrientValue = nutrientValue,
        nutrientKind = nutrientKind.toCore(),
        servingWeightGrams = servingWeightGrams,
        servingQuantity = servingQuantity,
        servingUnit = servingUnit,
    )
}

internal fun sanitizePer100Nutrient(
    value: Double?,
    kind: ServingNutrientKind,
): Double? {
    return coreSanitizePer100Nutrient(
        value = value,
        kind = kind.toCore(),
    )
}

internal fun Double.trimmedForDisplay(): String {
    return coreTrimmedForDisplay()
}

private fun ServingNutrientKind.toCore(): CoreServingNutrientKind {
    return when (this) {
        ServingNutrientKind.CALORIES -> CoreServingNutrientKind.CALORIES
        ServingNutrientKind.MACRO -> CoreServingNutrientKind.MACRO
    }
}
