package com.openfuel.app.domain

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.util.InsightsCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculatorTest {
    @Test
    fun buildSnapshot_aggregatesLast7AndLast30Windows() {
        val zoneId = ZoneId.of("UTC")
        val today = LocalDate.parse("2026-02-07")
        val entries = listOf(
            sampleEntry("2026-02-07T08:00:00Z", calories = 500.0),
            sampleEntry("2026-02-06T08:00:00Z", calories = 400.0),
            sampleEntry("2026-01-15T08:00:00Z", calories = 300.0),
        )

        val snapshot = InsightsCalculator.buildSnapshot(
            entries = entries,
            today = today,
            zoneId = zoneId,
        )

        assertEquals(2, snapshot.last7Days.loggedDays)
        assertEquals(900.0, snapshot.last7Days.total.caloriesKcal, 0.001)
        assertEquals(3, snapshot.last30Days.loggedDays)
        assertEquals(1200.0, snapshot.last30Days.total.caloriesKcal, 0.001)
        assertEquals(10, snapshot.consistencyScore)
    }

    @Test
    fun buildSnapshot_withNoEntries_returnsZeros() {
        val snapshot = InsightsCalculator.buildSnapshot(
            entries = emptyList(),
            today = LocalDate.parse("2026-02-07"),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(0, snapshot.last7Days.loggedDays)
        assertEquals(0.0, snapshot.last7Days.total.caloriesKcal, 0.001)
        assertEquals(0, snapshot.consistencyScore)
    }

    private fun sampleEntry(
        timestamp: String,
        calories: Double,
    ): MealEntryWithFood {
        val foodId = "food-$timestamp"
        return MealEntryWithFood(
            entry = MealEntry(
                id = "entry-$timestamp",
                timestamp = Instant.parse(timestamp),
                mealType = MealType.BREAKFAST,
                foodItemId = foodId,
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            ),
            food = FoodItem(
                id = foodId,
                name = "Sample",
                brand = null,
                caloriesKcal = calories,
                proteinG = 10.0,
                carbsG = 20.0,
                fatG = 5.0,
                createdAt = Instant.parse(timestamp),
            ),
        )
    }
}
