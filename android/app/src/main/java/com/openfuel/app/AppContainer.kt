package com.openfuel.app

import android.content.Context
import com.openfuel.app.data.datastore.settingsDataStore
import com.openfuel.app.data.db.OpenFuelDatabase
import com.openfuel.app.data.repository.FoodRepositoryImpl
import com.openfuel.app.data.repository.GoalsRepositoryImpl
import com.openfuel.app.data.repository.LogRepositoryImpl
import com.openfuel.app.data.repository.SettingsRepositoryImpl
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.export.ExportManager

class AppContainer(context: Context) {
    private val database: OpenFuelDatabase = OpenFuelDatabase.build(context)

    val foodRepository: FoodRepository = FoodRepositoryImpl(database.foodDao())
    val logRepository: LogRepository = LogRepositoryImpl(database.mealEntryDao())
    val goalsRepository: GoalsRepository = GoalsRepositoryImpl(context.settingsDataStore)
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(context.settingsDataStore)

    val exportManager: ExportManager = ExportManager(
        foodDao = database.foodDao(),
        mealEntryDao = database.mealEntryDao(),
        goalsRepository = goalsRepository,
    )
}
