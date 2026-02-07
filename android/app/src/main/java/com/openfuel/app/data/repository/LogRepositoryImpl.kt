package com.openfuel.app.data.repository

import com.openfuel.app.data.db.MealEntryDao
import com.openfuel.app.data.mappers.toDomain
import com.openfuel.app.data.mappers.toEntity
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.util.DayWindowCalculator
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepositoryImpl(
    private val mealEntryDao: MealEntryDao,
) : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) {
        mealEntryDao.upsertEntry(entry.toEntity())
    }

    override suspend fun updateMealEntry(entry: MealEntry) {
        mealEntryDao.upsertEntry(entry.toEntity())
    }

    override suspend fun deleteMealEntry(id: String) {
        mealEntryDao.deleteById(id)
    }

    override fun entriesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<MealEntryWithFood>> {
        val window = DayWindowCalculator.windowFor(date, zoneId)
        return mealEntryDao.observeEntriesForDay(window.startInclusive, window.endExclusive)
            .map { entries -> entries.map { it.toDomain() } }
    }

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        return mealEntryDao.observeEntryTimestampsDesc()
            .map { timestamps ->
                timestamps
                    .map { instant -> instant.atZone(zoneId).toLocalDate() }
                    .distinct()
            }
    }
}
