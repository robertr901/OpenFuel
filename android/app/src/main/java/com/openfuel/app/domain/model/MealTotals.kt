package com.openfuel.app.domain.model

data class MacroTotals(
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    companion object {
        val Zero = MacroTotals(0.0, 0.0, 0.0, 0.0)
    }
}

data class MealTotals(
    val totals: MacroTotals,
)
