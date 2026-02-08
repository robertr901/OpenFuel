package com.openfuel.app.data.entitlement

import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.model.EntitlementState
import com.openfuel.app.domain.repository.EntitlementsRepository
import com.openfuel.app.domain.security.SecurityPostureProvider
import com.openfuel.app.domain.service.EntitlementService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PlayBillingEntitlementService(
    private val entitlementsRepository: EntitlementsRepository,
    private val securityPostureProvider: SecurityPostureProvider,
    private val billingGateway: BillingGateway,
    private val proProductId: String,
) : EntitlementService {
    override fun getEntitlementState(): Flow<EntitlementState> {
        return combine(
            entitlementsRepository.isPro,
            securityPostureProvider.posture(),
        ) { isPro, posture ->
            EntitlementState(
                isPro = isPro,
                source = EntitlementSource.PLAY_BILLING,
                canToggleDebugOverride = false,
                securityPosture = posture,
            )
        }
    }

    override suspend fun refreshEntitlements() {
        val refreshResult = billingGateway.refreshEntitlement(productId = proProductId)
        if (refreshResult.errorMessage == null) {
            entitlementsRepository.setIsPro(refreshResult.isEntitled)
        }
    }

    override suspend fun purchasePro(): EntitlementActionResult {
        val purchaseResult = billingGateway.launchPurchaseFlow(productId = proProductId)
        return when (purchaseResult) {
            BillingPurchaseResult.Success -> {
                val refreshResult = billingGateway.refreshEntitlement(productId = proProductId)
                if (refreshResult.errorMessage != null) {
                    EntitlementActionResult.Error(refreshResult.errorMessage)
                } else if (refreshResult.isEntitled) {
                    entitlementsRepository.setIsPro(true)
                    EntitlementActionResult.Success("Pro unlocked.")
                } else {
                    EntitlementActionResult.Error("Purchase is pending. Use Restore if not unlocked.")
                }
            }
            BillingPurchaseResult.Cancelled -> EntitlementActionResult.Cancelled
            is BillingPurchaseResult.Error -> EntitlementActionResult.Error(purchaseResult.message)
        }
    }

    override suspend fun restorePurchases(): EntitlementActionResult {
        val refreshResult = billingGateway.refreshEntitlement(productId = proProductId)
        if (refreshResult.errorMessage != null) {
            return EntitlementActionResult.Error(refreshResult.errorMessage)
        }
        entitlementsRepository.setIsPro(refreshResult.isEntitled)
        return if (refreshResult.isEntitled) {
            EntitlementActionResult.Success("Pro restored.")
        } else {
            EntitlementActionResult.Error("No Pro purchase found for this account.")
        }
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        // Debug-only override is disabled in release entitlement service.
    }
}
