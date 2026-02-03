package com.openfuel.app.data.repository

import com.openfuel.app.data.db.DailyGoalDao
import com.openfuel.app.data.mappers.toDomain
import com.openfuel.app.data.mappers.toEntity
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.repository.GoalsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalsRepositoryImpl(
    private val dailyGoalDao: DailyGoalDao,
) : GoalsRepository {
    override fun goalForDate(date: LocalDate): Flow<DailyGoal?> {
        return dailyGoalDao.observeGoal(date).map { it?.toDomain() }
    }

    override suspend fun upsertGoal(goal: DailyGoal) {
        dailyGoalDao.upsertGoal(goal.toEntity())
    }
}
