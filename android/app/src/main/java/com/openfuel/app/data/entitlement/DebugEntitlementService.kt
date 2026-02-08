package com.openfuel.app.data.entitlement

import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.model.EntitlementState
import com.openfuel.app.domain.repository.EntitlementsRepository
import com.openfuel.app.domain.security.SecurityPostureProvider
import com.openfuel.app.domain.service.EntitlementService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DebugEntitlementService(
    private val entitlementsRepository: EntitlementsRepository,
    private val securityPostureProvider: SecurityPostureProvider,
) : EntitlementService {
    override fun getEntitlementState(): Flow<EntitlementState> {
        return combine(
            entitlementsRepository.isPro,
            securityPostureProvider.posture(),
        ) { isPro, posture ->
            EntitlementState(
                isPro = isPro,
                source = EntitlementSource.DEBUG_OVERRIDE,
                canToggleDebugOverride = true,
                securityPosture = posture,
            )
        }
    }

    override suspend fun refreshEntitlements() {
        // Local debug override has no remote refresh dependency.
    }

    override suspend fun purchasePro(): EntitlementActionResult {
        entitlementsRepository.setIsPro(true)
        return EntitlementActionResult.Success("Pro unlocked (debug override).")
    }

    override suspend fun restorePurchases(): EntitlementActionResult {
        entitlementsRepository.setIsPro(true)
        return EntitlementActionResult.Success("Pro restored (debug override).")
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        entitlementsRepository.setIsPro(enabled)
    }
}
