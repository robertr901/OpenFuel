package com.openfuel.app.domain.model

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACKS,
}

fun MealType.displayName(): String = when (this) {
    MealType.BREAKFAST -> "Breakfast"
    MealType.LUNCH -> "Lunch"
    MealType.DINNER -> "Dinner"
    MealType.SNACKS -> "Snacks"
}
