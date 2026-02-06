package com.openfuel.app.domain.model

import java.time.Instant

data class MealEntry(
    val id: String,
    val timestamp: Instant,
    val mealType: MealType,
    val foodItemId: String,
    val quantity: Double,
    val unit: FoodUnit,
)
