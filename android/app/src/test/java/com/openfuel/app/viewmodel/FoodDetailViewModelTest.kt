package com.openfuel.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsFoodFromRepository() = runTest {
        val foodRepository = FoodDetailFakeFoodRepository()
        val food = foodRepository.seedFood()
        val viewModel = FoodDetailViewModel(
            foodRepository = foodRepository,
            logRepository = FoodDetailFakeLogRepository(),
            savedStateHandle = SavedStateHandle(mapOf("foodId" to food.id)),
        )

        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.food)
        assertEquals(food.id, viewModel.uiState.value.food?.id)
    }

    @Test
    fun logFood_invalidQuantity_setsErrorMessage() = runTest {
        val foodRepository = FoodDetailFakeFoodRepository()
        val food = foodRepository.seedFood()
        val viewModel = FoodDetailViewModel(
            foodRepository = foodRepository,
            logRepository = FoodDetailFakeLogRepository(),
            savedStateHandle = SavedStateHandle(mapOf("foodId" to food.id)),
        )
        advanceUntilIdle()

        viewModel.logFood(quantity = 0.0, unit = FoodUnit.SERVING, mealType = MealType.BREAKFAST)
        advanceUntilIdle()

        assertEquals("Enter a valid quantity", viewModel.uiState.value.message)
    }

    @Test
    fun toggleFavorite_updatesFoodState() = runTest {
        val foodRepository = FoodDetailFakeFoodRepository()
        val food = foodRepository.seedFood()
        val viewModel = FoodDetailViewModel(
            foodRepository = foodRepository,
            logRepository = FoodDetailFakeLogRepository(),
            savedStateHandle = SavedStateHandle(mapOf("foodId" to food.id)),
        )
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.food?.isFavorite)
    }
}

private class FoodDetailFakeFoodRepository : FoodRepository {
    private val foods = mutableMapOf<String, FoodItem>()

    fun seedFood(): FoodItem {
        val food = FoodItem(
            id = "food-1",
            name = "Oats",
            brand = "OpenFuel",
            caloriesKcal = 120.0,
            proteinG = 4.0,
            carbsG = 22.0,
            fatG = 2.0,
            createdAt = Instant.parse("2026-02-07T00:00:00Z"),
        )
        foods[food.id] = food
        return food
    }

    override suspend fun upsertFood(foodItem: FoodItem) {
        foods[foodItem.id] = foodItem
    }

    override suspend fun getFoodById(id: String): FoodItem? = foods[id]

    override suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        return foods.values.firstOrNull { it.barcode == barcode }
    }

    override suspend fun setFavorite(foodId: String, isFavorite: Boolean) {
        val existing = foods[foodId] ?: return
        foods[foodId] = existing.copy(isFavorite = isFavorite)
    }

    override suspend fun setReportedIncorrect(foodId: String, isReportedIncorrect: Boolean) {
        val existing = foods[foodId] ?: return
        foods[foodId] = existing.copy(isReportedIncorrect = isReportedIncorrect)
    }

    override fun favoriteFoods(limit: Int): Flow<List<FoodItem>> = flowOf(emptyList())

    override fun recentLoggedFoods(limit: Int): Flow<List<FoodItem>> = flowOf(emptyList())

    override fun allFoods(query: String): Flow<List<FoodItem>> = flowOf(foods.values.toList())

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> = flowOf(foods.values.toList())

    override fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>> = flowOf(foods.values.toList())
}

private class FoodDetailFakeLogRepository : LogRepository {
    override suspend fun logMealEntry(entry: MealEntry) = Unit

    override suspend fun updateMealEntry(entry: MealEntry) = Unit

    override suspend fun deleteMealEntry(id: String) = Unit

    override fun entriesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<MealEntryWithFood>> = flowOf(emptyList())

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> = flowOf(emptyList())

    override fun entriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
}
