package com.openfuel.app.domain.util

object GoalValidation {
    const val MAX_CALORIES_KCAL = 10_000.0
    const val MAX_MACRO_GRAMS = 1_000.0

    fun isValidCalories(value: Double): Boolean {
        return value.isFinite() && value >= 0.0 && value <= MAX_CALORIES_KCAL
    }

    fun isValidMacro(value: Double): Boolean {
        return value.isFinite() && value >= 0.0 && value <= MAX_MACRO_GRAMS
    }
}
