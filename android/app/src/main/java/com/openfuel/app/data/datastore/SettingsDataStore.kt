package com.openfuel.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.core.DataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val ONLINE_LOOKUP_ENABLED = booleanPreferencesKey("online_lookup_enabled")
    val ENTITLEMENT_IS_PRO = booleanPreferencesKey("entitlement_is_pro")
    val FAST_LOG_REMINDER_ENABLED = booleanPreferencesKey("fast_log_reminder_enabled")
    val FAST_LOG_REMINDER_WINDOW_START_HOUR = intPreferencesKey("fast_log_reminder_window_start_hour")
    val FAST_LOG_REMINDER_WINDOW_END_HOUR = intPreferencesKey("fast_log_reminder_window_end_hour")
    val FAST_LOG_QUIET_HOURS_ENABLED = booleanPreferencesKey("fast_log_quiet_hours_enabled")
    val FAST_LOG_QUIET_HOURS_START_HOUR = intPreferencesKey("fast_log_quiet_hours_start_hour")
    val FAST_LOG_QUIET_HOURS_END_HOUR = intPreferencesKey("fast_log_quiet_hours_end_hour")
    val FAST_LOG_LAST_IMPRESSION_EPOCH_DAY = longPreferencesKey("fast_log_last_impression_epoch_day")
    val FAST_LOG_IMPRESSION_COUNT_FOR_DAY = intPreferencesKey("fast_log_impression_count_for_day")
    val FAST_LOG_CONSECUTIVE_DISMISSALS = intPreferencesKey("fast_log_consecutive_dismissals")
    val FAST_LOG_LAST_DISMISSED_EPOCH_DAY = longPreferencesKey("fast_log_last_dismissed_epoch_day")
    val GOAL_CALORIES_KCAL = doublePreferencesKey("goal_calories_kcal")
    val GOAL_PROTEIN_G = doublePreferencesKey("goal_protein_g")
    val GOAL_CARBS_G = doublePreferencesKey("goal_carbs_g")
    val GOAL_FAT_G = doublePreferencesKey("goal_fat_g")
}
