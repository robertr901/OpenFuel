package com.openfuel.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.core.DataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val ONLINE_LOOKUP_ENABLED = booleanPreferencesKey("online_lookup_enabled")
}
