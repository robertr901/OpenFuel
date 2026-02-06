package com.openfuel.app.domain.repository

import com.openfuel.app.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    suspend fun upsertFood(foodItem: FoodItem)
    suspend fun getFoodById(id: String): FoodItem?
    suspend fun getFoodByBarcode(barcode: String): FoodItem?
    suspend fun setFavorite(foodId: String, isFavorite: Boolean)
    fun favoriteFoods(limit: Int): Flow<List<FoodItem>>
    fun allFoods(query: String): Flow<List<FoodItem>>
    fun recentFoods(limit: Int): Flow<List<FoodItem>>
    fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>>
}
