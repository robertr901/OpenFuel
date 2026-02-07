package com.openfuel.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenFuelDatabaseMigrationTest {
    private lateinit var context: Context
    private val dbName = "openfuel-migration-test.db"
    private var database: OpenFuelDatabase? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrateFromV1ToV4_preservesRowsAndAddsExpectedColumns() {
        createVersion1Database()

        database = Room.databaseBuilder(context, OpenFuelDatabase::class.java, dbName)
            .addMigrations(
                OpenFuelDatabase.MIGRATION_1_2,
                OpenFuelDatabase.MIGRATION_2_3,
                OpenFuelDatabase.MIGRATION_3_4,
            )
            .allowMainThreadQueries()
            .build()

        val migratedDb = database!!.openHelper.writableDatabase

        assertColumnExists(migratedDb, table = "food_items", column = "barcode")
        assertColumnExists(migratedDb, table = "food_items", column = "isFavorite")
        assertColumnExists(migratedDb, table = "food_items", column = "isReportedIncorrect")
        assertIndexExists(migratedDb, table = "food_items", index = "index_food_items_name_brand")
        assertIndexExists(migratedDb, table = "food_items", index = "index_food_items_barcode")

        migratedDb.query(
            "SELECT barcode, isFavorite, isReportedIncorrect FROM food_items WHERE id = 'food-1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
        }
    }

    private fun createVersion1Database() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_items (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                brand TEXT,
                caloriesKcal REAL NOT NULL,
                proteinG REAL NOT NULL,
                carbsG REAL NOT NULL,
                fatG REAL NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        sqliteDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS meal_entries (
                id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                mealType TEXT NOT NULL,
                foodItemId TEXT NOT NULL,
                quantity REAL NOT NULL,
                unit TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        sqliteDb.execSQL(
            "CREATE INDEX IF NOT EXISTS index_meal_entries_timestamp ON meal_entries(timestamp)",
        )
        sqliteDb.execSQL(
            "CREATE INDEX IF NOT EXISTS index_meal_entries_foodItemId ON meal_entries(foodItemId)",
        )
        sqliteDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_goals (
                date TEXT NOT NULL,
                caloriesKcalTarget REAL NOT NULL,
                proteinGTarget REAL NOT NULL,
                carbsGTarget REAL NOT NULL,
                fatGTarget REAL NOT NULL,
                PRIMARY KEY(date)
            )
            """.trimIndent(),
        )
        sqliteDb.execSQL(
            """
            INSERT INTO food_items (
                id, name, brand, caloriesKcal, proteinG, carbsG, fatG, createdAt
            ) VALUES (
                'food-1', 'Oats', NULL, 150.0, 5.0, 27.0, 3.0, 1704067200000
            )
            """.trimIndent(),
        )
        sqliteDb.execSQL("PRAGMA user_version = 1")
        sqliteDb.close()
    }

    private fun assertColumnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ) {
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    found = true
                    break
                }
            }
            assertTrue("Expected column '$column' in table '$table'", found)
        }
    }

    private fun assertIndexExists(
        db: SupportSQLiteDatabase,
        table: String,
        index: String,
    ) {
        db.query("PRAGMA index_list($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == index) {
                    found = true
                    break
                }
            }
            assertTrue("Expected index '$index' for table '$table'", found)
        }
    }
}
