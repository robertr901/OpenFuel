package com.openfuel.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.openfuel.app.data.datastore.SettingsKeys
import com.openfuel.app.domain.repository.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EntitlementsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : EntitlementsRepository {
    override val isPro: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.ENTITLEMENT_IS_PRO] ?: false }

    override suspend fun setIsPro(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.ENTITLEMENT_IS_PRO] = enabled
        }
    }
}
