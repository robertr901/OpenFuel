package com.openfuel.app.data.entitlement

import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.model.EntitlementState
import com.openfuel.app.domain.repository.EntitlementsRepository
import com.openfuel.app.domain.service.EntitlementService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebugEntitlementService(
    private val entitlementsRepository: EntitlementsRepository,
) : EntitlementService {
    override fun getEntitlementState(): Flow<EntitlementState> {
        return entitlementsRepository.isPro.map { isPro ->
            EntitlementState(
                isPro = isPro,
                source = EntitlementSource.DEBUG_OVERRIDE,
                canToggleDebugOverride = true,
            )
        }
    }

    override suspend fun refreshEntitlements() {
        // Local debug override has no remote refresh dependency.
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        entitlementsRepository.setIsPro(enabled)
    }
}
