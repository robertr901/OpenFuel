package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.repository.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateSearchQuery_afterDebounce_updatesUiStateWithFilteredFoods() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodLibraryViewModel(repository)
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
    )

    override suspend fun upsertFood(foodItem: FoodItem) {
        // no-op for tests
    }

    override suspend fun getFoodById(id: String): FoodItem? {
        return foods.firstOrNull { it.id == id }
    }

    override fun allFoods(query: String): Flow<List<FoodItem>> {
        val normalized = query.trim().lowercase()
        val filtered = if (normalized.isBlank()) {
            foods
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
