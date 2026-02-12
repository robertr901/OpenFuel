package com.openfuel.app.domain.quality

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntryWithFood

enum class FoodDataQualityLevel {
    COMPLETE,
    NEEDS_REVIEW,
}

enum class FoodDataQualityReason {
    REPORTED_INCORRECT,
    UNKNOWN_CALORIES,
    UNKNOWN_MACROS,
}

data class FoodDataQualitySignals(
    val level: FoodDataQualityLevel,
    val reasons: Set<FoodDataQualityReason> = emptySet(),
) {
    val needsReview: Boolean get() = level == FoodDataQualityLevel.NEEDS_REVIEW
}

fun classifyFoodItemQuality(food: FoodItem): FoodDataQualitySignals {
    val reasons = buildSet {
        if (food.isReportedIncorrect) {
            add(FoodDataQualityReason.REPORTED_INCORRECT)
        }
        if (food.caloriesKcal <= 0.0) {
            add(FoodDataQualityReason.UNKNOWN_CALORIES)
        }
        if (food.proteinG <= 0.0 && food.carbsG <= 0.0 && food.fatG <= 0.0) {
            add(FoodDataQualityReason.UNKNOWN_MACROS)
        }
    }
    return if (reasons.isEmpty()) {
        FoodDataQualitySignals(level = FoodDataQualityLevel.COMPLETE)
    } else {
        FoodDataQualitySignals(
            level = FoodDataQualityLevel.NEEDS_REVIEW,
            reasons = reasons,
        )
    }
}

fun classifyMealEntryQuality(entry: MealEntryWithFood): FoodDataQualitySignals {
    return classifyFoodItemQuality(entry.food)
}
