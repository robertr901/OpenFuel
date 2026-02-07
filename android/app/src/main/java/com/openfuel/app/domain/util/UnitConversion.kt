package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MacroTotals

object UnitConversion {
    private const val DEFAULT_SERVING_GRAMS = 100.0

    fun servingsFrom(quantity: Double, unit: FoodUnit, defaultServingGrams: Double = DEFAULT_SERVING_GRAMS): Double {
        val safeQuantity = EntryValidation.nonNegative(quantity)
        return when (unit) {
            FoodUnit.SERVING -> safeQuantity
            FoodUnit.GRAM -> safeQuantity / defaultServingGrams
        }
    }

    fun scaleMacros(
        foodItem: FoodItem,
        quantity: Double,
        unit: FoodUnit,
        defaultServingGrams: Double = DEFAULT_SERVING_GRAMS,
    ): MacroTotals {
        val servings = servingsFrom(quantity, unit, defaultServingGrams)
        val calories = EntryValidation.nonNegative(foodItem.caloriesKcal)
        val protein = EntryValidation.nonNegative(foodItem.proteinG)
        val carbs = EntryValidation.nonNegative(foodItem.carbsG)
        val fat = EntryValidation.nonNegative(foodItem.fatG)
        return MacroTotals(
            caloriesKcal = calories * servings,
            proteinG = protein * servings,
            carbsG = carbs * servings,
            fatG = fat * servings,
        )
    }
}
