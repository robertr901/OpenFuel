package com.openfuel.app.data.entitlement

import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.model.EntitlementState
import com.openfuel.app.domain.security.SecurityPostureProvider
import com.openfuel.app.domain.service.EntitlementService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaceholderPlayBillingEntitlementService(
    private val securityPostureProvider: SecurityPostureProvider,
) : EntitlementService {
    override fun getEntitlementState(): Flow<EntitlementState> {
        return securityPostureProvider.posture().map { posture ->
            EntitlementState(
                isPro = false,
                source = EntitlementSource.RELEASE_PLACEHOLDER,
                canToggleDebugOverride = false,
                securityPosture = posture,
            )
        }
    }

    override suspend fun refreshEntitlements() {
        // Play Billing validation will be wired in a follow-up phase.
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        // No-op by design outside debug builds.
    }
}
