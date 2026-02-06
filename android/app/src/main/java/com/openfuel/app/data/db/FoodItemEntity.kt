package com.openfuel.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["brand"]),
        Index(value = ["name", "brand"]),
    ],
)
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val createdAt: Instant,
)
