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

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodItemEntity?

    @Query("UPDATE food_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query(
        """
        SELECT * FROM food_items
        WHERE isFavorite = 1
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeFavoriteFoods(limit: Int): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY createdAt DESC")
    fun observeAllFoods(): Flow<List<FoodItemEntity>>

    @Query(
        """
        SELECT * FROM food_items
        WHERE name LIKE '%' || :escapedQuery || '%' ESCAPE '\'
           OR IFNULL(brand, '') LIKE '%' || :escapedQuery || '%' ESCAPE '\'
        ORDER BY createdAt DESC
        """
    )
    fun observeAllFoodsBySearch(escapedQuery: String): Flow<List<FoodItemEntity>>

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
