package com.openfuel.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.openfuel.app.data.datastore.SettingsKeys
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

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] = enabled
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
}
