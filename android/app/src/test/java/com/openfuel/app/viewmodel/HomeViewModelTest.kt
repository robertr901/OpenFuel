package com.openfuel.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun goToPreviousAndNextDay_updatesSelectedDate() = runTest {
        val repository = FakeLogRepository()
        val viewModel = HomeViewModel(
            logRepository = repository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
        )

        val startDate = viewModel.selectedDate.value
        viewModel.goToPreviousDay()
        assertEquals(startDate.minusDays(1), viewModel.selectedDate.value)

        viewModel.goToNextDay()
        assertEquals(startDate, viewModel.selectedDate.value)
    }

    @Test
    fun selectedDateChange_requestsNewDateEntries() = runTest {
        val repository = FakeLogRepository()
        val viewModel = HomeViewModel(
            logRepository = repository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        val startDate = viewModel.selectedDate.value
        viewModel.goToNextDay()
        advanceUntilIdle()

        assertTrue(repository.requestedDates.contains(startDate))
        assertTrue(repository.requestedDates.contains(startDate.plusDays(1)))

        collectJob.cancel()
    }

    @Test
    fun updateEntry_withInvalidQuantity_skipsRepositoryUpdate() = runTest {
        val repository = FakeLogRepository()
        val viewModel = HomeViewModel(
            logRepository = repository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
        )

        viewModel.updateEntry(
            entry = sampleUiEntry(),
            quantity = -1.0,
            unit = FoodUnit.GRAM,
            mealType = MealType.DINNER,
        )
        advanceUntilIdle()

        assertNull(repository.updatedEntry)
    }

    @Test
    fun updateAndDeleteEntry_callRepositoryWithExpectedValues() = runTest {
        val repository = FakeLogRepository()
        val viewModel = HomeViewModel(
            logRepository = repository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
        )
        val entry = sampleUiEntry()

        viewModel.updateEntry(
            entry = entry,
            quantity = 2.5,
            unit = FoodUnit.GRAM,
            mealType = MealType.SNACKS,
        )
        viewModel.deleteEntry(entry.id)
        advanceUntilIdle()

        assertEquals(entry.id, repository.updatedEntry?.id)
        assertEquals(2.5, repository.updatedEntry?.quantity ?: 0.0, 0.0001)
        assertEquals(FoodUnit.GRAM, repository.updatedEntry?.unit)
        assertEquals(MealType.SNACKS, repository.updatedEntry?.mealType)
        assertEquals(entry.id, repository.deletedId)
    }

    @Test
    fun deleteEntry_whenRepositoryFails_exposesMessage() = runTest {
        val repository = FakeLogRepository(throwOnDelete = true)
        val viewModel = HomeViewModel(
            logRepository = repository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.deleteEntry("entry-1")
        advanceUntilIdle()

        assertEquals(
            "Could not delete entry. Please try again.",
            viewModel.uiState.value.message,
        )
        viewModel.consumeMessage()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.message)
        collectJob.cancel()
    }

    @Test
    fun savedStateHandleDate_isRestoredAtStartup() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("selectedDate" to "2026-02-05"))
        val viewModel = HomeViewModel(
            logRepository = FakeLogRepository(),
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = savedStateHandle,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(LocalDate.parse("2026-02-05"), viewModel.selectedDate.value)
    }

    private fun sampleUiEntry(): MealEntryUi {
        return MealEntryUi(
            id = "entry-1",
            foodId = "food-1",
            timestamp = Instant.parse("2024-01-01T12:00:00Z"),
            mealType = MealType.BREAKFAST,
            name = "Yogurt",
            quantity = 1.0,
            unit = FoodUnit.SERVING,
            caloriesKcal = 100.0,
        )
    }
}

private class FakeLogRepository(
    private val throwOnDelete: Boolean = false,
) : LogRepository {
    val requestedDates = mutableListOf<LocalDate>()
    var updatedEntry: MealEntry? = null
    var deletedId: String? = null

    override suspend fun logMealEntry(entry: MealEntry) {
        // no-op for tests
    }

    override suspend fun updateMealEntry(entry: MealEntry) {
        updatedEntry = entry
    }

    override suspend fun deleteMealEntry(id: String) {
        if (throwOnDelete) {
            throw IllegalStateException("delete failed")
        }
        deletedId = id
    }

    override fun entriesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<MealEntryWithFood>> {
        requestedDates += date
        return flowOf(emptyList())
    }

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> {
        return flowOf(emptyList())
    }

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        return flowOf(emptyList())
    }
}

private class FakeGoalsRepository : GoalsRepository {
    override fun goalForDate(date: LocalDate): Flow<DailyGoal?> {
        return flowOf(null)
    }

    override suspend fun upsertGoal(goal: DailyGoal) {
        // no-op for tests
    }
}
