package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyReviewCalculatorTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun buildLast7DaySummary_aggregatesTotalsAveragesAndMissingDays() {
        val today = LocalDate.parse("2026-02-12")
        val entries = listOf(
            entryForDay("2026-02-12", calories = 500.0, protein = 30.0, carbs = 50.0, fat = 10.0),
            entryForDay("2026-02-11", calories = 700.0, protein = 40.0, carbs = 70.0, fat = 20.0),
            entryForDay("2026-02-09", calories = 300.0, protein = 20.0, carbs = 30.0, fat = 10.0),
        )

        val summary = WeeklyReviewCalculator.buildLast7DaySummary(entries, today, zoneId)

        assertEquals(LocalDate.parse("2026-02-06"), summary.startDate)
        assertEquals(today, summary.endDate)
        assertEquals(3, summary.loggedDays)
        assertEquals(4, summary.missingDays)
        assertEquals(0, summary.unknownEntryCount)
        assertEquals(1500.0, summary.total.caloriesKcal, 0.0001)
        assertEquals(90.0, summary.total.proteinG, 0.0001)
        assertEquals(150.0, summary.total.carbsG, 0.0001)
        assertEquals(40.0, summary.total.fatG, 0.0001)
        assertEquals(500.0, summary.average.caloriesKcal, 0.0001)
        assertEquals(30.0, summary.average.proteinG, 0.0001)
    }

    @Test
    fun buildLast7DaySummary_countsUnknownEntries() {
        val today = LocalDate.parse("2026-02-12")
        val entries = listOf(
            entryForDay("2026-02-12", calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0),
            entryForDay("2026-02-11", calories = 300.0, protein = 10.0, carbs = 20.0, fat = 5.0),
        )

        val summary = WeeklyReviewCalculator.buildLast7DaySummary(entries, today, zoneId)

        assertEquals(1, summary.unknownEntryCount)
    }

    @Test
    fun buildLast7DaySummary_emptyEntries_returnsZeros() {
        val today = LocalDate.parse("2026-02-12")
        val summary = WeeklyReviewCalculator.buildLast7DaySummary(
            entries = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(0, summary.loggedDays)
        assertEquals(7, summary.missingDays)
        assertEquals(0.0, summary.total.caloriesKcal, 0.0001)
        assertEquals(0.0, summary.average.caloriesKcal, 0.0001)
    }

    private fun entryForDay(
        date: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
    ): MealEntryWithFood {
        val day = LocalDate.parse(date)
        val instant = day.atStartOfDay(zoneId).plusHours(12).toInstant()
        return MealEntryWithFood(
            entry = MealEntry(
                id = "entry-$date",
                timestamp = instant,
                mealType = MealType.LUNCH,
                foodItemId = "food-$date",
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            ),
            food = FoodItem(
                id = "food-$date",
                name = "Food $date",
                brand = null,
                caloriesKcal = calories,
                proteinG = protein,
                carbsG = carbs,
                fatG = fat,
                createdAt = Instant.EPOCH,
            ),
        )
    }
}
