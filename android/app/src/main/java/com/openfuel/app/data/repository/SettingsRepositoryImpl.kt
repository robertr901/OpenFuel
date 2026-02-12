package com.openfuel.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.openfuel.app.data.datastore.SettingsKeys
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    private companion object {
        const val DEFAULT_FAST_LOG_REMINDER_ENABLED = true
        const val DEFAULT_FAST_LOG_WINDOW_START_HOUR = 7
        const val DEFAULT_FAST_LOG_WINDOW_END_HOUR = 21
        const val DEFAULT_FAST_LOG_QUIET_HOURS_ENABLED = true
        const val DEFAULT_FAST_LOG_QUIET_HOURS_START_HOUR = 21
        const val DEFAULT_FAST_LOG_QUIET_HOURS_END_HOUR = 7
    }

    override val onlineLookupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] ?: true }

    override val goalProfile: Flow<GoalProfile?> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.GOAL_PROFILE]
                ?.let(::goalProfileFromStoredValue)
        }

    override val goalProfileOverlays: Flow<Set<DietaryOverlay>> = dataStore.data
        .map { preferences ->
            val stored = preferences[SettingsKeys.GOAL_PROFILE_OVERLAYS].orEmpty()
            stored.mapNotNull(::dietaryOverlayFromStoredValue).toSet()
        }

    override val goalProfileOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.GOAL_PROFILE_ONBOARDING_COMPLETED] ?: false
        }

    override val goalsCustomised: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.GOALS_CUSTOMISED] ?: false
        }

    override val weeklyReviewDismissedWeekStartEpochDay: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.WEEKLY_REVIEW_DISMISSED_WEEK_START_EPOCH_DAY]
        }

    override val fastLogReminderEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.FAST_LOG_REMINDER_ENABLED] ?: DEFAULT_FAST_LOG_REMINDER_ENABLED
        }

    override val fastLogReminderWindowStartHour: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.FAST_LOG_REMINDER_WINDOW_START_HOUR] ?: DEFAULT_FAST_LOG_WINDOW_START_HOUR
        }

    override val fastLogReminderWindowEndHour: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.FAST_LOG_REMINDER_WINDOW_END_HOUR] ?: DEFAULT_FAST_LOG_WINDOW_END_HOUR
        }

    override val fastLogQuietHoursEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.FAST_LOG_QUIET_HOURS_ENABLED] ?: DEFAULT_FAST_LOG_QUIET_HOURS_ENABLED
        }

    override val fastLogQuietHoursStartHour: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.FAST_LOG_QUIET_HOURS_START_HOUR] ?: DEFAULT_FAST_LOG_QUIET_HOURS_START_HOUR
        }

    override val fastLogQuietHoursEndHour: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[SettingsKeys.FAST_LOG_QUIET_HOURS_END_HOUR] ?: DEFAULT_FAST_LOG_QUIET_HOURS_END_HOUR
        }

    override val fastLogLastImpressionEpochDay: Flow<Long?> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.FAST_LOG_LAST_IMPRESSION_EPOCH_DAY] }

    override val fastLogImpressionCountForDay: Flow<Int> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.FAST_LOG_IMPRESSION_COUNT_FOR_DAY] ?: 0 }

    override val fastLogConsecutiveDismissals: Flow<Int> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.FAST_LOG_CONSECUTIVE_DISMISSALS] ?: 0 }

    override val fastLogLastDismissedEpochDay: Flow<Long?> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.FAST_LOG_LAST_DISMISSED_EPOCH_DAY] }

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] = enabled
        }
    }

    override suspend fun setGoalProfile(profile: GoalProfile?) {
        dataStore.edit { preferences ->
            if (profile == null) {
                preferences.remove(SettingsKeys.GOAL_PROFILE)
            } else {
                preferences[SettingsKeys.GOAL_PROFILE] = profile.name
            }
        }
    }

    override suspend fun setGoalProfileOverlays(overlays: Set<DietaryOverlay>) {
        dataStore.edit { preferences ->
            if (overlays.isEmpty()) {
                preferences.remove(SettingsKeys.GOAL_PROFILE_OVERLAYS)
            } else {
                preferences[SettingsKeys.GOAL_PROFILE_OVERLAYS] = overlays.mapTo(linkedSetOf()) { it.name }
            }
        }
    }

    override suspend fun setGoalProfileOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.GOAL_PROFILE_ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setGoalsCustomised(customised: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.GOALS_CUSTOMISED] = customised
        }
    }

    override suspend fun setWeeklyReviewDismissedWeekStartEpochDay(epochDay: Long?) {
        dataStore.edit { preferences ->
            if (epochDay == null) {
                preferences.remove(SettingsKeys.WEEKLY_REVIEW_DISMISSED_WEEK_START_EPOCH_DAY)
            } else {
                preferences[SettingsKeys.WEEKLY_REVIEW_DISMISSED_WEEK_START_EPOCH_DAY] = epochDay
            }
        }
    }

    override suspend fun setFastLogReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setFastLogReminderWindow(startHour: Int, endHour: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_REMINDER_WINDOW_START_HOUR] = startHour.coerceIn(0, 23)
            preferences[SettingsKeys.FAST_LOG_REMINDER_WINDOW_END_HOUR] = endHour.coerceIn(0, 23)
        }
    }

    override suspend fun setFastLogQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_QUIET_HOURS_ENABLED] = enabled
        }
    }

    override suspend fun setFastLogQuietHoursWindow(startHour: Int, endHour: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_QUIET_HOURS_START_HOUR] = startHour.coerceIn(0, 23)
            preferences[SettingsKeys.FAST_LOG_QUIET_HOURS_END_HOUR] = endHour.coerceIn(0, 23)
        }
    }

    override suspend fun setFastLogReminderImpression(epochDay: Long, countForDay: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_LAST_IMPRESSION_EPOCH_DAY] = epochDay
            preferences[SettingsKeys.FAST_LOG_IMPRESSION_COUNT_FOR_DAY] = countForDay.coerceAtLeast(0)
        }
    }

    override suspend fun setFastLogDismissalState(consecutiveDismissals: Int, lastDismissedEpochDay: Long?) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_CONSECUTIVE_DISMISSALS] = consecutiveDismissals.coerceAtLeast(0)
            if (lastDismissedEpochDay == null) {
                preferences.remove(SettingsKeys.FAST_LOG_LAST_DISMISSED_EPOCH_DAY)
            } else {
                preferences[SettingsKeys.FAST_LOG_LAST_DISMISSED_EPOCH_DAY] = lastDismissedEpochDay
            }
        }
    }

    override suspend fun resetFastLogDismissalState() {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.FAST_LOG_CONSECUTIVE_DISMISSALS] = 0
            preferences.remove(SettingsKeys.FAST_LOG_LAST_DISMISSED_EPOCH_DAY)
        }
    }

    private fun goalProfileFromStoredValue(raw: String): GoalProfile? {
        return runCatching { GoalProfile.valueOf(raw) }.getOrNull()
    }

    private fun dietaryOverlayFromStoredValue(raw: String): DietaryOverlay? {
        return runCatching { DietaryOverlay.valueOf(raw) }.getOrNull()
    }
}
