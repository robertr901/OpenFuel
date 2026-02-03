package com.openfuel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MealEntryEntity)

    @Transaction
    @Query(
        """
        SELECT * FROM meal_entries
        WHERE timestamp >= :start AND timestamp < :end
        ORDER BY timestamp ASC
        """
    )
    fun observeEntriesForDay(
        start: Instant,
        end: Instant,
    ): Flow<List<MealEntryWithFoodEntity>>

    @Query("SELECT * FROM meal_entries ORDER BY timestamp DESC")
    suspend fun getAllEntries(): List<MealEntryEntity>
}
