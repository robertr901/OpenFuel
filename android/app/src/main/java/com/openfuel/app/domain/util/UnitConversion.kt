package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MacroTotals

object UnitConversion {
    private const val DEFAULT_SERVING_GRAMS = 100.0

    fun servingsFrom(quantity: Double, unit: FoodUnit, defaultServingGrams: Double = DEFAULT_SERVING_GRAMS): Double {
        return when (unit) {
            FoodUnit.SERVING -> quantity
            FoodUnit.GRAM -> quantity / defaultServingGrams
        }
    }

    fun scaleMacros(
        foodItem: FoodItem,
        quantity: Double,
        unit: FoodUnit,
        defaultServingGrams: Double = DEFAULT_SERVING_GRAMS,
    ): MacroTotals {
        val servings = servingsFrom(quantity, unit, defaultServingGrams)
        return MacroTotals(
            caloriesKcal = foodItem.caloriesKcal * servings,
            proteinG = foodItem.proteinG * servings,
            carbsG = foodItem.carbsG * servings,
            fatG = foodItem.fatG * servings,
        )
    }
}
