package com.openfuel.app.export

import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntry
import java.time.Instant

const val EXPORT_SCHEMA_VERSION = 1

data class ExportSnapshot(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Instant,
    val foods: List<FoodItem>,
    val mealEntries: List<MealEntry>,
    val dailyGoals: List<DailyGoal>,
)
