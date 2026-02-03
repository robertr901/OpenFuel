package com.openfuel.app.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class MealEntryWithFoodEntity(
    @Embedded val entry: MealEntryEntity,
    @Relation(
        parentColumn = "foodItemId",
        entityColumn = "id",
    )
    val food: FoodItemEntity,
)
