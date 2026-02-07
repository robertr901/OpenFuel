package com.openfuel.app.export

import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSerializerTest {
    @Test
    fun serialize_includesTopLevelFields() {
        val snapshot = ExportSnapshot(
            schemaVersion = EXPORT_SCHEMA_VERSION,
            appVersion = "1.0",
            exportedAt = Instant.parse("2024-01-01T00:00:00Z"),
            foods = listOf(
                FoodItem(
                    id = "food-1",
                    name = "Yogurt",
                    brand = "OpenFuel",
                    barcode = "1234567890",
                    caloriesKcal = 120.0,
                    proteinG = 10.0,
                    carbsG = 14.0,
                    fatG = 3.0,
                    isFavorite = true,
                    isReportedIncorrect = false,
                    createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                ),
            ),
            mealEntries = listOf(
                MealEntry(
                    id = "entry-1",
                    timestamp = Instant.parse("2024-01-01T08:00:00Z"),
                    mealType = MealType.BREAKFAST,
                    foodItemId = "food-1",
                    quantity = 1.0,
                    unit = FoodUnit.SERVING,
                ),
            ),
            dailyGoals = listOf(
                DailyGoal(
                    date = LocalDate.parse("2024-01-01"),
                    caloriesKcalTarget = 2000.0,
                    proteinGTarget = 120.0,
                    carbsGTarget = 250.0,
                    fatGTarget = 60.0,
                ),
            ),
        )

        val json = ExportSerializer().serialize(snapshot)

        assertTrue(json.contains("\"schemaVersion\":$EXPORT_SCHEMA_VERSION"))
        assertTrue(json.contains("\"appVersion\":\"1.0\""))
        assertTrue(json.contains("\"foods\""))
        assertTrue(json.contains("\"mealEntries\""))
        assertTrue(json.contains("\"dailyGoals\""))
        assertTrue(json.contains("\"food-1\""))
        assertTrue(json.contains("\"barcode\":\"1234567890\""))
        assertTrue(json.contains("\"isFavorite\":true"))
        assertTrue(json.contains("\"isReportedIncorrect\":false"))
        assertTrue(json.contains("\"mealType\":\"BREAKFAST\""))
    }
}
