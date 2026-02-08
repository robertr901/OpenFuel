package com.openfuel.app.data.entitlement

import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.model.SecurityPosture
import com.openfuel.app.domain.repository.EntitlementsRepository
import com.openfuel.app.domain.security.SecurityPostureProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntitlementServiceTest {
    @Test
    fun debugService_reflectsRepositoryAndSupportsOverride() = runTest {
        val repository = FakeEntitlementsRepository(initialValue = false)
        val service = DebugEntitlementService(
            entitlementsRepository = repository,
            securityPostureProvider = FakeSecurityPostureProvider(),
        )

        val initial = service.getEntitlementState().first()
        assertFalse(initial.isPro)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, initial.source)
        assertTrue(initial.canToggleDebugOverride)

        service.setDebugProOverride(true)

        assertTrue(service.getEntitlementState().first().isPro)
    }

    @Test
    fun playBillingService_refreshUpdatesRepositoryWhenGatewaySucceeds() = runTest {
        val repository = FakeEntitlementsRepository(initialValue = false)
        val billingGateway = FakeBillingGateway(
            refreshResult = BillingRefreshResult(isEntitled = true),
        )
        val service = PlayBillingEntitlementService(
            entitlementsRepository = repository,
            securityPostureProvider = FakeSecurityPostureProvider(),
            billingGateway = billingGateway,
            proProductId = "openfuel_pro",
        )

        service.refreshEntitlements()

        val refreshed = service.getEntitlementState().first()
        assertTrue(refreshed.isPro)
        assertEquals(EntitlementSource.PLAY_BILLING, refreshed.source)
        assertFalse(refreshed.canToggleDebugOverride)
    }

    @Test
    fun playBillingService_purchaseSuccess_unlocksPro() = runTest {
        val repository = FakeEntitlementsRepository(initialValue = false)
        val billingGateway = FakeBillingGateway(
            purchaseResult = BillingPurchaseResult.Success,
            refreshResult = BillingRefreshResult(isEntitled = true),
        )
        val service = PlayBillingEntitlementService(
            entitlementsRepository = repository,
            securityPostureProvider = FakeSecurityPostureProvider(),
            billingGateway = billingGateway,
            proProductId = "openfuel_pro",
        )

        val result = service.purchasePro()

        assertTrue(result is EntitlementActionResult.Success)
        assertTrue(service.getEntitlementState().first().isPro)
    }

    @Test
    fun playBillingService_purchaseCancelled_keepsExistingEntitlement() = runTest {
        val repository = FakeEntitlementsRepository(initialValue = false)
        val service = PlayBillingEntitlementService(
            entitlementsRepository = repository,
            securityPostureProvider = FakeSecurityPostureProvider(),
            billingGateway = FakeBillingGateway(
                purchaseResult = BillingPurchaseResult.Cancelled,
                refreshResult = BillingRefreshResult(isEntitled = false),
            ),
            proProductId = "openfuel_pro",
        )

        val result = service.purchasePro()

        assertTrue(result is EntitlementActionResult.Cancelled)
        assertFalse(service.getEntitlementState().first().isPro)
    }
}

private class FakeSecurityPostureProvider : SecurityPostureProvider {
    override fun posture(): Flow<SecurityPosture> = flowOf(SecurityPosture.secure())
}

private class FakeEntitlementsRepository(
    initialValue: Boolean,
) : EntitlementsRepository {
    private val state = MutableStateFlow(initialValue)

    override val isPro: Flow<Boolean> = state

    override suspend fun setIsPro(enabled: Boolean) {
        state.value = enabled
    }
}

private class FakeBillingGateway(
    private val refreshResult: BillingRefreshResult,
    private val purchaseResult: BillingPurchaseResult = BillingPurchaseResult.Error("Not configured"),
) : BillingGateway {
    override suspend fun refreshEntitlement(productId: String): BillingRefreshResult {
        return refreshResult
    }

    override suspend fun launchPurchaseFlow(productId: String): BillingPurchaseResult {
        return purchaseResult
    }
}
