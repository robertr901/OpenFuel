package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.analytics.AnalyticsService
import com.openfuel.app.domain.analytics.NoOpAnalyticsService
import com.openfuel.app.domain.analytics.ProductEventName
import com.openfuel.app.domain.retention.FastLogReminderContext
import com.openfuel.app.domain.retention.FastLogReminderSettings
import com.openfuel.app.domain.retention.RetentionPolicy
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.util.EntryValidation
import com.openfuel.app.domain.util.MealTotalsCalculator
import com.openfuel.app.domain.util.UnitConversion
import java.time.Instant
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val logRepository: LogRepository,
    private val settingsRepository: SettingsRepository,
    private val goalsRepository: GoalsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val analyticsService: AnalyticsService = NoOpAnalyticsService,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.system(zoneId),
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        parseDate(savedStateHandle.get<String>(SELECTED_DATE_KEY))
            ?: LocalDate.now(zoneId),
    )
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    private val message = MutableStateFlow<String?>(null)
    private val fastLogReminderConsumedInSession = MutableStateFlow(false)
    private val fastLogReminderVisibleInSession = MutableStateFlow(false)

    private val baseUiState: StateFlow<HomeUiState> = selectedDate
        .flatMapLatest { date ->
            combine(
                logRepository.entriesForDate(date, zoneId),
                goalsRepository.goalForDate(date),
            ) { entries, goal ->
                buildUiState(date, entries, goal)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                date = _selectedDate.value,
                totals = MacroTotals.Zero,
                goal = null,
                meals = MealType.values().map { mealType ->
                    MealSectionUi(mealType = mealType, entries = emptyList(), totals = MacroTotals.Zero)
                },
                message = null,
            ),
        )

    val uiState: StateFlow<HomeUiState> = combine(
        baseUiState,
        message,
        settingsRepository.fastLogReminderEnabled,
        settingsRepository.fastLogReminderWindowStartHour,
        settingsRepository.fastLogReminderWindowEndHour,
        settingsRepository.fastLogQuietHoursEnabled,
        settingsRepository.fastLogQuietHoursStartHour,
        settingsRepository.fastLogQuietHoursEndHour,
        settingsRepository.fastLogLastImpressionEpochDay,
        settingsRepository.fastLogImpressionCountForDay,
        settingsRepository.fastLogConsecutiveDismissals,
        settingsRepository.fastLogLastDismissedEpochDay,
        fastLogReminderConsumedInSession,
        fastLogReminderVisibleInSession,
    ) { values ->
        val baseState = values[0] as HomeUiState
        val currentMessage = values[1] as String?
        val reminderEnabled = values[2] as Boolean
        val reminderWindowStartHour = values[3] as Int
        val reminderWindowEndHour = values[4] as Int
        val quietHoursEnabled = values[5] as Boolean
        val quietHoursStartHour = values[6] as Int
        val quietHoursEndHour = values[7] as Int
        val lastImpressionEpochDay = values[8] as Long?
        val impressionCountForDay = values[9] as Int
        val consecutiveDismissals = values[10] as Int
        val lastDismissedEpochDay = values[11] as Long?
        val reminderConsumed = values[12] as Boolean
        val reminderVisible = values[13] as Boolean

        val now = LocalDateTime.now(clock.withZone(zoneId))
        val today = now.toLocalDate()
        val hasEntriesForSelectedDay = baseState.meals.any { meal -> meal.entries.isNotEmpty() }
        val canRenderReminder = baseState.date == today
        val impressionsToday = if (lastImpressionEpochDay == today.toEpochDay()) {
            impressionCountForDay
        } else {
            0
        }
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = FastLogReminderSettings(
                enabled = reminderEnabled,
                reminderWindowStartHour = reminderWindowStartHour,
                reminderWindowEndHour = reminderWindowEndHour,
                quietHoursEnabled = quietHoursEnabled,
                quietHoursStartHour = quietHoursStartHour,
                quietHoursEndHour = quietHoursEndHour,
            ),
            context = FastLogReminderContext(
                now = now,
                hasLoggedToday = hasEntriesForSelectedDay,
                impressionsToday = impressionsToday,
                impressionsThisSession = if (reminderConsumed) 1 else 0,
                consecutiveDismissals = consecutiveDismissals,
                lastDismissedDate = lastDismissedEpochDay?.let(LocalDate::ofEpochDay),
            ),
        )

        baseState.copy(
            message = currentMessage,
            showFastLogReminder = canRenderReminder &&
                (reminderVisible || (evaluation.shouldShow && !reminderConsumed)),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = baseUiState.value,
    )

    fun goToPreviousDay() {
        selectDate(_selectedDate.value.minusDays(1))
    }

    fun goToNextDay() {
        selectDate(_selectedDate.value.plusDays(1))
    }

    fun applyNavigationDate(rawDate: String?) {
        val parsedDate = parseDate(rawDate) ?: return
        selectDate(parsedDate)
    }

    fun updateEntry(
        entry: MealEntryUi,
        quantity: Double,
        unit: FoodUnit,
        mealType: MealType,
    ) {
        if (!EntryValidation.isValidQuantity(quantity)) {
            return
        }
        viewModelScope.launch {
            try {
                logRepository.updateMealEntry(
                    MealEntry(
                        id = entry.id,
                        timestamp = entry.timestamp,
                        mealType = mealType,
                        foodItemId = entry.foodId,
                        quantity = quantity,
                        unit = unit,
                    ),
                )
            } catch (_: Exception) {
                message.value = "Could not update entry. Please try again."
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                logRepository.deleteMealEntry(entryId)
            } catch (_: Exception) {
                message.value = "Could not delete entry. Please try again."
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    fun onFastLogReminderShown() {
        if (fastLogReminderConsumedInSession.value) return
        viewModelScope.launch {
            val todayEpochDay = LocalDate.now(clock.withZone(zoneId)).toEpochDay()
            val lastImpressionEpochDay = settingsRepository.fastLogLastImpressionEpochDay.first()
            val existingCount = settingsRepository.fastLogImpressionCountForDay.first()
            val nextCount = if (lastImpressionEpochDay == todayEpochDay) {
                existingCount + 1
            } else {
                1
            }
            settingsRepository.setFastLogReminderImpression(
                epochDay = todayEpochDay,
                countForDay = nextCount,
            )
            analyticsService.track(
                ProductEventName.RETENTION_FASTLOG_REMINDER_SHOWN,
                mapOf(
                    "screen" to "today",
                    "surface" to "home_card",
                    "reminder_state" to "shown",
                ),
            )
            fastLogReminderConsumedInSession.value = true
            fastLogReminderVisibleInSession.value = true
        }
    }

    fun dismissFastLogReminder() {
        viewModelScope.launch {
            val todayEpochDay = LocalDate.now(clock.withZone(zoneId)).toEpochDay()
            val dismissals = settingsRepository.fastLogConsecutiveDismissals.first()
            settingsRepository.setFastLogDismissalState(
                consecutiveDismissals = dismissals + 1,
                lastDismissedEpochDay = todayEpochDay,
            )
            analyticsService.track(
                ProductEventName.RETENTION_FASTLOG_REMINDER_DISMISSED,
                mapOf(
                    "screen" to "today",
                    "surface" to "home_card",
                    "reminder_state" to "dismissed",
                ),
            )
            fastLogReminderVisibleInSession.value = false
        }
    }

    fun onFastLogReminderActioned() {
        viewModelScope.launch {
            settingsRepository.resetFastLogDismissalState()
            analyticsService.track(
                ProductEventName.RETENTION_FASTLOG_REMINDER_ACTED,
                mapOf(
                    "screen" to "today",
                    "surface" to "home_card",
                    "reminder_state" to "acted",
                ),
            )
            fastLogReminderVisibleInSession.value = false
        }
    }

    private fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) {
            return
        }
        _selectedDate.value = date
        savedStateHandle[SELECTED_DATE_KEY] = date.toString()
    }

    private fun buildUiState(
        date: LocalDate,
        entries: List<MealEntryWithFood>,
        goal: DailyGoal?,
    ): HomeUiState {
        val totals = MealTotalsCalculator.totalsFor(entries).totals
        val grouped = entries.groupBy { it.entry.mealType }
        val meals = MealType.values().map { mealType ->
            val mealEntries = grouped[mealType].orEmpty()
            val mealTotals = MealTotalsCalculator.totalsFor(mealEntries).totals
            MealSectionUi(
                mealType = mealType,
                entries = mealEntries.map { entryWithFood ->
                    val macros = UnitConversion.scaleMacros(
                        foodItem = entryWithFood.food,
                        quantity = entryWithFood.entry.quantity,
                        unit = entryWithFood.entry.unit,
                    )
                    MealEntryUi(
                        id = entryWithFood.entry.id,
                        foodId = entryWithFood.entry.foodItemId,
                        timestamp = entryWithFood.entry.timestamp,
                        mealType = entryWithFood.entry.mealType,
                        name = entryWithFood.food.name,
                        quantity = entryWithFood.entry.quantity,
                        unit = entryWithFood.entry.unit,
                        caloriesKcal = macros.caloriesKcal,
                    )
                },
                totals = mealTotals,
            )
        }
        return HomeUiState(
            date = date,
            totals = totals,
            goal = goal,
            meals = meals,
            message = null,
        )
    }

    private companion object {
        private const val SELECTED_DATE_KEY = "selectedDate"

        private fun parseDate(rawDate: String?): LocalDate? {
            if (rawDate.isNullOrBlank()) return null
            return runCatching { LocalDate.parse(rawDate) }.getOrNull()
        }
    }
}

data class HomeUiState(
    val date: LocalDate,
    val totals: MacroTotals,
    val goal: DailyGoal?,
    val meals: List<MealSectionUi>,
    val showFastLogReminder: Boolean = false,
    val message: String? = null,
)

data class MealSectionUi(
    val mealType: MealType,
    val entries: List<MealEntryUi>,
    val totals: MacroTotals,
)

data class MealEntryUi(
    val id: String,
    val foodId: String,
    val timestamp: Instant,
    val mealType: MealType,
    val name: String,
    val quantity: Double,
    val unit: FoodUnit,
    val caloriesKcal: Double,
)
