package com.openfuel.app.data.repository

import com.openfuel.app.data.db.MealEntryDao
import com.openfuel.app.data.db.MealEntryEntity
import com.openfuel.app.data.db.MealEntryWithFoodEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogRepositoryImplTest {
    @Test
    fun loggedDates_deduplicatesByLocalDate_inDescendingOrder() = runTest {
        val dao = FakeMealEntryDao()
        val repository = LogRepositoryImpl(dao)

        dao.timestamps.value = listOf(
            Instant.parse("2026-03-03T10:00:00Z"),
            Instant.parse("2026-03-03T08:00:00Z"),
            Instant.parse("2026-03-02T23:00:00Z"),
            Instant.parse("2026-03-01T23:00:00Z"),
        )

        val dates = repository.loggedDates(ZoneId.of("UTC")).first()

        assertEquals(
            listOf(
                LocalDate.parse("2026-03-03"),
                LocalDate.parse("2026-03-02"),
                LocalDate.parse("2026-03-01"),
            ),
            dates,
        )
    }

    @Test
    fun loggedDates_usesSqlAggregatedDates_forSystemZone() = runTest {
        val dao = FakeMealEntryDao()
        val repository = LogRepositoryImpl(dao)

        dao.loggedDateStrings.value = listOf("2026-03-04", "2026-03-03")

        val dates = repository.loggedDates(ZoneId.systemDefault()).first()

        assertEquals(
            listOf(
                LocalDate.parse("2026-03-04"),
                LocalDate.parse("2026-03-03"),
            ),
            dates,
        )
    }
}

private class FakeMealEntryDao : MealEntryDao {
    val timestamps = MutableStateFlow<List<Instant>>(emptyList())
    val loggedDateStrings = MutableStateFlow<List<String>>(emptyList())

    override suspend fun upsertEntry(entry: MealEntryEntity) {
        // no-op
    }

    override suspend fun deleteById(id: String) {
        // no-op
    }

    override fun observeEntriesForDay(
        start: Instant,
        end: Instant,
    ): Flow<List<MealEntryWithFoodEntity>> {
        return flowOf(emptyList())
    }

    override fun observeEntriesBetween(
        start: Instant,
        end: Instant,
    ): Flow<List<MealEntryWithFoodEntity>> {
        return flowOf(emptyList())
    }

    override fun observeEntryTimestampsDesc(): Flow<List<Instant>> {
        return timestamps
    }

    override fun observeLoggedLocalDatesDesc(): Flow<List<String>> {
        return loggedDateStrings
    }

    override suspend fun getAllEntries(): List<MealEntryEntity> {
        return emptyList()
    }
}
