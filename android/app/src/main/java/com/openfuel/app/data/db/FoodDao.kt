package com.openfuel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(foodItem: FoodItemEntity)

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_items ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentFoods(limit: Int): Flow<List<FoodItemEntity>>

    @Query(
        """
        SELECT * FROM food_items
        WHERE name LIKE '%' || :escapedQuery || '%' ESCAPE '\'
           OR IFNULL(brand, '') LIKE '%' || :escapedQuery || '%' ESCAPE '\'
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeFoodsBySearch(escapedQuery: String, limit: Int): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY createdAt DESC")
    suspend fun getAllFoods(): List<FoodItemEntity>
}
