package com.openfuel.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealEntryDaoInstrumentedTest {
    private lateinit var database: OpenFuelDatabase
    private lateinit var mealEntryDao: MealEntryDao
    private lateinit var foodDao: FoodDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Context
        database = Room.inMemoryDatabaseBuilder(context, OpenFuelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mealEntryDao = database.mealEntryDao()
        foodDao = database.foodDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeEntryTimestampsDesc_returnsNewestFirst() = runBlocking {
        foodDao.upsertFood(
            FoodItemEntity(
                id = "food-1",
                name = "Oats",
                brand = null,
                caloriesKcal = 100.0,
                proteinG = 5.0,
                carbsG = 20.0,
                fatG = 2.0,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        mealEntryDao.upsertEntry(
            MealEntryEntity(
                id = "entry-1",
                timestamp = Instant.parse("2026-01-01T08:00:00Z"),
                mealType = MealType.BREAKFAST,
                foodItemId = "food-1",
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            ),
        )
        mealEntryDao.upsertEntry(
            MealEntryEntity(
                id = "entry-2",
                timestamp = Instant.parse("2026-01-02T08:00:00Z"),
                mealType = MealType.BREAKFAST,
                foodItemId = "food-1",
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            ),
        )

        val timestamps = mealEntryDao.observeEntryTimestampsDesc().first()

        assertEquals(
            listOf(
                Instant.parse("2026-01-02T08:00:00Z"),
                Instant.parse("2026-01-01T08:00:00Z"),
            ),
            timestamps,
        )
    }
}
