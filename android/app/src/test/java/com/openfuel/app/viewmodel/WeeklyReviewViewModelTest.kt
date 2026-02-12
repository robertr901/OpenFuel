package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WeeklyReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_showsInsufficientData_whenLoggedDaysLessThanThree() = runTest {
        val zoneId = ZoneId.of("UTC")
        val clock = Clock.fixed(Instant.parse("2026-02-12T12:00:00Z"), ZoneOffset.UTC)
        val settingsRepository = FakeWeeklySettingsRepository()
        val viewModel = WeeklyReviewViewModel(
            logRepository = FakeWeeklyLogRepository(
                entries = listOf(
                    mealEntry(day = "2026-02-12", calories = 500.0, protein = 30.0, carbs = 50.0, fat = 15.0),
                    mealEntry(day = "2026-02-11", calories = 400.0, protein = 25.0, carbs = 45.0, fat = 12.0),
                ),
            ),
            settingsRepository = settingsRepository,
            goalsRepository = FakeWeeklyGoalsRepository(),
            zoneId = zoneId,
            clock = clock,
        )

        val state = viewModel.uiState.first { it.isEligible }

        assertTrue(state.isEligible)
        assertTrue(state.showInsufficientData)
        assertNull(state.suggestion)
    }

    @Test
    fun dismissSuggestionForCurrentWeek_hidesSuggestion_andPersistsWeekStart() = runTest {
        val zoneId = ZoneId.of("UTC")
        val clock = Clock.fixed(Instant.parse("2026-02-12T12:00:00Z"), ZoneOffset.UTC)
        val settingsRepository = FakeWeeklySettingsRepository().apply {
            goalProfileFlow.value = GoalProfile.MUSCLE_GAIN
        }
        val viewModel = WeeklyReviewViewModel(
            logRepository = FakeWeeklyLogRepository(
                entries = listOf(
                    mealEntry(day = "2026-02-12", calories = 400.0, protein = 70.0, carbs = 60.0, fat = 10.0),
                    mealEntry(day = "2026-02-11", calories = 500.0, protein = 80.0, carbs = 70.0, fat = 15.0),
                    mealEntry(day = "2026-02-10", calories = 550.0, protein = 75.0, carbs = 80.0, fat = 18.0),
                ),
            ),
            settingsRepository = settingsRepository,
            goalsRepository = FakeWeeklyGoalsRepository(
                goal = DailyGoal(
                    date = LocalDate.parse("2026-02-12"),
                    caloriesKcalTarget = 2200.0,
                    proteinGTarget = 180.0,
                    carbsGTarget = 250.0,
                    fatGTarget = 75.0,
                ),
            ),
            zoneId = zoneId,
            clock = clock,
        )

        assertNotNull(viewModel.uiState.first { it.suggestion != null }.suggestion)

        viewModel.dismissSuggestionForCurrentWeek()
        val dismissedState = viewModel.uiState.first { it.isSuggestionDismissedForCurrentWeek }

        assertNull(dismissedState.suggestion)
        assertEquals(LocalDate.parse("2026-02-09").toEpochDay(), settingsRepository.dismissedWeekStartEpochDayFlow.value)
    }

    @Test
    fun uiState_includesDataQualityNote_whenMissingDaysAndUnknownEntriesPresent() = runTest {
        val zoneId = ZoneId.of("UTC")
        val clock = Clock.fixed(Instant.parse("2026-02-12T12:00:00Z"), ZoneOffset.UTC)
        val viewModel = WeeklyReviewViewModel(
            logRepository = FakeWeeklyLogRepository(
                entries = listOf(
                    mealEntry(day = "2026-02-12", calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0),
                    mealEntry(day = "2026-02-10", calories = 600.0, protein = 30.0, carbs = 80.0, fat = 20.0),
                    mealEntry(day = "2026-02-09", calories = 700.0, protein = 40.0, carbs = 90.0, fat = 25.0),
                ),
            ),
            settingsRepository = FakeWeeklySettingsRepository(),
            goalsRepository = FakeWeeklyGoalsRepository(),
            zoneId = zoneId,
            clock = clock,
        )

        val state = viewModel.uiState.first { it.dataQualityNote != null }
        val note = state.dataQualityNote

        assertNotNull(note)
        assertTrue(note!!.contains("incomplete nutrition values"))
        assertFalse(state.showInsufficientData)
    }
}

private class FakeWeeklyLogRepository(
    private val entries: List<MealEntryWithFood>,
) : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) = Unit

    override suspend fun updateMealEntry(entry: MealEntry) = Unit

    override suspend fun deleteMealEntry(id: String) = Unit

    override fun entriesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<MealEntryWithFood>> {
        return flowOf(emptyList())
    }

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        return flowOf(emptyList())
    }

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> {
        return flowOf(entries)
    }
}

private class FakeWeeklyGoalsRepository(
    private val goal: DailyGoal? = null,
) : GoalsRepository {
    override fun goalForDate(date: LocalDate): Flow<DailyGoal?> {
        return flowOf(goal?.copy(date = date))
    }

    override suspend fun upsertGoal(goal: DailyGoal) = Unit
}

private class FakeWeeklySettingsRepository : SettingsRepository {
    val dismissedWeekStartEpochDayFlow = MutableStateFlow<Long?>(null)
    val goalProfileFlow = MutableStateFlow<GoalProfile?>(null)

    override val onlineLookupEnabled: Flow<Boolean> = flowOf(true)
    override val goalProfile: Flow<GoalProfile?> = goalProfileFlow
    override val goalProfileOverlays: Flow<Set<DietaryOverlay>> = flowOf(emptySet())
    override val goalProfileOnboardingCompleted: Flow<Boolean> = flowOf(false)
    override val goalsCustomised: Flow<Boolean> = flowOf(false)
    override val weeklyReviewDismissedWeekStartEpochDay: Flow<Long?> = dismissedWeekStartEpochDayFlow
    override val fastLogReminderEnabled: Flow<Boolean> = flowOf(true)
    override val fastLogReminderWindowStartHour: Flow<Int> = flowOf(7)
    override val fastLogReminderWindowEndHour: Flow<Int> = flowOf(21)
    override val fastLogQuietHoursEnabled: Flow<Boolean> = flowOf(true)
    override val fastLogQuietHoursStartHour: Flow<Int> = flowOf(21)
    override val fastLogQuietHoursEndHour: Flow<Int> = flowOf(7)
    override val fastLogLastImpressionEpochDay: Flow<Long?> = flowOf(null)
    override val fastLogImpressionCountForDay: Flow<Int> = flowOf(0)
    override val fastLogConsecutiveDismissals: Flow<Int> = flowOf(0)
    override val fastLogLastDismissedEpochDay: Flow<Long?> = flowOf(null)

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) = Unit

    override suspend fun setGoalProfile(profile: GoalProfile?) {
        goalProfileFlow.value = profile
    }

    override suspend fun setGoalProfileOverlays(overlays: Set<DietaryOverlay>) = Unit

    override suspend fun setGoalProfileOnboardingCompleted(completed: Boolean) = Unit

    override suspend fun setGoalsCustomised(customised: Boolean) = Unit

    override suspend fun setWeeklyReviewDismissedWeekStartEpochDay(epochDay: Long?) {
        dismissedWeekStartEpochDayFlow.value = epochDay
    }

    override suspend fun setFastLogReminderEnabled(enabled: Boolean) = Unit

    override suspend fun setFastLogReminderWindow(startHour: Int, endHour: Int) = Unit

    override suspend fun setFastLogQuietHoursEnabled(enabled: Boolean) = Unit

    override suspend fun setFastLogQuietHoursWindow(startHour: Int, endHour: Int) = Unit

    override suspend fun setFastLogReminderImpression(epochDay: Long, countForDay: Int) = Unit

    override suspend fun setFastLogDismissalState(consecutiveDismissals: Int, lastDismissedEpochDay: Long?) = Unit

    override suspend fun resetFastLogDismissalState() = Unit
}

private fun mealEntry(
    day: String,
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
): MealEntryWithFood {
    val zoneId = ZoneId.of("UTC")
    val date = LocalDate.parse(day)
    val timestamp = date.atStartOfDay(zoneId).plusHours(12).toInstant()
    return MealEntryWithFood(
        entry = MealEntry(
            id = "entry-$day",
            timestamp = timestamp,
            mealType = MealType.LUNCH,
            foodItemId = "food-$day",
            quantity = 1.0,
            unit = FoodUnit.SERVING,
        ),
        food = FoodItem(
            id = "food-$day",
            name = "Food $day",
            brand = null,
            caloriesKcal = calories,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            createdAt = Instant.EPOCH,
        ),
    )
}
