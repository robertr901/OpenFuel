package com.openfuel.app.viewmodel

import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.data.remote.RemoteFoodDataSource
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.data.remote.UserInitiatedNetworkToken
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanBarcodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onBarcodeDetected_success_setsPreviewFood() = runTest {
        val remoteDataSource = ScanFakeRemoteFoodDataSource()
        val viewModel = ScanBarcodeViewModel(
            remoteFoodDataSource = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.onBarcodeDetected("123456")
        advanceUntilIdle()

        assertEquals(1, remoteDataSource.lookupCalls)
        assertEquals("123456", viewModel.uiState.value.lastBarcode)
        assertNotNull(viewModel.uiState.value.previewFood)
        assertNull(viewModel.uiState.value.errorMessage)
        collectJob.cancel()
    }

    @Test
    fun onBarcodeDetected_notFound_setsError() = runTest {
        val remoteDataSource = ScanFakeRemoteFoodDataSource(foundBarcode = null)
        val viewModel = ScanBarcodeViewModel(
            remoteFoodDataSource = remoteDataSource,
            userInitiatedNetworkGuard = UserInitiatedNetworkGuard(),
            foodRepository = ScanFakeFoodRepository(),
            logRepository = ScanFakeLogRepository(),
        )
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.onBarcodeDetected("999")
        advanceUntilIdle()

        assertEquals("No matching food found for barcode.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.previewFood)
        collectJob.cancel()
    }
}

private class ScanFakeRemoteFoodDataSource(
    private val foundBarcode: String? = "123456",
) : RemoteFoodDataSource {
    var lookupCalls: Int = 0

    override suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        return emptyList()
    }

    override suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        lookupCalls += 1
        if (barcode != foundBarcode) {
            return null
        }
        return RemoteFoodCandidate(
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

    override fun loggedDates(zoneId: ZoneId): Flow<List<LocalDate>> {
        return flowOf(emptyList())
    }
}
