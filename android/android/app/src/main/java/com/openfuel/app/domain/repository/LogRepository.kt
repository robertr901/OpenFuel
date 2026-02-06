package com.openfuel.app.domain.repository

import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun logMealEntry(entry: MealEntry)
    fun entriesForDate(date: LocalDate): Flow<List<MealEntryWithFood>>
}
