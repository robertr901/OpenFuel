package com.openfuel.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface EntitlementsRepository {
    val isPro: Flow<Boolean>
    suspend fun setIsPro(enabled: Boolean)
}
