package com.openfuel.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.analytics.AnalyticsService
import com.openfuel.app.domain.analytics.ProductEvent
import com.openfuel.app.domain.analytics.ProductEventName
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import java.time.Instant
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            settingsRepository = FakeHomeSettingsRepository(),
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
            settingsRepository = FakeHomeSettingsRepository(),
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
            settingsRepository = FakeHomeSettingsRepository(),
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
            settingsRepository = FakeHomeSettingsRepository(),
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
            settingsRepository = FakeHomeSettingsRepository(),
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
            settingsRepository = FakeHomeSettingsRepository(),
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = savedStateHandle,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(LocalDate.parse("2026-02-05"), viewModel.selectedDate.value)
    }

    @Test
    fun fastLogReminder_showsForEmptyToday_andDismissesDeterministically() = runTest {
        val settingsRepository = FakeHomeSettingsRepository()
        val viewModel = HomeViewModel(
            logRepository = FakeLogRepository(),
            settingsRepository = settingsRepository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
            clock = Clock.fixed(Instant.parse("2026-02-11T12:00:00Z"), ZoneOffset.UTC),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showFastLogReminder)

        viewModel.onFastLogReminderShown()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showFastLogReminder)

        viewModel.dismissFastLogReminder()
        advanceUntilIdle()
        assertTrue(settingsRepository.dismissalCount > 0)
        assertTrue(!viewModel.uiState.value.showFastLogReminder)
        collectJob.cancel()
    }

    @Test
    fun fastLogReminder_hiddenWhenMealAlreadyLoggedToday() = runTest {
        val viewModel = HomeViewModel(
            logRepository = FakeLogRepository(entries = listOf(sampleEntryWithFood())),
            settingsRepository = FakeHomeSettingsRepository(),
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            zoneId = ZoneId.of("UTC"),
            clock = Clock.fixed(Instant.parse("2026-02-11T12:00:00Z"), ZoneOffset.UTC),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.showFastLogReminder)
        collectJob.cancel()
    }

    @Test
    fun fastLogReminder_tracksShownDismissedAndActedEvents() = runTest {
        val analytics = FakeAnalyticsService()
        val settingsRepository = FakeHomeSettingsRepository()
        val viewModel = HomeViewModel(
            logRepository = FakeLogRepository(),
            settingsRepository = settingsRepository,
            goalsRepository = FakeGoalsRepository(),
            savedStateHandle = SavedStateHandle(),
            analyticsService = analytics,
            zoneId = ZoneId.of("UTC"),
            clock = Clock.fixed(Instant.parse("2026-02-11T12:00:00Z"), ZoneOffset.UTC),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        viewModel.onFastLogReminderShown()
        advanceUntilIdle()
        viewModel.dismissFastLogReminder()
        advanceUntilIdle()
        viewModel.onFastLogReminderActioned()
        advanceUntilIdle()

        assertEquals(
            listOf(
                ProductEventName.RETENTION_FASTLOG_REMINDER_SHOWN,
                ProductEventName.RETENTION_FASTLOG_REMINDER_DISMISSED,
                ProductEventName.RETENTION_FASTLOG_REMINDER_ACTED,
            ),
            analytics.events.value.map { it.name },
        )
        collectJob.cancel()
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

    private fun sampleEntryWithFood(): MealEntryWithFood {
        return MealEntryWithFood(
            entry = MealEntry(
                id = "entry-1",
                timestamp = Instant.parse("2026-02-11T08:00:00Z"),
                mealType = MealType.BREAKFAST,
                foodItemId = "food-1",
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            ),
            food = FoodItem(
                id = "food-1",
                name = "Oatmeal",
                brand = null,
                caloriesKcal = 150.0,
                proteinG = 5.0,
                carbsG = 27.0,
                fatG = 3.0,
                createdAt = Instant.parse("2026-02-10T10:00:00Z"),
            ),
        )
    }
}

private class FakeLogRepository(
    private val throwOnDelete: Boolean = false,
    private val entries: List<MealEntryWithFood> = emptyList(),
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
        return flowOf(entries)
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

private class FakeHomeSettingsRepository : SettingsRepository {
    private val onlineLookupEnabledFlow = MutableStateFlow(true)
    private val goalProfileFlow = MutableStateFlow<GoalProfile?>(null)
    private val goalProfileOverlaysFlow = MutableStateFlow<Set<DietaryOverlay>>(emptySet())
    private val goalProfileOnboardingCompletedFlow = MutableStateFlow(false)
    private val goalsCustomisedFlow = MutableStateFlow(false)
    private val fastLogReminderEnabledFlow = MutableStateFlow(true)
    private val fastLogReminderWindowStartHourFlow = MutableStateFlow(7)
    private val fastLogReminderWindowEndHourFlow = MutableStateFlow(21)
    private val fastLogQuietHoursEnabledFlow = MutableStateFlow(true)
    private val fastLogQuietHoursStartHourFlow = MutableStateFlow(21)
    private val fastLogQuietHoursEndHourFlow = MutableStateFlow(7)
    private val fastLogLastImpressionEpochDayFlow = MutableStateFlow<Long?>(null)
    private val fastLogImpressionCountForDayFlow = MutableStateFlow(0)
    private val fastLogConsecutiveDismissalsFlow = MutableStateFlow(0)
    private val fastLogLastDismissedEpochDayFlow = MutableStateFlow<Long?>(null)
    var dismissalCount: Int = 0
        private set

    override val onlineLookupEnabled: Flow<Boolean> = onlineLookupEnabledFlow
    override val goalProfile: Flow<GoalProfile?> = goalProfileFlow
    override val goalProfileOverlays: Flow<Set<DietaryOverlay>> = goalProfileOverlaysFlow
    override val goalProfileOnboardingCompleted: Flow<Boolean> = goalProfileOnboardingCompletedFlow
    override val goalsCustomised: Flow<Boolean> = goalsCustomisedFlow
    override val fastLogReminderEnabled: Flow<Boolean> = fastLogReminderEnabledFlow
    override val fastLogReminderWindowStartHour: Flow<Int> = fastLogReminderWindowStartHourFlow
    override val fastLogReminderWindowEndHour: Flow<Int> = fastLogReminderWindowEndHourFlow
    override val fastLogQuietHoursEnabled: Flow<Boolean> = fastLogQuietHoursEnabledFlow
    override val fastLogQuietHoursStartHour: Flow<Int> = fastLogQuietHoursStartHourFlow
    override val fastLogQuietHoursEndHour: Flow<Int> = fastLogQuietHoursEndHourFlow
    override val fastLogLastImpressionEpochDay: Flow<Long?> = fastLogLastImpressionEpochDayFlow
    override val fastLogImpressionCountForDay: Flow<Int> = fastLogImpressionCountForDayFlow
    override val fastLogConsecutiveDismissals: Flow<Int> = fastLogConsecutiveDismissalsFlow
    override val fastLogLastDismissedEpochDay: Flow<Long?> = fastLogLastDismissedEpochDayFlow

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        onlineLookupEnabledFlow.value = enabled
    }

    override suspend fun setGoalProfile(profile: GoalProfile?) {
        goalProfileFlow.value = profile
    }

    override suspend fun setGoalProfileOverlays(overlays: Set<DietaryOverlay>) {
        goalProfileOverlaysFlow.value = overlays
    }

    override suspend fun setGoalProfileOnboardingCompleted(completed: Boolean) {
        goalProfileOnboardingCompletedFlow.value = completed
    }

    override suspend fun setGoalsCustomised(customised: Boolean) {
        goalsCustomisedFlow.value = customised
    }

    override suspend fun setFastLogReminderEnabled(enabled: Boolean) {
        fastLogReminderEnabledFlow.value = enabled
    }

    override suspend fun setFastLogReminderWindow(startHour: Int, endHour: Int) {
        fastLogReminderWindowStartHourFlow.value = startHour
        fastLogReminderWindowEndHourFlow.value = endHour
    }

    override suspend fun setFastLogQuietHoursEnabled(enabled: Boolean) {
        fastLogQuietHoursEnabledFlow.value = enabled
    }

    override suspend fun setFastLogQuietHoursWindow(startHour: Int, endHour: Int) {
        fastLogQuietHoursStartHourFlow.value = startHour
        fastLogQuietHoursEndHourFlow.value = endHour
    }

    override suspend fun setFastLogReminderImpression(epochDay: Long, countForDay: Int) {
        fastLogLastImpressionEpochDayFlow.value = epochDay
        fastLogImpressionCountForDayFlow.value = countForDay
    }

    override suspend fun setFastLogDismissalState(consecutiveDismissals: Int, lastDismissedEpochDay: Long?) {
        fastLogConsecutiveDismissalsFlow.value = consecutiveDismissals
        fastLogLastDismissedEpochDayFlow.value = lastDismissedEpochDay
        dismissalCount = consecutiveDismissals
    }

    override suspend fun resetFastLogDismissalState() {
        fastLogConsecutiveDismissalsFlow.value = 0
        fastLogLastDismissedEpochDayFlow.value = null
    }
}

private class FakeAnalyticsService : AnalyticsService {
    private val _events = MutableStateFlow<List<ProductEvent>>(emptyList())
    override val events: StateFlow<List<ProductEvent>> = _events

    override fun track(
        name: ProductEventName,
        properties: Map<String, String>,
    ) {
        _events.value = _events.value + ProductEvent(
            name = name,
            properties = properties,
            occurredAtEpochMs = 0L,
        )
    }
}
