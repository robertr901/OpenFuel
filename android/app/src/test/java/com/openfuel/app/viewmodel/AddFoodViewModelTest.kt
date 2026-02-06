package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.data.remote.RemoteFoodDataSource
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun searchOnline_onlyRunsWhenExplicitlyRequested() = runTest {
        val remoteDataSource = FakeRemoteFoodDataSource()
        val viewModel = AddFoodViewModel(
            foodRepository = AddFoodFakeFoodRepository(),
            logRepository = AddFoodFakeLogRepository(),
            remoteFoodDataSource = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.updateSearchQuery("oat")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(0, remoteDataSource.searchCalls)

        viewModel.searchOnline()
        advanceUntilIdle()

        assertEquals(1, remoteDataSource.searchCalls)
        assertEquals(1, viewModel.uiState.value.onlineResults.size)
        collectJob.cancel()
    }
}

private class AddFoodFakeFoodRepository : FoodRepository {
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

    override fun favoriteFoods(limit: Int): Flow<List<FoodItem>> {
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
}

private class FakeRemoteFoodDataSource : RemoteFoodDataSource {
    var searchCalls: Int = 0

    override suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        searchCalls += 1
        return listOf(
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
        )
    }

    override suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        return null
    }
}
