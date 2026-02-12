package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.service.ProviderExecutionReport
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderMergedCandidate
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanBarcodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onBarcodeDetected_success_setsPreviewFood() = runTest {
        val remoteDataSource = ScanFakeProviderExecutor()
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("123456")
        advanceUntilIdle()

        assertEquals(1, remoteDataSource.lookupCalls)
        assertEquals("123456", viewModel.uiState.value.lastBarcode)
        assertNotNull(viewModel.uiState.value.previewFood)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(BarcodeLookupStatus.SUCCESS, viewModel.uiState.value.lookupStatus)
        assertFalse(viewModel.uiState.value.canRetry)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_notFound_setsError() = runTest {
        val remoteDataSource = ScanFakeProviderExecutor(foundBarcode = null)
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.onBarcodeDetected("999")
        advanceUntilIdle()

        assertEquals("No matching food found for barcode.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.previewFood)
        assertEquals(BarcodeLookupStatus.ERROR, viewModel.uiState.value.lookupStatus)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_whenOnlineDisabled_skipsLookupAndShowsMessage() = runTest {
        val remoteDataSource = ScanFakeProviderExecutor()
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = false),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("123456")
        advanceUntilIdle()

        assertEquals(0, remoteDataSource.lookupCalls)
        assertEquals(
            "Online search is turned off. Enable it in Settings to use barcode lookup.",
            viewModel.uiState.value.message,
        )
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_whenUsdaKeyMissing_showsConfigurationMessage() = runTest {
        val remoteDataSource = ScanStatusProviderExecutor(
            providerId = "usda_fdc",
            status = ProviderStatus.DISABLED_BY_SETTINGS,
            diagnostics = "USDA API key missing. Add USDA_API_KEY in local.properties.",
        )
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("012345")
        advanceUntilIdle()

        assertEquals(
            "USDA provider is not configured. Add USDA_API_KEY in local.properties.",
            viewModel.uiState.value.errorMessage,
        )
        assertNull(viewModel.uiState.value.previewFood)
        assertEquals(BarcodeLookupStatus.ERROR, viewModel.uiState.value.lookupStatus)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_sameBarcodeWithinWindow_skipsDuplicateLookup() = runTest {
        val remoteDataSource = ScanFakeProviderExecutor()
        var nowMs = 1_000L
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
            nowEpochMillis = { nowMs },
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("123456")
        advanceUntilIdle()
        nowMs = 1_200L
        viewModel.onBarcodeDetected("123456")
        advanceUntilIdle()

        assertEquals(1, remoteDataSource.lookupCalls)
        collectJob.cancel()
    }

    @Test
    fun retryLookup_bypassesDedupe_andTriggersSingleAdditionalLookup() = runTest {
        val remoteDataSource = ScanStatusProviderExecutor(
            providerId = "open_food_facts",
            status = ProviderStatus.TIMEOUT,
            diagnostics = "Provider execution timed out.",
        )
        var nowMs = 1_000L
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
            nowEpochMillis = { nowMs },
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("123456")
        advanceUntilIdle()
        assertEquals(1, remoteDataSource.lookupCalls)
        assertEquals(BarcodeLookupStatus.TIMEOUT, viewModel.uiState.value.lookupStatus)
        nowMs = 1_200L
        viewModel.retryLookup()
        advanceUntilIdle()

        assertEquals(2, remoteDataSource.lookupCalls)
        assertEquals(BarcodeLookupStatus.TIMEOUT, viewModel.uiState.value.lookupStatus)
        assertTrue(viewModel.uiState.value.canRetry)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_latestBarcodeWins_whenEarlierLookupCompletesLater() = runTest {
        val remoteDataSource = ControlledScanProviderExecutor()
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("111")
        advanceUntilIdle()
        viewModel.onBarcodeDetected("222")
        advanceUntilIdle()

        remoteDataSource.completeSuccess(barcode = "222", providerId = "provider_b")
        advanceUntilIdle()
        assertEquals("222", viewModel.uiState.value.previewFood?.barcode)
        assertEquals("222", viewModel.uiState.value.lastBarcode)

        remoteDataSource.completeSuccess(barcode = "111", providerId = "provider_a")
        advanceUntilIdle()
        assertEquals("222", viewModel.uiState.value.previewFood?.barcode)
        assertEquals("222", viewModel.uiState.value.lastBarcode)
        assertEquals(BarcodeLookupStatus.SUCCESS, viewModel.uiState.value.lookupStatus)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_whenNetworkUnavailable_setsNoConnectionState() = runTest {
        val remoteDataSource = ScanStatusProviderExecutor(
            providerId = "open_food_facts",
            status = ProviderStatus.NETWORK_UNAVAILABLE,
            diagnostics = "No internet connection",
        )
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("987654")
        advanceUntilIdle()

        assertEquals(BarcodeLookupStatus.NO_CONNECTION, viewModel.uiState.value.lookupStatus)
        assertEquals("No connection.", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.canRetry)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_whenTimeout_setsTimeoutState() = runTest {
        val remoteDataSource = ScanStatusProviderExecutor(
            providerId = "open_food_facts",
            status = ProviderStatus.TIMEOUT,
            diagnostics = "Provider execution timed out.",
        )
        val viewModel = ScanBarcodeViewModel(
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
            settingsRepository = FakeScanSettingsRepository(enabled = true),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onBarcodeDetected("111222")
        advanceUntilIdle()

        assertEquals(BarcodeLookupStatus.TIMEOUT, viewModel.uiState.value.lookupStatus)
        assertEquals("Timed out (check connection).", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.canRetry)
        collectJob.cancel()
    }
}

private class ScanFakeProviderExecutor(
    private val foundBarcode: String? = "123456",
) : ProviderExecutor {
    var lookupCalls: Int = 0

    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        val barcode = request.barcode.orEmpty()
        lookupCalls += 1
        val item = if (barcode != foundBarcode) {
            null
        } else {
            RemoteFoodCandidate(
                source = RemoteFoodSource.OPEN_FOOD_FACTS,
                sourceId = barcode,
                barcode = barcode,
                name = "Scanned Oats",
                brand = "OpenFoodFacts",
                caloriesKcalPer100g = 370.0,
                proteinGPer100g = 13.0,
                carbsGPer100g = 67.0,
                fatGPer100g = 7.0,
                servingSize = "40 g",
            )
        }
        return ProviderExecutionReport(
            requestType = request.requestType,
            sourceFilter = request.sourceFilter,
            mergedCandidates = item?.let { candidate ->
                listOf(
                    ProviderMergedCandidate(
                        providerId = "test_provider",
                        candidate = candidate.copy(providerKey = "test_provider"),
                        dedupeKey = candidate.sourceId,
                    ),
                )
            }.orEmpty(),
            providerResults = listOf(
                ProviderResult(
                    providerId = "test_provider",
                    capability = request.requestType.capability,
                    status = if (item == null) ProviderStatus.EMPTY else ProviderStatus.AVAILABLE,
                    items = item?.let { listOf(it) }.orEmpty(),
                    elapsedMs = 1L,
                ),
            ),
            overallElapsedMs = 1L,
        )
    }
}

private class ControlledScanProviderExecutor : ProviderExecutor {
    private val pending = mutableMapOf<String, CompletableDeferred<ProviderExecutionReport>>()

    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        val barcode = request.barcode.orEmpty()
        val deferred = pending.getOrPut(barcode) { CompletableDeferred() }
        return withContext(NonCancellable) {
            deferred.await()
        }
    }

    fun completeSuccess(
        barcode: String,
        providerId: String,
    ) {
        val candidate = RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = barcode,
            barcode = barcode,
            name = "Food $barcode",
            brand = "Provider",
            caloriesKcalPer100g = 100.0,
            proteinGPer100g = 5.0,
            carbsGPer100g = 10.0,
            fatGPer100g = 1.0,
            servingSize = "100 g",
        )
        val report = ProviderExecutionReport(
            requestType = com.openfuel.app.domain.service.ProviderRequestType.BARCODE_LOOKUP,
            sourceFilter = com.openfuel.app.domain.search.SearchSourceFilter.ONLINE_ONLY,
            mergedCandidates = listOf(
                ProviderMergedCandidate(
                    providerId = providerId,
                    candidate = candidate.copy(providerKey = providerId),
                    dedupeKey = barcode,
                ),
            ),
            providerResults = listOf(
                ProviderResult(
                    providerId = providerId,
                    capability = com.openfuel.app.domain.service.ProviderCapability.BARCODE_LOOKUP,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(candidate),
                    elapsedMs = 1L,
                ),
            ),
            overallElapsedMs = 1L,
        )
        pending.getOrPut(barcode) { CompletableDeferred() }.complete(report)
    }
}

private class ScanFakeFoodRepository : FoodRepository {
    override suspend fun upsertFood(foodItem: FoodItem) {
        // no-op
    }

    override suspend fun getFoodById(id: String): FoodItem? {
        return null
    }

    override suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        return null
    }

    override suspend fun setFavorite(foodId: String, isFavorite: Boolean) {
        // no-op
    }

    override suspend fun setReportedIncorrect(foodId: String, isReportedIncorrect: Boolean) {
        // no-op
    }

    override fun favoriteFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(emptyList())
    }

    override fun recentLoggedFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(emptyList())
    }

    override fun allFoods(query: String): Flow<List<FoodItem>> {
        return flowOf(emptyList())
    }

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(emptyList())
    }

    override fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>> {
        return flowOf(emptyList())
    }
}

private class ScanFakeLogRepository : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) {
        // no-op
    }

    override suspend fun updateMealEntry(entry: MealEntry) {
        // no-op
    }

    override suspend fun deleteMealEntry(id: String) {
        // no-op
    }

    override fun entriesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<MealEntryWithFood>> {
        return flowOf(emptyList())
    }

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> {
        return flowOf(emptyList())
    }

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        return flowOf(emptyList())
    }
}

private class FakeScanSettingsRepository(
    enabled: Boolean,
) : SettingsRepository {
    override val onlineLookupEnabled: Flow<Boolean> = flowOf(enabled)
    override val goalProfile: Flow<GoalProfile?> = flowOf(null)
    override val goalProfileOverlays: Flow<Set<DietaryOverlay>> = flowOf(emptySet())
    override val goalProfileOnboardingCompleted: Flow<Boolean> = flowOf(false)
    override val goalsCustomised: Flow<Boolean> = flowOf(false)
    override val weeklyReviewDismissedWeekStartEpochDay: Flow<Long?> = flowOf(null)
    override val fastLogReminderEnabled: Flow<Boolean> = flowOf(true)
    override val fastLogReminderWindowStartHour: Flow<Int> = flowOf(7)
    override val fastLogReminderWindowEndHour: Flow<Int> = flowOf(21)
    override val fastLogQuietHoursEnabled: Flow<Boolean> = flowOf(true)
    override val fastLogQuietHoursStartHour: Flow<Int> = flowOf(21)
    override val fastLogQuietHoursEndHour: Flow<Int> = flowOf(7)
    override val fastLogLastImpressionEpochDay: Flow<Long?> = flowOf(null)
    override val fastLogImpressionCountForDay: Flow<Int> = flowOf(0)
    override val fastLogConsecutiveDismissals: Flow<Int> = flowOf(0)
    override val fastLogLastDismissedEpochDay: Flow<Long?> = flowOf(null)

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        // no-op
    }

    override suspend fun setGoalProfile(profile: GoalProfile?) {
        // no-op
    }

    override suspend fun setGoalProfileOverlays(overlays: Set<DietaryOverlay>) {
        // no-op
    }

    override suspend fun setGoalProfileOnboardingCompleted(completed: Boolean) {
        // no-op
    }

    override suspend fun setGoalsCustomised(customised: Boolean) {
        // no-op
    }

    override suspend fun setWeeklyReviewDismissedWeekStartEpochDay(epochDay: Long?) {
        // no-op
    }

    override suspend fun setFastLogReminderEnabled(enabled: Boolean) {
        // no-op
    }

    override suspend fun setFastLogReminderWindow(startHour: Int, endHour: Int) {
        // no-op
    }

    override suspend fun setFastLogQuietHoursEnabled(enabled: Boolean) {
        // no-op
    }

    override suspend fun setFastLogQuietHoursWindow(startHour: Int, endHour: Int) {
        // no-op
    }

    override suspend fun setFastLogReminderImpression(epochDay: Long, countForDay: Int) {
        // no-op
    }

    override suspend fun setFastLogDismissalState(consecutiveDismissals: Int, lastDismissedEpochDay: Long?) {
        // no-op
    }

    override suspend fun resetFastLogDismissalState() {
        // no-op
    }
}

private class ScanStatusProviderExecutor(
    private val providerId: String,
    private val status: ProviderStatus,
    private val diagnostics: String?,
) : ProviderExecutor {
    var lookupCalls: Int = 0

    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        lookupCalls += 1
        return ProviderExecutionReport(
            requestType = request.requestType,
            sourceFilter = request.sourceFilter,
            mergedCandidates = emptyList(),
            providerResults = listOf(
                ProviderResult(
                    providerId = providerId,
                    capability = request.requestType.capability,
                    status = status,
                    items = emptyList(),
                    elapsedMs = 0L,
                    diagnostics = diagnostics,
                ),
            ),
            overallElapsedMs = 0L,
        )
    }
}
