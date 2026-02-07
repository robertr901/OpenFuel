package com.openfuel.app.data.entitlement

import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.repository.EntitlementsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
        val service = DebugEntitlementService(repository)

        val initial = service.getEntitlementState().first()
        assertFalse(initial.isPro)
        assertEquals(EntitlementSource.DEBUG_OVERRIDE, initial.source)
        assertTrue(initial.canToggleDebugOverride)

        service.setDebugProOverride(true)

        assertTrue(service.getEntitlementState().first().isPro)
    }

    @Test
    fun placeholderService_remainsFreeAndIgnoresOverride() = runTest {
        val service = PlaceholderPlayBillingEntitlementService()

        val initial = service.getEntitlementState().first()
        assertFalse(initial.isPro)
        assertEquals(EntitlementSource.RELEASE_PLACEHOLDER, initial.source)
        assertFalse(initial.canToggleDebugOverride)

        service.setDebugProOverride(true)

        assertFalse(service.getEntitlementState().first().isPro)
    }
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
