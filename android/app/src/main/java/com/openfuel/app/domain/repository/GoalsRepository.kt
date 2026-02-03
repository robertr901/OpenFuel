package com.openfuel.app.domain.repository

import com.openfuel.app.domain.model.DailyGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface GoalsRepository {
    fun goalForDate(date: LocalDate): Flow<DailyGoal?>
    suspend fun upsertGoal(goal: DailyGoal)
}
