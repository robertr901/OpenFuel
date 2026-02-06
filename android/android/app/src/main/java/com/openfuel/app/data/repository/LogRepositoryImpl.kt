package com.openfuel.app.data.repository

import com.openfuel.app.data.db.MealEntryDao
import com.openfuel.app.data.mappers.toDomain
import com.openfuel.app.data.mappers.toEntity
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.repository.LogRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepositoryImpl(
    private val mealEntryDao: MealEntryDao,
) : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) {
        mealEntryDao.insertEntry(entry.toEntity())
    }

    override fun entriesForDate(date: LocalDate): Flow<List<MealEntryWithFood>> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return mealEntryDao.observeEntriesForDay(start, end)
            .map { entries -> entries.map { it.toDomain() } }
    }
}
