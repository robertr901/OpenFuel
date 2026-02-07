package com.openfuel.app.domain.repository

import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun logMealEntry(entry: MealEntry)
    suspend fun updateMealEntry(entry: MealEntry)
    suspend fun deleteMealEntry(id: String)
    fun entriesForDate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<MealEntryWithFood>>
    fun loggedDates(
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<LocalDate>>
    fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<MealEntryWithFood>>
}
