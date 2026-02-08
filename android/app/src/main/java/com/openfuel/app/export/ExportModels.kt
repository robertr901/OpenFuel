package com.openfuel.app.export

import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntry
import java.time.Instant

const val EXPORT_SCHEMA_VERSION = 2

enum class ExportFormat {
    JSON,
    CSV,
}

data class ExportRedactionOptions(
    val redactBrand: Boolean,
)

data class AdvancedExportPreview(
    val foodCount: Int,
    val mealEntryCount: Int,
    val dailyGoalCount: Int,
    val redactedBrandCount: Int,
) {
    companion object {
        fun empty(): AdvancedExportPreview = AdvancedExportPreview(
            foodCount = 0,
            mealEntryCount = 0,
            dailyGoalCount = 0,
            redactedBrandCount = 0,
        )
    }
}

data class ExportSnapshot(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Instant,
    val foods: List<FoodItem>,
    val mealEntries: List<MealEntry>,
    val dailyGoals: List<DailyGoal>,
)
