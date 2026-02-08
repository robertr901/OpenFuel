package com.openfuel.app.domain.model

import java.time.Instant
import java.util.UUID

private const val MAX_CALORIES_KCAL = 10_000.0
private const val MAX_MACRO_GRAMS = 1_000.0
private const val DEFAULT_IMPORTED_FOOD_NAME = "Imported food"

fun RemoteFoodCandidate.toLocalFoodItem(
    id: String = UUID.randomUUID().toString(),
    createdAt: Instant = Instant.now(),
): FoodItem {
    return FoodItem(
        id = id,
        name = name.trim().ifBlank { DEFAULT_IMPORTED_FOOD_NAME },
        brand = brand?.trim()?.takeIf { it.isNotEmpty() },
        barcode = barcode?.trim()?.takeIf { it.isNotEmpty() },
        caloriesKcal = caloriesKcalPer100g?.coerceIn(0.0, MAX_CALORIES_KCAL) ?: 0.0,
        proteinG = proteinGPer100g?.coerceIn(0.0, MAX_MACRO_GRAMS) ?: 0.0,
        carbsG = carbsGPer100g?.coerceIn(0.0, MAX_MACRO_GRAMS) ?: 0.0,
        fatG = fatGPer100g?.coerceIn(0.0, MAX_MACRO_GRAMS) ?: 0.0,
        isFavorite = false,
        createdAt = createdAt,
    )
}
