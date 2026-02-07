package com.openfuel.app.domain.model

import java.time.Instant

data class FoodItem(
    val id: String,
    val name: String,
    val brand: String?,
    val barcode: String? = null,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val isFavorite: Boolean = false,
    val isReportedIncorrect: Boolean = false,
    val createdAt: Instant,
)
