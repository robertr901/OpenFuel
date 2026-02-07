package com.openfuel.app.data.repository

import com.openfuel.app.data.db.FoodDao
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
    private val foodDao: FoodDao? = null,
) : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) {
        if (!hasFoodReference(entry.foodItemId)) return
        mealEntryDao.upsertEntry(entry.toEntity())
    }

    override suspend fun updateMealEntry(entry: MealEntry) {
        if (!hasFoodReference(entry.foodItemId)) return
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

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> {
        val startInstant = startDate.atStartOfDay(zoneId).toInstant()
        val endExclusive = endDateInclusive.plusDays(1).atStartOfDay(zoneId).toInstant()
        return mealEntryDao.observeEntriesBetween(startInstant, endExclusive)
            .map { entries -> entries.map { it.toDomain() } }
    }

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        if (zoneId == ZoneId.systemDefault()) {
            return mealEntryDao.observeLoggedLocalDatesDesc()
                .map { dates -> dates.map { LocalDate.parse(it) } }
        }
        return mealEntryDao.observeEntryTimestampsDesc()
            .map { timestamps ->
                timestamps
                    .map { instant -> instant.atZone(zoneId).toLocalDate() }
                    .distinct()
            }
    }

    private suspend fun hasFoodReference(foodId: String): Boolean {
        val dao = foodDao ?: return true
        return dao.getFoodById(foodId) != null
    }
}
