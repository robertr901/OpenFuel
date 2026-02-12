package com.openfuel.app.domain.quality

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDataQualityTest {
    @Test
    fun classifyFoodItemQuality_complete_whenCaloriesAndAnyMacroPresent() {
        val signals = classifyFoodItemQuality(
            food(
                calories = 220.0,
                protein = 12.0,
                carbs = 20.0,
                fat = 8.0,
            ),
        )

        assertEquals(FoodDataQualityLevel.COMPLETE, signals.level)
        assertTrue(signals.reasons.isEmpty())
        assertFalse(signals.needsReview)
    }

    @Test
    fun classifyFoodItemQuality_needsReview_whenReportedIncorrect() {
        val signals = classifyFoodItemQuality(
            food(
                calories = 180.0,
                protein = 8.0,
                carbs = 22.0,
                fat = 5.0,
                reportedIncorrect = true,
            ),
        )

        assertEquals(FoodDataQualityLevel.NEEDS_REVIEW, signals.level)
        assertTrue(signals.reasons.contains(FoodDataQualityReason.REPORTED_INCORRECT))
        assertTrue(signals.needsReview)
    }

    @Test
    fun classifyFoodItemQuality_needsReview_whenCaloriesUnknown() {
        val signals = classifyFoodItemQuality(
            food(
                calories = 0.0,
                protein = 10.0,
                carbs = 25.0,
                fat = 7.0,
            ),
        )

        assertEquals(FoodDataQualityLevel.NEEDS_REVIEW, signals.level)
        assertTrue(signals.reasons.contains(FoodDataQualityReason.UNKNOWN_CALORIES))
    }

    @Test
    fun classifyFoodItemQuality_needsReview_whenAllMacrosUnknown() {
        val signals = classifyFoodItemQuality(
            food(
                calories = 200.0,
                protein = 0.0,
                carbs = 0.0,
                fat = 0.0,
            ),
        )

        assertEquals(FoodDataQualityLevel.NEEDS_REVIEW, signals.level)
        assertTrue(signals.reasons.contains(FoodDataQualityReason.UNKNOWN_MACROS))
    }

    @Test
    fun classifyMealEntryQuality_delegatesToFoodClassification() {
        val entry = MealEntryWithFood(
            entry = MealEntry(
                id = "entry-1",
                timestamp = Instant.parse("2026-02-12T12:00:00Z"),
                mealType = MealType.LUNCH,
                foodItemId = "food-1",
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            ),
            food = food(
                calories = 0.0,
                protein = 0.0,
                carbs = 0.0,
                fat = 0.0,
            ),
        )

        val signals = classifyMealEntryQuality(entry)

        assertEquals(FoodDataQualityLevel.NEEDS_REVIEW, signals.level)
        assertTrue(signals.reasons.contains(FoodDataQualityReason.UNKNOWN_CALORIES))
        assertTrue(signals.reasons.contains(FoodDataQualityReason.UNKNOWN_MACROS))
    }

    private fun food(
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        reportedIncorrect: Boolean = false,
    ): FoodItem {
        return FoodItem(
            id = "food-1",
            name = "Food",
            brand = null,
            caloriesKcal = calories,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            isReportedIncorrect = reportedIncorrect,
            createdAt = Instant.EPOCH,
        )
    }
}
