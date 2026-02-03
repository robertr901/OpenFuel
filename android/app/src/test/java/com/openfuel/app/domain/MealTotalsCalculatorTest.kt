package com.openfuel.app.domain

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.util.MealTotalsCalculator
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MealTotalsCalculatorTest {
    @Test
    fun totalsForEmptyDay_returnsZero() {
        val totals = MealTotalsCalculator.totalsFor(emptyList()).totals
        assertEquals(0.0, totals.caloriesKcal, 0.0001)
        assertEquals(0.0, totals.proteinG, 0.0001)
        assertEquals(0.0, totals.carbsG, 0.0001)
        assertEquals(0.0, totals.fatG, 0.0001)
    }

    @Test
    fun totalsForEntries_handlesServingsAndGrams() {
        val food = FoodItem(
            id = "food-1",
            name = "Oats",
            brand = null,
            caloriesKcal = 200.0,
            proteinG = 10.0,
            carbsG = 30.0,
            fatG = 5.0,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )
        val entryServing = MealEntry(
            id = "entry-1",
            timestamp = Instant.parse("2024-01-01T12:00:00Z"),
            mealType = MealType.BREAKFAST,
            foodItemId = food.id,
            quantity = 2.0,
            unit = FoodUnit.SERVING,
        )
        val entryGrams = MealEntry(
            id = "entry-2",
            timestamp = Instant.parse("2024-01-01T12:30:00Z"),
            mealType = MealType.BREAKFAST,
            foodItemId = food.id,
            quantity = 50.0,
            unit = FoodUnit.GRAM,
        )
        val totals = MealTotalsCalculator.totalsFor(
            listOf(
                MealEntryWithFood(entryServing, food),
                MealEntryWithFood(entryGrams, food),
            ),
        ).totals
        assertEquals(500.0, totals.caloriesKcal, 0.0001)
        assertEquals(25.0, totals.proteinG, 0.0001)
        assertEquals(75.0, totals.carbsG, 0.0001)
        assertEquals(12.5, totals.fatG, 0.0001)
    }

    @Test
    fun totalsByMealType_groupsEntries() {
        val food = FoodItem(
            id = "food-2",
            name = "Eggs",
            brand = null,
            caloriesKcal = 100.0,
            proteinG = 6.0,
            carbsG = 1.0,
            fatG = 7.0,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )
        val breakfastEntry = MealEntry(
            id = "entry-3",
            timestamp = Instant.parse("2024-01-01T08:00:00Z"),
            mealType = MealType.BREAKFAST,
            foodItemId = food.id,
            quantity = 1.0,
            unit = FoodUnit.SERVING,
        )
        val lunchEntry = MealEntry(
            id = "entry-4",
            timestamp = Instant.parse("2024-01-01T12:00:00Z"),
            mealType = MealType.LUNCH,
            foodItemId = food.id,
            quantity = 1.0,
            unit = FoodUnit.SERVING,
        )
        val grouped = MealTotalsCalculator.totalsByMealType(
            listOf(
                MealEntryWithFood(breakfastEntry, food),
                MealEntryWithFood(lunchEntry, food),
            ),
        )
        assertEquals(2, grouped.size)
        assertEquals(100.0, grouped[MealType.BREAKFAST]?.totals?.caloriesKcal ?: 0.0, 0.0001)
        assertEquals(100.0, grouped[MealType.LUNCH]?.totals?.caloriesKcal ?: 0.0, 0.0001)
    }
}
