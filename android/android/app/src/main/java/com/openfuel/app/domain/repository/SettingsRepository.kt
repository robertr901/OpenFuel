package com.openfuel.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val onlineLookupEnabled: Flow<Boolean>
    suspend fun setOnlineLookupEnabled(enabled: Boolean)
}
