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
    version = 1,
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
                // Placeholder for future migrations.
            }
        }

        fun build(context: Context): OpenFuelDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OpenFuelDatabase::class.java,
                DB_NAME,
            )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
