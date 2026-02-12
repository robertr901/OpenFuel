package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.GoalProfileDefaults
import com.openfuel.app.domain.model.WeeklyReviewSuggestion
import com.openfuel.app.domain.model.WeeklyReviewSummary
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.util.WeeklyReviewCalculator
import com.openfuel.app.domain.util.WeeklyReviewSuggestionEngine
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeeklyReviewViewModel(
    private val logRepository: LogRepository,
    private val settingsRepository: SettingsRepository,
    private val goalsRepository: GoalsRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.system(zoneId),
) : ViewModel() {
    private val today: LocalDate = LocalDate.now(clock.withZone(zoneId))
    private val currentWeekStart: LocalDate = weekStartFor(today)
    private val currentWeekStartEpochDay: Long = currentWeekStart.toEpochDay()

    val uiState: StateFlow<WeeklyReviewUiState> = combine(
        logRepository.entriesInRange(
            startDate = today.minusDays(6),
            endDateInclusive = today,
            zoneId = zoneId,
        ),
        goalsRepository.goalForDate(today),
        settingsRepository.goalProfile,
        settingsRepository.weeklyReviewDismissedWeekStartEpochDay,
    ) { entries, goal, goalProfile, dismissedWeekStartEpochDay ->
        val summary = WeeklyReviewCalculator.buildLast7DaySummary(
            entries = entries,
            today = today,
            zoneId = zoneId,
        )
        val emphasis = goalProfile?.let(GoalProfileDefaults::emphasisFor)
        val suggestion = WeeklyReviewSuggestionEngine.suggest(
            summary = summary,
            goal = goal,
            emphasis = emphasis,
        )
        val isSuggestionDismissedForCurrentWeek = dismissedWeekStartEpochDay == currentWeekStartEpochDay
        val isEligible = summary.loggedDays > 0
        WeeklyReviewUiState(
            today = today,
            weekStart = currentWeekStart,
            summary = summary,
            isEligible = isEligible,
            showInsufficientData = isEligible && summary.loggedDays < MINIMUM_LOGGED_DAYS_FOR_ACTION,
            suggestion = if (isSuggestionDismissedForCurrentWeek) null else suggestion,
            isSuggestionDismissedForCurrentWeek = isSuggestionDismissedForCurrentWeek,
            dataQualityNote = buildDataQualityNote(summary),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeeklyReviewUiState(
            today = today,
            weekStart = currentWeekStart,
            summary = WeeklyReviewSummary(
                startDate = today.minusDays(6),
                endDate = today,
                loggedDays = 0,
                missingDays = 7,
                unknownEntryCount = 0,
                total = com.openfuel.app.domain.model.MacroTotals.Zero,
                average = com.openfuel.app.domain.model.MacroTotals.Zero,
            ),
            isEligible = false,
            showInsufficientData = false,
            suggestion = null,
            isSuggestionDismissedForCurrentWeek = false,
            dataQualityNote = null,
        ),
    )

    fun dismissSuggestionForCurrentWeek() {
        viewModelScope.launch {
            settingsRepository.setWeeklyReviewDismissedWeekStartEpochDay(currentWeekStartEpochDay)
        }
    }

    private fun buildDataQualityNote(summary: WeeklyReviewSummary): String? {
        return when {
            summary.loggedDays == 0 -> null
            summary.unknownEntryCount > 0 && summary.missingDays > 0 ->
                "Based on ${summary.loggedDays} of last 7 days. ${summary.unknownEntryCount} entries had incomplete nutrition values."
            summary.unknownEntryCount > 0 ->
                "${summary.unknownEntryCount} entries had incomplete nutrition values."
            summary.missingDays > 0 ->
                "Based on ${summary.loggedDays} of last 7 days."
            else -> null
        }
    }

    private fun weekStartFor(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    private companion object {
        const val MINIMUM_LOGGED_DAYS_FOR_ACTION = 3
    }
}

data class WeeklyReviewUiState(
    val today: LocalDate,
    val weekStart: LocalDate,
    val summary: WeeklyReviewSummary,
    val isEligible: Boolean,
    val showInsufficientData: Boolean,
    val suggestion: WeeklyReviewSuggestion?,
    val isSuggestionDismissedForCurrentWeek: Boolean,
    val dataQualityNote: String?,
)
