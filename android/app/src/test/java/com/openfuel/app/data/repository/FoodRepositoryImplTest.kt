package com.openfuel.app.data.repository

import com.openfuel.app.data.db.FoodDao
import com.openfuel.app.data.db.FoodItemEntity
import com.openfuel.app.domain.model.FoodItem
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodRepositoryImplTest {
    @Test
    fun escapeLikeQuery_escapesSqlWildcardsAndBackslash() {
        val escaped = escapeLikeQuery("100%_\\test")

        assertEquals("100\\%\\_\\\\test", escaped)
    }

    @Test
    fun upsertFood_whenBarcodeAlreadyExists_reusesExistingId() = runTest {
        val fakeDao = FakeFoodDao()
        fakeDao.upsertFood(
            FoodItemEntity(
                id = "existing-id",
                name = "Old Food",
                brand = "Brand",
                barcode = "12345",
                caloriesKcal = 100.0,
                proteinG = 10.0,
                carbsG = 10.0,
                fatG = 10.0,
                isFavorite = true,
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            ),
        )
        val repository = FoodRepositoryImpl(fakeDao)

        repository.upsertFood(
            FoodItem(
                id = "new-id",
                name = "Imported Food",
                brand = "OpenFoodFacts",
                barcode = "12345",
                caloriesKcal = 250.0,
                proteinG = 5.0,
                carbsG = 30.0,
                fatG = 7.0,
                isFavorite = false,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            ),
        )

        assertNull(fakeDao.getFoodById("new-id"))
        val merged = fakeDao.getFoodById("existing-id")
        assertEquals("Imported Food", merged?.name)
        assertEquals(true, merged?.isFavorite)
        assertEquals("12345", merged?.barcode)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), merged?.createdAt)
    }
}

private class FakeFoodDao : FoodDao {
    private val storage = LinkedHashMap<String, FoodItemEntity>()

    override suspend fun upsertFood(foodItem: FoodItemEntity) {
        storage[foodItem.id] = foodItem
    }

    override suspend fun getFoodById(id: String): FoodItemEntity? {
        return storage[id]
    }

    override suspend fun getFoodByBarcode(barcode: String): FoodItemEntity? {
        return storage.values.firstOrNull { it.barcode == barcode }
    }

    override suspend fun updateFavorite(id: String, isFavorite: Boolean) {
        val current = storage[id] ?: return
        storage[id] = current.copy(isFavorite = isFavorite)
    }

    override fun observeFavoriteFoods(limit: Int): Flow<List<FoodItemEntity>> {
        return flowOf(
            storage.values
                .filter { it.isFavorite }
                .sortedByDescending { it.createdAt }
                .take(limit),
        )
    }

    override fun observeRecentLoggedFoods(limit: Int): Flow<List<FoodItemEntity>> {
        return flowOf(storage.values.take(limit))
    }

    override fun observeAllFoods(): Flow<List<FoodItemEntity>> {
        return flowOf(storage.values.toList())
    }

    override fun observeAllFoodsBySearch(escapedQuery: String): Flow<List<FoodItemEntity>> {
        val normalized = escapedQuery.lowercase()
        return flowOf(
            storage.values.filter { food ->
                food.name.lowercase().contains(normalized) ||
                    food.brand.orEmpty().lowercase().contains(normalized)
            },
        )
    }

    override fun observeRecentFoods(limit: Int): Flow<List<FoodItemEntity>> {
        return flowOf(
            storage.values
                .sortedByDescending { it.createdAt }
                .take(limit),
        )
    }

    override fun observeFoodsBySearch(escapedQuery: String, limit: Int): Flow<List<FoodItemEntity>> {
        val normalized = escapedQuery.lowercase()
        return flowOf(
            storage.values
                .filter { food ->
                    food.name.lowercase().contains(normalized) ||
                        food.brand.orEmpty().lowercase().contains(normalized)
                }
                .take(limit),
        )
    }

    override suspend fun getAllFoods(): List<FoodItemEntity> {
        return storage.values.toList()
    }
}
