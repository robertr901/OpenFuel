package com.openfuel.app.domain.service

import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.EntitlementState
import kotlinx.coroutines.flow.Flow

interface EntitlementService {
    fun getEntitlementState(): Flow<EntitlementState>
    suspend fun refreshEntitlements()
    suspend fun purchasePro(): EntitlementActionResult
    suspend fun restorePurchases(): EntitlementActionResult
    suspend fun setDebugProOverride(enabled: Boolean)
}
