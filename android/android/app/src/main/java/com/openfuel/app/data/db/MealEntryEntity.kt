package com.openfuel.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import java.time.Instant

@Entity(
    tableName = "meal_entries",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["foodItemId"]),
    ],
)
data class MealEntryEntity(
    @PrimaryKey val id: String,
    val timestamp: Instant,
    val mealType: MealType,
    val foodItemId: String,
    val quantity: Double,
    val unit: FoodUnit,
)
