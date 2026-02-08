package com.openfuel.app.domain.search

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedFoodSearchModelsTest {
    @Test
    fun mergeUnifiedSearchResults_returnsLocalFirstThenOnline() {
        val local = listOf(
            localFood(id = "l1", name = "Oatmeal", calories = 370.0),
            localFood(id = "l2", name = "Banana", calories = 89.0),
        )
        val online = listOf(
            remoteFood(id = "o1", name = "Greek Yogurt", calories = 60.0),
            remoteFood(id = "o2", name = "Cottage Cheese", calories = 98.0),
        )

        val merged = mergeUnifiedSearchResults(local, online)

        assertEquals(4, merged.size)
        assertTrue(merged[0] is UnifiedFoodSearchResult.LocalResult)
        assertTrue(merged[1] is UnifiedFoodSearchResult.LocalResult)
        assertTrue(merged[2] is UnifiedFoodSearchResult.OnlineResult)
        assertTrue(merged[3] is UnifiedFoodSearchResult.OnlineResult)
    }

    @Test
    fun mergeUnifiedSearchResults_dedupesOnlineByBarcodeAgainstLocal() {
        val local = listOf(
            localFood(id = "l1", name = "Coke Zero", calories = 0.0, barcode = "123456"),
        )
        val online = listOf(
            remoteFood(id = "o1", name = "Coke Zero", calories = 0.0, barcode = "123456"),
            remoteFood(id = "o2", name = "Sprite Zero", calories = 0.0, barcode = "222"),
        )

        val merged = mergeUnifiedSearchResults(local, online)

        assertEquals(2, merged.size)
        assertTrue(merged.any { result -> result is UnifiedFoodSearchResult.LocalResult })
        assertEquals(
            1,
            merged.count { result -> result is UnifiedFoodSearchResult.OnlineResult },
        )
    }

    @Test
    fun mergeUnifiedSearchResults_dedupesByNameBrandAndCaloriesWhenBarcodeMissing() {
        val local = listOf(
            localFood(
                id = "l1",
                name = "Greek Yogurt",
                brand = "Demo",
                calories = 60.0,
                barcode = null,
            ),
        )
        val online = listOf(
            remoteFood(
                id = "o1",
                name = "  greek  yogurt ",
                brand = "demo",
                calories = 62.0,
                barcode = null,
            ),
            remoteFood(
                id = "o2",
                name = "Kefir",
                brand = "Demo",
                calories = 65.0,
                barcode = null,
            ),
        )

        val merged = mergeUnifiedSearchResults(local, online)

        assertEquals(2, merged.size)
        assertEquals(
            1,
            merged.count { result -> result is UnifiedFoodSearchResult.OnlineResult },
        )
    }

    @Test
    fun applySourceFilter_filtersResultsDeterministically() {
        val merged = listOf(
            UnifiedFoodSearchResult.LocalResult(localFood(id = "l1", name = "Oatmeal", calories = 370.0)),
            UnifiedFoodSearchResult.OnlineResult(
                candidate = remoteFood(id = "o1", name = "Yogurt", calories = 60.0),
                isSavedLocally = false,
            ),
        )

        val localOnly = applySourceFilter(merged, SearchSourceFilter.LOCAL_ONLY)
        val onlineOnly = applySourceFilter(merged, SearchSourceFilter.ONLINE_ONLY)
        val all = applySourceFilter(merged, SearchSourceFilter.ALL)

        assertEquals(1, localOnly.size)
        assertTrue(localOnly.first() is UnifiedFoodSearchResult.LocalResult)
        assertEquals(1, onlineOnly.size)
        assertTrue(onlineOnly.first() is UnifiedFoodSearchResult.OnlineResult)
        assertEquals(2, all.size)
    }
}

private fun localFood(
    id: String,
    name: String,
    calories: Double,
    brand: String? = null,
    barcode: String? = null,
): FoodItem {
    return FoodItem(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        caloriesKcal = calories,
        proteinG = 0.0,
        carbsG = 0.0,
        fatG = 0.0,
        isFavorite = false,
        isReportedIncorrect = false,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}

private fun remoteFood(
    id: String,
    name: String,
    calories: Double,
    brand: String? = null,
    barcode: String? = null,
): RemoteFoodCandidate {
    return RemoteFoodCandidate(
        source = RemoteFoodSource.OPEN_FOOD_FACTS,
        sourceId = id,
        barcode = barcode,
        name = name,
        brand = brand,
        caloriesKcalPer100g = calories,
        proteinGPer100g = 0.0,
        carbsGPer100g = 0.0,
        fatGPer100g = 0.0,
        servingSize = null,
    )
}
