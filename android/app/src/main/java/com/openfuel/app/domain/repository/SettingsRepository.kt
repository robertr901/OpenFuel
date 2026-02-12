package com.openfuel.app.domain.repository

import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.GoalProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val onlineLookupEnabled: Flow<Boolean>
    val goalProfile: Flow<GoalProfile?>
    val goalProfileOverlays: Flow<Set<DietaryOverlay>>
    val goalProfileOnboardingCompleted: Flow<Boolean>
    val goalsCustomised: Flow<Boolean>
    val weeklyReviewDismissedWeekStartEpochDay: Flow<Long?>
    val fastLogReminderEnabled: Flow<Boolean>
    val fastLogReminderWindowStartHour: Flow<Int>
    val fastLogReminderWindowEndHour: Flow<Int>
    val fastLogQuietHoursEnabled: Flow<Boolean>
    val fastLogQuietHoursStartHour: Flow<Int>
    val fastLogQuietHoursEndHour: Flow<Int>
    val fastLogLastImpressionEpochDay: Flow<Long?>
    val fastLogImpressionCountForDay: Flow<Int>
    val fastLogConsecutiveDismissals: Flow<Int>
    val fastLogLastDismissedEpochDay: Flow<Long?>
    suspend fun setOnlineLookupEnabled(enabled: Boolean)
    suspend fun setGoalProfile(profile: GoalProfile?)
    suspend fun setGoalProfileOverlays(overlays: Set<DietaryOverlay>)
    suspend fun setGoalProfileOnboardingCompleted(completed: Boolean)
    suspend fun setGoalsCustomised(customised: Boolean)
    suspend fun setWeeklyReviewDismissedWeekStartEpochDay(epochDay: Long?)
    suspend fun setFastLogReminderEnabled(enabled: Boolean)
    suspend fun setFastLogReminderWindow(startHour: Int, endHour: Int)
    suspend fun setFastLogQuietHoursEnabled(enabled: Boolean)
    suspend fun setFastLogQuietHoursWindow(startHour: Int, endHour: Int)
    suspend fun setFastLogReminderImpression(epochDay: Long, countForDay: Int)
    suspend fun setFastLogDismissalState(consecutiveDismissals: Int, lastDismissedEpochDay: Long?)
    suspend fun resetFastLogDismissalState()
}
