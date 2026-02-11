package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.entitlement.PaywallPromptPolicy
import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.EntitlementSource
import com.openfuel.app.domain.model.EntitlementState
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.service.EntitlementService
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun entitlementState_controlsProGating() = runTest {
        val entitlementService = FakeEntitlementService(
            EntitlementState(
                isPro = false,
                source = EntitlementSource.DEBUG_OVERRIDE,
                canToggleDebugOverride = true,
            ),
        )
        val viewModel = InsightsViewModel(
            entitlementService = entitlementService,
            logRepository = FakeInsightsLogRepository(),
            paywallPromptPolicy = PaywallPromptPolicy(),
            zoneId = ZoneId.of("UTC"),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isPro)

        entitlementService.emit(isPro = true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPro)

        collectJob.cancel()
    }

    @Test
    fun openPaywall_sessionLimitedPromptOnlyShowsOncePerSession() = runTest {
        val viewModel = InsightsViewModel(
            entitlementService = FakeEntitlementService(
                EntitlementState(
                    isPro = false,
                    source = EntitlementSource.DEBUG_OVERRIDE,
                    canToggleDebugOverride = true,
                ),
            ),
            logRepository = FakeInsightsLogRepository(),
            paywallPromptPolicy = PaywallPromptPolicy(),
            zoneId = ZoneId.of("UTC"),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPaywall)

        viewModel.openPaywall()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showPaywall)

        viewModel.dismissPaywall()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPaywall)

        viewModel.openPaywall()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPaywall)

        collectJob.cancel()
    }

    @Test
    fun openPaywallForGatedFeature_alwaysShowsPaywall() = runTest {
        val viewModel = InsightsViewModel(
            entitlementService = FakeEntitlementService(
                EntitlementState(
                    isPro = false,
                    source = EntitlementSource.DEBUG_OVERRIDE,
                    canToggleDebugOverride = true,
                ),
            ),
            logRepository = FakeInsightsLogRepository(),
            paywallPromptPolicy = PaywallPromptPolicy(),
            zoneId = ZoneId.of("UTC"),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        viewModel.openPaywallForGatedFeature()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showPaywall)

        viewModel.dismissPaywall()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPaywall)

        viewModel.openPaywallForGatedFeature()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.showPaywall)

        collectJob.cancel()
    }
}

private class FakeEntitlementService(
    initialValue: EntitlementState,
) : EntitlementService {
    private val state = MutableStateFlow(initialValue)

    override fun getEntitlementState(): Flow<EntitlementState> = state

    override suspend fun refreshEntitlements() {
        // no-op for tests
    }

    override suspend fun purchasePro(): EntitlementActionResult {
        return EntitlementActionResult.Cancelled
    }

    override suspend fun restorePurchases(): EntitlementActionResult {
        return EntitlementActionResult.Cancelled
    }

    override suspend fun setDebugProOverride(enabled: Boolean) {
        state.value = state.value.copy(isPro = enabled)
    }

    fun emit(isPro: Boolean) {
        state.value = state.value.copy(isPro = isPro)
    }
}

private class FakeInsightsLogRepository : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) = Unit

    override suspend fun updateMealEntry(entry: MealEntry) = Unit

    override suspend fun deleteMealEntry(id: String) = Unit

    override fun entriesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<MealEntryWithFood>> {
        return flowOf(emptyList())
    }

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        return flowOf(emptyList())
    }

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> {
        return flowOf(emptyList())
    }
}
