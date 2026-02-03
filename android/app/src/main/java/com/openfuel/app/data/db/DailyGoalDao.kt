package com.openfuel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun observeGoal(date: LocalDate): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals ORDER BY date DESC")
    suspend fun getAllGoals(): List<DailyGoalEntity>
}
