package com.openfuel.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val onlineLookupEnabled: Flow<Boolean>
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
    suspend fun setFastLogReminderEnabled(enabled: Boolean)
    suspend fun setFastLogReminderWindow(startHour: Int, endHour: Int)
    suspend fun setFastLogQuietHoursEnabled(enabled: Boolean)
    suspend fun setFastLogQuietHoursWindow(startHour: Int, endHour: Int)
    suspend fun setFastLogReminderImpression(epochDay: Long, countForDay: Int)
    suspend fun setFastLogDismissalState(consecutiveDismissals: Int, lastDismissedEpochDay: Long?)
    suspend fun resetFastLogDismissalState()
}
