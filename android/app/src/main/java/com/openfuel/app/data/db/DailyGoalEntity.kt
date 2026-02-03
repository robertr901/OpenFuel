package com.openfuel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val date: LocalDate,
    val caloriesKcalTarget: Double,
    val proteinGTarget: Double,
    val carbsGTarget: Double,
    val fatGTarget: Double,
)
