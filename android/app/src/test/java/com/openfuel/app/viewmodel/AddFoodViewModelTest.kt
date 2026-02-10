package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.data.remote.UserInitiatedNetworkToken
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.ProviderExecutionReport
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderMergedCandidate
import com.openfuel.app.domain.service.ProviderRefreshPolicy
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun searchOnline_onlyRunsWhenExplicitlyRequested() = runTest {
        val remoteDataSource = FakeProviderExecutor(delayMs = 1_000)
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(
                foods = listOf(
                    fakeFood(id = "local-1", name = "Oatmeal"),
                ),
            ),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(0, remoteDataSource.searchCalls)

        viewModel.searchOnline()
        runCurrent()

        assertTrue(viewModel.uiState.value.isOnlineSearchInProgress)
        advanceUntilIdle()

        assertEquals(1, remoteDataSource.searchCalls)
        assertEquals(1, viewModel.uiState.value.onlineResults.size)
        assertFalse(viewModel.uiState.value.isOnlineSearchInProgress)
        assertTrue(viewModel.uiState.value.hasSearchedOnline)
        collectJob.cancel()
    }

    @Test
    fun updateSearchQuery_updatesLocalResultsWithoutOnlineCall() = runTest {
        val remoteDataSource = FakeProviderExecutor()
        val foodRepository = AddFoodFakeFoodRepository(
            foods = listOf(
                fakeFood(id = "f1", name = "Oatmeal"),
                fakeFood(id = "f2", name = "Greek Yogurt"),
            ),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = foodRepository,
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(0, remoteDataSource.searchCalls)
        assertEquals(1, viewModel.uiState.value.foods.size)
        assertEquals("Oatmeal", viewModel.uiState.value.foods.first().name)
        assertEquals("oat", foodRepository.searchQueries.last())
        collectJob.cancel()
    }

    @Test
    fun updateSearchQuery_normalizesPunctuationAndUnitsForLocalSearch() = runTest {
        val foodRepository = AddFoodFakeFoodRepository(
            foods = listOf(
                fakeFood(id = "f1", name = "Coke Zero 330 ml"),
            ),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = foodRepository,
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = FakeProviderExecutor(),
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("Coke-Zero (330ml)")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("coke zero 330 ml", foodRepository.searchQueries.last())
        collectJob.cancel()
    }

    @Test
    fun searchOnline_normalizesQueryBeforeExplicitExecution() = runTest {
        val remoteDataSource = FakeProviderExecutor()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("Coke-Zero (330ml)")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals(1, remoteDataSource.requests.size)
        assertEquals("coke zero 330 ml", remoteDataSource.requests.single().query)
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenDisabled_skipsNetworkCallAndShowsMessage() = runTest {
        val remoteDataSource = FakeProviderExecutor()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = false),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals(0, remoteDataSource.searchCalls)
        assertEquals(
            "Online search is turned off. Enable it in Settings to continue.",
            viewModel.uiState.value.onlineErrorMessage,
        )
        assertFalse(viewModel.uiState.value.hasSearchedOnline)
        assertFalse(viewModel.uiState.value.isOnlineSearchInProgress)
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenNoResults_marksAttemptWithoutError() = runTest {
        val remoteDataSource = FakeProviderExecutor(results = emptyList())
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("coke zero")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasSearchedOnline)
        assertTrue(viewModel.uiState.value.onlineResults.isEmpty())
        assertEquals(null, viewModel.uiState.value.onlineErrorMessage)
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenProviderThrows_exposesErrorState() = runTest {
        val remoteDataSource = FakeProviderExecutor(
            throwable = IllegalStateException("boom"),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("coke zero")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasSearchedOnline)
        assertFalse(viewModel.uiState.value.isOnlineSearchInProgress)
        assertEquals("Online search failed. Check connection and try again.", viewModel.uiState.value.onlineErrorMessage)
        assertTrue(viewModel.uiState.value.onlineResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenNetworkUnavailable_showsFriendlyErrorWithoutCrash() = runTest {
        val remoteDataSource = StatusOnlyProviderExecutor(
            providerId = "open_food_facts",
            status = ProviderStatus.NETWORK_UNAVAILABLE,
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("banana")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals("No connection.", viewModel.uiState.value.onlineErrorMessage)
        assertTrue(viewModel.uiState.value.onlineResults.isEmpty())
        assertFalse(viewModel.uiState.value.isOnlineSearchInProgress)
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenUsdaKeyMissing_andNoResults_showsConfigurationMessage() = runTest {
        val remoteDataSource = StatusOnlyProviderExecutor(
            providerId = "usda_fdc",
            status = ProviderStatus.DISABLED_BY_SETTINGS,
            diagnostics = "USDA API key missing. Add USDA_API_KEY in local.properties.",
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oats")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals("Source needs setup. See statuses below.", viewModel.uiState.value.onlineErrorMessage)
        assertTrue(viewModel.uiState.value.onlineResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenMultipleProvidersFail_showsSingleSummaryMessage() = runTest {
        val remoteDataSource = MultiStatusProviderExecutor(
            providerResults = listOf(
                ProviderResult(
                    providerId = "usda_fdc",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.DISABLED_BY_SETTINGS,
                    items = emptyList(),
                    elapsedMs = 0L,
                    diagnostics = "USDA API key missing. Add USDA_API_KEY in local.properties.",
                ),
                ProviderResult(
                    providerId = "open_food_facts",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.TIMEOUT,
                    items = emptyList(),
                    elapsedMs = 0L,
                    diagnostics = "Provider execution timed out.",
                ),
            ),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals("Some sources failed. See statuses below.", viewModel.uiState.value.onlineErrorMessage)
        assertTrue(viewModel.uiState.value.onlineResults.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenResultsExistAndAnotherProviderNeedsSetup_hidesTopLevelErrorMessage() = runTest {
        val remoteDataSource = MultiStatusProviderExecutor(
            providerResults = listOf(
                ProviderResult(
                    providerId = "open_food_facts",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(
                        RemoteFoodCandidate(
                            source = RemoteFoodSource.OPEN_FOOD_FACTS,
                            sourceId = "off-1",
                            barcode = "123",
                            name = "Greek Yogurt",
                            brand = "OFF",
                            caloriesKcalPer100g = 62.0,
                            proteinGPer100g = 10.0,
                            carbsGPer100g = 4.0,
                            fatGPer100g = 1.0,
                            servingSize = "170 g",
                        ),
                    ),
                    elapsedMs = 0L,
                ),
                ProviderResult(
                    providerId = "usda_fdc",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.DISABLED_BY_SETTINGS,
                    items = emptyList(),
                    elapsedMs = 0L,
                    diagnostics = "USDA API key missing. Add USDA_API_KEY in local.properties.",
                ),
            ),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("yogurt")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.onlineResults.size)
        assertEquals(null, viewModel.uiState.value.onlineErrorMessage)
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenResultsExistAndAnotherProviderFails_hidesTopLevelErrorMessage() = runTest {
        val remoteDataSource = MultiStatusProviderExecutor(
            providerResults = listOf(
                ProviderResult(
                    providerId = "open_food_facts",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(
                        RemoteFoodCandidate(
                            source = RemoteFoodSource.OPEN_FOOD_FACTS,
                            sourceId = "off-2",
                            barcode = "456",
                            name = "Oatmeal",
                            brand = "OFF",
                            caloriesKcalPer100g = 370.0,
                            proteinGPer100g = 13.0,
                            carbsGPer100g = 67.0,
                            fatGPer100g = 7.0,
                            servingSize = "40 g",
                        ),
                    ),
                    elapsedMs = 0L,
                ),
                ProviderResult(
                    providerId = "nutritionix",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.TIMEOUT,
                    items = emptyList(),
                    elapsedMs = 0L,
                    diagnostics = "Provider execution timed out.",
                ),
            ),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.onlineResults.size)
        assertEquals(null, viewModel.uiState.value.onlineErrorMessage)
        collectJob.cancel()
    }

    @Test
    fun updateSearchQuery_clearsPreviousOnlineResultsAndAttemptState() = runTest {
        val remoteDataSource = FakeProviderExecutor()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.onlineResults.isNotEmpty())
        assertTrue(viewModel.uiState.value.hasSearchedOnline)

        viewModel.updateSearchQuery("banana")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.onlineResults.isEmpty())
        assertFalse(viewModel.uiState.value.hasSearchedOnline)
        assertEquals(null, viewModel.uiState.value.onlineErrorMessage)
        collectJob.cancel()
    }

    @Test
    fun updateSearchQuery_doesNotClearRecentsOrFavorites() = runTest {
        val favorite = fakeFood(id = "favorite-1", name = "Favorite Yogurt")
        val recent = fakeFood(id = "recent-1", name = "Recent Oatmeal")
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(
                foods = listOf(favorite, recent),
                favorites = listOf(favorite),
                recents = listOf(recent),
            ),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = FakeProviderExecutor(),
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.favoriteFoods.size)
        assertEquals("Favorite Yogurt", viewModel.uiState.value.favoriteFoods.first().name)
        assertEquals(1, viewModel.uiState.value.recentLoggedFoods.size)
        assertEquals("Recent Oatmeal", viewModel.uiState.value.recentLoggedFoods.first().name)
        collectJob.cancel()
    }

    @Test
    fun refreshOnline_usesForceRefreshAndIncrementsExecutionCount() = runTest {
        val remoteDataSource = FakeProviderExecutor()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()
        viewModel.searchOnline()
        advanceUntilIdle()
        viewModel.refreshOnline()
        advanceUntilIdle()

        assertEquals(2, remoteDataSource.searchCalls)
        assertEquals(2, viewModel.uiState.value.onlineExecutionCount)
        assertEquals(ProviderRefreshPolicy.CACHE_PREFERRED, remoteDataSource.requests[0].refreshPolicy)
        assertEquals(ProviderRefreshPolicy.FORCE_REFRESH, remoteDataSource.requests[1].refreshPolicy)
        collectJob.cancel()
    }

    @Test
    fun saveOnlineFood_blankNameAndBrand_persistsSanitizedItem() = runTest {
        val foodRepository = AddFoodFakeFoodRepository()
        val viewModel = AddFoodViewModel(
            foodRepository = foodRepository,
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = FakeProviderExecutor(),
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )

        viewModel.saveOnlineFood(
            food = RemoteFoodCandidate(
                source = RemoteFoodSource.OPEN_FOOD_FACTS,
                sourceId = "edge-case-1",
                barcode = "  ",
                name = "  ",
                brand = " ",
                caloriesKcalPer100g = null,
                proteinGPer100g = null,
                carbsGPer100g = null,
                fatGPer100g = null,
                servingSize = null,
            ),
        )
        advanceUntilIdle()

        assertEquals(1, foodRepository.upsertedFoods.size)
        val saved = foodRepository.upsertedFoods.single()
        assertEquals("Imported food", saved.name)
        assertEquals(null, saved.brand)
        assertEquals(null, saved.barcode)
        assertEquals(0.0, saved.caloriesKcal, 0.0)
    }

    @Test
    fun saveAndLogOnlineFood_partialNutrition_savesThenLogs() = runTest {
        val foodRepository = AddFoodFakeFoodRepository()
        val logRepository = AddFoodFakeLogRepository()
        val viewModel = AddFoodViewModel(
            foodRepository = foodRepository,
            logRepository = logRepository,
            settingsRepository = FakeSettingsRepository(enabled = true),
            providerExecutor = FakeProviderExecutor(),
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )

        viewModel.saveAndLogOnlineFood(
            food = RemoteFoodCandidate(
                source = RemoteFoodSource.STATIC_SAMPLE,
                sourceId = "edge-case-2",
                barcode = "123",
                name = "Sample item",
                brand = " ",
                caloriesKcalPer100g = null,
                proteinGPer100g = null,
                carbsGPer100g = null,
                fatGPer100g = null,
                servingSize = "?? weird-unit",
            ),
            mealType = MealType.DINNER,
            quantity = 1.0,
            unit = FoodUnit.SERVING,
        )
        advanceUntilIdle()

        assertEquals(1, foodRepository.upsertedFoods.size)
        assertEquals(1, logRepository.loggedEntries.size)
        assertEquals(foodRepository.upsertedFoods.single().id, logRepository.loggedEntries.single().foodItemId)
    }
}

private class AddFoodFakeFoodRepository(
    private val foods: List<FoodItem> = emptyList(),
    private val favorites: List<FoodItem> = emptyList(),
    private val recents: List<FoodItem> = emptyList(),
) : FoodRepository {
    val upsertedFoods = mutableListOf<FoodItem>()
    val searchQueries = mutableListOf<String>()

    override suspend fun upsertFood(foodItem: FoodItem) {
        upsertedFoods += foodItem
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
        return flowOf(favorites.take(limit))
    }

    override fun recentLoggedFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(recents.take(limit))
    }

    override fun allFoods(query: String): Flow<List<FoodItem>> {
        return flowOf(filterFoods(query))
    }

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(foods.take(limit))
    }

    override fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>> {
        searchQueries += query
        return flowOf(filterFoods(query).take(limit))
    }

    private fun filterFoods(query: String): List<FoodItem> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return foods
        }
        val normalizedQuery = trimmedQuery.lowercase()
        return foods.filter { food ->
            food.name.lowercase().contains(normalizedQuery) ||
                food.brand?.lowercase()?.contains(normalizedQuery) == true
        }
    }
}

private class AddFoodFakeLogRepository : LogRepository {
    val loggedEntries = mutableListOf<MealEntry>()

    override suspend fun logMealEntry(entry: MealEntry) {
        loggedEntries += entry
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

private class FakeSettingsRepository(
    enabled: Boolean,
) : SettingsRepository {
    override val onlineLookupEnabled: Flow<Boolean> = flowOf(enabled)

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        // no-op
    }
}

private class FakeProviderExecutor(
    private val results: List<RemoteFoodCandidate> = listOf(
        RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = "123",
            barcode = "123",
            name = "Oatmeal",
            brand = "OpenFoodFacts",
            caloriesKcalPer100g = 370.0,
            proteinGPer100g = 13.0,
            carbsGPer100g = 67.0,
            fatGPer100g = 7.0,
            servingSize = "40 g",
        ),
    ),
    private val throwable: Throwable? = null,
    private val delayMs: Long = 0L,
) : ProviderExecutor {
    var searchCalls: Int = 0
    val requests = mutableListOf<ProviderExecutionRequest>()

    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        searchCalls += 1
        requests += request
        if (delayMs > 0) {
            delay(delayMs)
        }
        throwable?.let { failure ->
            throw failure
        }
        val status = if (results.isEmpty()) ProviderStatus.EMPTY else ProviderStatus.AVAILABLE
        return ProviderExecutionReport(
            requestType = request.requestType,
            sourceFilter = request.sourceFilter,
            mergedCandidates = results.map { candidate ->
                ProviderMergedCandidate(
                    providerId = "test_provider",
                    candidate = candidate.copy(providerKey = "test_provider"),
                    dedupeKey = candidate.sourceId,
                )
            },
            providerResults = listOf(
                ProviderResult(
                    providerId = "test_provider",
                    capability = request.requestType.capability,
                    status = status,
                    items = results,
                    elapsedMs = delayMs,
                ),
            ),
            overallElapsedMs = delayMs,
        )
    }
}

private fun fakeFood(
    id: String,
    name: String,
    brand: String? = null,
): FoodItem {
    return FoodItem(
        id = id,
        name = name,
        brand = brand,
        barcode = null,
        caloriesKcal = 100.0,
        proteinG = 5.0,
        carbsG = 10.0,
        fatG = 2.0,
        isFavorite = false,
        isReportedIncorrect = false,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}

private class StatusOnlyProviderExecutor(
    private val providerId: String,
    private val status: ProviderStatus,
    private val diagnostics: String? = null,
) : ProviderExecutor {
    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
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

private class MultiStatusProviderExecutor(
    private val providerResults: List<ProviderResult>,
) : ProviderExecutor {
    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        return ProviderExecutionReport(
            requestType = request.requestType,
            sourceFilter = request.sourceFilter,
            mergedCandidates = emptyList(),
            providerResults = providerResults,
            overallElapsedMs = 0L,
        )
    }
}
