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
import com.openfuel.app.domain.service.FoodCatalogProvider
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
        val remoteDataSource = FakeRemoteFoodDataSource(delayMs = 1_000)
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(
                foods = listOf(
                    fakeFood(id = "local-1", name = "Oatmeal"),
                ),
            ),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            foodCatalogProvider = remoteDataSource,
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
        val remoteDataSource = FakeRemoteFoodDataSource()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(
                foods = listOf(
                    fakeFood(id = "f1", name = "Oatmeal"),
                    fakeFood(id = "f2", name = "Greek Yogurt"),
                ),
            ),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            foodCatalogProvider = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(0, remoteDataSource.searchCalls)
        assertEquals(1, viewModel.uiState.value.foods.size)
        assertEquals("Oatmeal", viewModel.uiState.value.foods.first().name)
        collectJob.cancel()
    }

    @Test
    fun searchOnline_whenDisabled_skipsNetworkCallAndShowsMessage() = runTest {
        val remoteDataSource = FakeRemoteFoodDataSource()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = false),
            foodCatalogProvider = remoteDataSource,
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
        val remoteDataSource = FakeRemoteFoodDataSource(results = emptyList())
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            foodCatalogProvider = remoteDataSource,
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
        val remoteDataSource = FakeRemoteFoodDataSource(
            throwable = IllegalStateException("boom"),
        )
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            foodCatalogProvider = remoteDataSource,
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
    fun updateSearchQuery_clearsPreviousOnlineResultsAndAttemptState() = runTest {
        val remoteDataSource = FakeRemoteFoodDataSource()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            settingsRepository = FakeSettingsRepository(enabled = true),
            foodCatalogProvider = remoteDataSource,
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
}

private class AddFoodFakeFoodRepository(
    private val foods: List<FoodItem> = emptyList(),
) : FoodRepository {
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
        return flowOf(filterFoods(query))
    }

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(foods.take(limit))
    }

    override fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>> {
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

private class FakeSettingsRepository(
    enabled: Boolean,
) : SettingsRepository {
    override val onlineLookupEnabled: Flow<Boolean> = flowOf(enabled)

    override suspend fun setOnlineLookupEnabled(enabled: Boolean) {
        // no-op
    }
}

private class FakeRemoteFoodDataSource(
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
) : FoodCatalogProvider {
    var searchCalls: Int = 0

    override suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        searchCalls += 1
        if (delayMs > 0) {
            delay(delayMs)
        }
        throwable?.let { failure ->
            throw failure
        }
        return results
    }

    override suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        return null
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
