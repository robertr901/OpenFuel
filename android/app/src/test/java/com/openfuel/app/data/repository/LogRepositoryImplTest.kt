package com.openfuel.app.data.repository

import com.openfuel.app.data.db.FoodDao
import com.openfuel.app.data.db.FoodItemEntity
import com.openfuel.app.data.db.MealEntryDao
import com.openfuel.app.data.db.MealEntryEntity
import com.openfuel.app.data.db.MealEntryWithFoodEntity
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogRepositoryImplTest {
    @Test
    fun loggedDates_deduplicatesByLocalDate_inDescendingOrder() = runTest {
        val dao = FakeMealEntryDao()
        val repository = LogRepositoryImpl(dao)

        dao.timestamps.value = listOf(
            Instant.parse("2026-03-03T10:00:00Z"),
            Instant.parse("2026-03-03T08:00:00Z"),
            Instant.parse("2026-03-02T23:00:00Z"),
            Instant.parse("2026-03-01T23:00:00Z"),
        )

        val dates = repository.loggedDates(ZoneId.of("UTC")).first()

        assertEquals(
            listOf(
                LocalDate.parse("2026-03-03"),
                LocalDate.parse("2026-03-02"),
                LocalDate.parse("2026-03-01"),
            ),
            dates,
        )
    }

    @Test
    fun loggedDates_usesSqlAggregatedDates_forSystemZone() = runTest {
        val dao = FakeMealEntryDao()
        val repository = LogRepositoryImpl(dao)

        dao.loggedDateStrings.value = listOf("2026-03-04", "2026-03-03")

        val dates = repository.loggedDates(ZoneId.systemDefault()).first()

        assertEquals(
            listOf(
                LocalDate.parse("2026-03-04"),
                LocalDate.parse("2026-03-03"),
            ),
            dates,
        )
    }

    @Test
    fun logMealEntry_throws_whenFoodReferenceMissing() = runTest {
        val mealEntryDao = FakeMealEntryDao()
        val foodDao = LogRepositoryFakeFoodDao(foodIds = emptySet())
        val repository = LogRepositoryImpl(mealEntryDao, foodDao)

        val error = try {
            repository.logMealEntry(sampleMealEntry(foodItemId = "missing-food"))
            fail("Expected IllegalStateException when food reference is missing.")
            return@runTest
        } catch (exception: IllegalStateException) {
            exception
        }

        assertEquals(null, mealEntryDao.lastUpsertedEntry)
        assertEquals("Could not save meal entry. Food reference not found.", error.message)
    }

    @Test
    fun logMealEntry_inserts_whenFoodReferenceExists() = runTest {
        val mealEntryDao = FakeMealEntryDao()
        val foodDao = LogRepositoryFakeFoodDao(foodIds = setOf("food-1"))
        val repository = LogRepositoryImpl(mealEntryDao, foodDao)

        repository.logMealEntry(sampleMealEntry(foodItemId = "food-1"))

        assertEquals("food-1", mealEntryDao.lastUpsertedEntry?.foodItemId)
    }

    private fun sampleMealEntry(foodItemId: String): MealEntry {
        return MealEntry(
            id = "entry-1",
            timestamp = Instant.parse("2026-03-04T08:00:00Z"),
            mealType = MealType.BREAKFAST,
            foodItemId = foodItemId,
            quantity = 1.0,
            unit = FoodUnit.SERVING,
        )
    }
}

private class FakeMealEntryDao : MealEntryDao {
    val timestamps = MutableStateFlow<List<Instant>>(emptyList())
    val loggedDateStrings = MutableStateFlow<List<String>>(emptyList())
    var lastUpsertedEntry: MealEntryEntity? = null

    override suspend fun upsertEntry(entry: MealEntryEntity) {
        lastUpsertedEntry = entry
    }

    override suspend fun deleteById(id: String) {
        // no-op
    }

    override fun observeEntriesForDay(
        start: Instant,
        end: Instant,
    ): Flow<List<MealEntryWithFoodEntity>> {
        return flowOf(emptyList())
    }

    override fun observeEntriesBetween(
        start: Instant,
        end: Instant,
    ): Flow<List<MealEntryWithFoodEntity>> {
        return flowOf(emptyList())
    }

    override fun observeEntryTimestampsDesc(): Flow<List<Instant>> {
        return timestamps
    }

    override fun observeLoggedLocalDatesDesc(): Flow<List<String>> {
        return loggedDateStrings
    }

    override suspend fun getAllEntries(): List<MealEntryEntity> {
        return emptyList()
    }
}

private class LogRepositoryFakeFoodDao(
    private val foodIds: Set<String>,
) : FoodDao {
    override suspend fun upsertFood(foodItem: FoodItemEntity) = Unit

    override suspend fun getFoodById(id: String): FoodItemEntity? {
        if (id !in foodIds) return null
        return FoodItemEntity(
            id = id,
            name = "Test Food",
            brand = null,
            caloriesKcal = 100.0,
            proteinG = 1.0,
            carbsG = 1.0,
            fatG = 1.0,
            createdAt = Instant.parse("2026-03-01T00:00:00Z"),
        )
    }

    override suspend fun getFoodByBarcode(barcode: String): FoodItemEntity? = null

    override suspend fun updateFavorite(id: String, isFavorite: Boolean) = Unit

    override suspend fun updateReportedIncorrect(id: String, isReportedIncorrect: Boolean) = Unit

    override fun observeFavoriteFoods(limit: Int): Flow<List<FoodItemEntity>> {
        return flowOf(emptyList())
    }

    override fun observeRecentLoggedFoods(limit: Int): Flow<List<FoodItemEntity>> {
        return flowOf(emptyList())
    }

    override fun observeAllFoods(): Flow<List<FoodItemEntity>> {
        return flowOf(emptyList())
    }

    override fun observeAllFoodsBySearch(escapedQuery: String): Flow<List<FoodItemEntity>> {
        return flowOf(emptyList())
    }

    override fun observeRecentFoods(limit: Int): Flow<List<FoodItemEntity>> {
        return flowOf(emptyList())
    }

    override fun observeFoodsBySearch(
        escapedQuery: String,
        limit: Int,
    ): Flow<List<FoodItemEntity>> {
        return flowOf(emptyList())
    }

    override suspend fun getAllFoods(): List<FoodItemEntity> = emptyList()
}
