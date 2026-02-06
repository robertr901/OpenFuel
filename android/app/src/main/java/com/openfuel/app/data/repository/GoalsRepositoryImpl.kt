package com.openfuel.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.openfuel.app.data.datastore.SettingsKeys
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.repository.GoalsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : GoalsRepository {
    override fun goalForDate(date: LocalDate): Flow<DailyGoal?> {
        return dataStore.data.map { preferences ->
            val calories = preferences[SettingsKeys.GOAL_CALORIES_KCAL]
            val protein = preferences[SettingsKeys.GOAL_PROTEIN_G]
            val carbs = preferences[SettingsKeys.GOAL_CARBS_G]
            val fat = preferences[SettingsKeys.GOAL_FAT_G]

            if (calories == null && protein == null && carbs == null && fat == null) {
                null
            } else {
                DailyGoal(
                    date = date,
                    caloriesKcalTarget = calories ?: 0.0,
                    proteinGTarget = protein ?: 0.0,
                    carbsGTarget = carbs ?: 0.0,
                    fatGTarget = fat ?: 0.0,
                )
            }
        }
    }

    override suspend fun upsertGoal(goal: DailyGoal) {
        dataStore.edit { preferences ->
            writeNullableGoal(preferences, SettingsKeys.GOAL_CALORIES_KCAL, goal.caloriesKcalTarget)
            writeNullableGoal(preferences, SettingsKeys.GOAL_PROTEIN_G, goal.proteinGTarget)
            writeNullableGoal(preferences, SettingsKeys.GOAL_CARBS_G, goal.carbsGTarget)
            writeNullableGoal(preferences, SettingsKeys.GOAL_FAT_G, goal.fatGTarget)
        }
    }
}

private fun writeNullableGoal(
    preferences: androidx.datastore.preferences.core.MutablePreferences,
    key: androidx.datastore.preferences.core.Preferences.Key<Double>,
    value: Double,
) {
    if (value <= 0.0) {
        preferences.remove(key)
    } else {
        preferences[key] = value
    }
}
