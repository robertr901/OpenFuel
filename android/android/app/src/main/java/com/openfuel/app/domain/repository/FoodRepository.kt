package com.openfuel.app.domain.repository

import com.openfuel.app.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    suspend fun upsertFood(foodItem: FoodItem)
    suspend fun getFoodById(id: String): FoodItem?
    fun recentFoods(limit: Int): Flow<List<FoodItem>>
}
