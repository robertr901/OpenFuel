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
    override val onlineLookupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] ?: false }

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] = enabled
        }
    }
}
