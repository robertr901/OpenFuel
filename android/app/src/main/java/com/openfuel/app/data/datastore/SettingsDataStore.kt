package com.openfuel.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.core.DataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val ONLINE_LOOKUP_ENABLED = booleanPreferencesKey("online_lookup_enabled")
    val GOAL_CALORIES_KCAL = doublePreferencesKey("goal_calories_kcal")
    val GOAL_PROTEIN_G = doublePreferencesKey("goal_protein_g")
    val GOAL_CARBS_G = doublePreferencesKey("goal_carbs_g")
    val GOAL_FAT_G = doublePreferencesKey("goal_fat_g")
}
