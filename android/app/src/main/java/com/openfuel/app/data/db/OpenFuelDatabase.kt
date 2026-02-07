package com.openfuel.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodItemEntity::class,
        MealEntryEntity::class,
        DailyGoalEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class OpenFuelDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun dailyGoalDao(): DailyGoalDao

    companion object {
        private const val DB_NAME = "openfuel.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_food_items_name ON food_items(name)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_food_items_brand ON food_items(brand)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_food_items_name_brand ON food_items(name, brand)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE food_items ADD COLUMN barcode TEXT",
                )
                db.execSQL(
                    "ALTER TABLE food_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_food_items_barcode ON food_items(barcode)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE food_items ADD COLUMN isReportedIncorrect INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun build(context: Context): OpenFuelDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OpenFuelDatabase::class.java,
                DB_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
