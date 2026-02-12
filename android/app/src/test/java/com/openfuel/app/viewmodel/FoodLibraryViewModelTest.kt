package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateSearchQuery_afterDebounce_updatesUiStateWithFilteredFoods() = runTest {
        val repository = FakeFoodRepository()
        val logRepository = FakeLibraryLogRepository()
        val viewModel = FoodLibraryViewModel(repository, logRepository)
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("oat", viewModel.uiState.value.searchQuery)
        assertEquals(1, viewModel.uiState.value.foods.size)
        assertEquals("Oats", viewModel.uiState.value.foods.first().name)
        collectJob.cancel()
    }

    @Test
    fun blankQuery_emitsRecentAndFavoritesSections() = runTest {
        val repository = FakeFoodRepository()
        val logRepository = FakeLibraryLogRepository()
        val viewModel = FoodLibraryViewModel(repository, logRepository)
        val collectJob = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(3, viewModel.uiState.value.recentFoods.size)
        assertEquals(1, viewModel.uiState.value.favoriteFoods.size)
        assertEquals("Greek Yogurt", viewModel.uiState.value.favoriteFoods.first().name)
        assertEquals(0, viewModel.uiState.value.localResults.size)
        collectJob.cancel()
    }

    @Test
    fun quickLog_logsMealEntryWithDefaultServing() = runTest {
        val repository = FakeFoodRepository()
        val logRepository = FakeLibraryLogRepository()
        val viewModel = FoodLibraryViewModel(repository, logRepository)
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.logFood(foodId = "food-1", mealType = MealType.LUNCH)
        advanceUntilIdle()

        assertEquals(1, logRepository.entries.size)
        assertEquals("food-1", logRepository.entries.first().foodItemId)
        assertEquals(MealType.LUNCH, logRepository.entries.first().mealType)
        assertEquals("Food logged.", viewModel.uiState.value.message)
        collectJob.cancel()
    }

    @Test
    fun consumeMessage_clearsTransientMessage() = runTest {
        val repository = FakeFoodRepository()
        val logRepository = FakeLibraryLogRepository()
        val viewModel = FoodLibraryViewModel(repository, logRepository)
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.logFood(foodId = "food-1", mealType = MealType.DINNER)
        advanceUntilIdle()
        assertEquals("Food logged.", viewModel.uiState.value.message)

        viewModel.consumeMessage()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.message)
        collectJob.cancel()
    }
}

private class FakeFoodRepository : FoodRepository {
    private val foods = listOf(
        FoodItem(
            id = "food-1",
            name = "Oats",
            brand = "OpenFuel",
            caloriesKcal = 150.0,
            proteinG = 5.0,
            carbsG = 27.0,
            fatG = 3.0,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        ),
        FoodItem(
            id = "food-2",
            name = "Eggs",
            brand = "Farm",
            caloriesKcal = 72.0,
            proteinG = 6.0,
            carbsG = 0.0,
            fatG = 5.0,
            createdAt = Instant.parse("2024-01-02T00:00:00Z"),
        ),
        FoodItem(
            id = "food-3",
            name = "Greek Yogurt",
            brand = "OpenFuel",
            caloriesKcal = 110.0,
            proteinG = 18.0,
            carbsG = 5.0,
            fatG = 0.0,
            isFavorite = true,
            createdAt = Instant.parse("2024-01-03T00:00:00Z"),
        ),
    )

    override suspend fun upsertFood(foodItem: FoodItem) {
        // no-op for tests
    }

    override suspend fun getFoodById(id: String): FoodItem? {
        return foods.firstOrNull { it.id == id }
    }

    override suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        return foods.firstOrNull { it.barcode == barcode }
    }

    override suspend fun setFavorite(foodId: String, isFavorite: Boolean) {
        // no-op for tests
    }

    override suspend fun setReportedIncorrect(foodId: String, isReportedIncorrect: Boolean) {
        // no-op for tests
    }

    override fun favoriteFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(foods.filter { it.isFavorite }.take(limit))
    }

    override fun recentLoggedFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(foods.take(limit))
    }

    override fun allFoods(query: String): Flow<List<FoodItem>> {
        val normalized = query.trim().lowercase()
        val filtered = if (normalized.isBlank()) {
            emptyList()
        } else {
            foods.filter { food ->
                food.name.lowercase().contains(normalized) ||
                    food.brand.orEmpty().lowercase().contains(normalized)
            }
        }
        return flowOf(filtered)
    }

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> {
        return flowOf(foods.take(limit))
    }

    override fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>> {
        return allFoods(query)
    }
}

private class FakeLibraryLogRepository : LogRepository {
    val entries = mutableListOf<MealEntry>()

    override suspend fun logMealEntry(entry: MealEntry) {
        entries += entry
    }

    override suspend fun updateMealEntry(entry: MealEntry) = Unit

    override suspend fun deleteMealEntry(id: String) = Unit

    override fun entriesForDate(
        date: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> = flowOf(emptyList())

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> = flowOf(emptyList())

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
}
