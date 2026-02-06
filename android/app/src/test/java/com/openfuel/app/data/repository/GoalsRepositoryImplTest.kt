package com.openfuel.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.openfuel.app.domain.model.DailyGoal
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun goalForDate_whenUnset_returnsNull() = runTest {
        val repository = createRepository(backgroundScope)

        val goal = repository.goalForDate(LocalDate.parse("2024-01-01")).first()

        assertNull(goal)
    }

    @Test
    fun upsertGoal_persistsGlobalGoalForAnyDate() = runTest {
        val repository = createRepository(backgroundScope)
        repository.upsertGoal(
            DailyGoal(
                date = LocalDate.parse("2024-01-01"),
                caloriesKcalTarget = 2000.0,
                proteinGTarget = 120.0,
                carbsGTarget = 250.0,
                fatGTarget = 70.0,
            ),
        )

        val firstDay = repository.goalForDate(LocalDate.parse("2024-01-01")).first()
        val secondDay = repository.goalForDate(LocalDate.parse("2024-01-15")).first()

        assertEquals(2000.0, firstDay?.caloriesKcalTarget ?: 0.0, 0.0001)
        assertEquals(120.0, firstDay?.proteinGTarget ?: 0.0, 0.0001)
        assertEquals(LocalDate.parse("2024-01-15"), secondDay?.date)
        assertEquals(250.0, secondDay?.carbsGTarget ?: 0.0, 0.0001)
    }

    @Test
    fun upsertGoal_withZeroTargets_clearsGoals() = runTest {
        val repository = createRepository(backgroundScope)
        repository.upsertGoal(
            DailyGoal(
                date = LocalDate.parse("2024-01-01"),
                caloriesKcalTarget = 1800.0,
                proteinGTarget = 100.0,
                carbsGTarget = 210.0,
                fatGTarget = 60.0,
            ),
        )

        repository.upsertGoal(
            DailyGoal(
                date = LocalDate.parse("2024-01-02"),
                caloriesKcalTarget = 0.0,
                proteinGTarget = 0.0,
                carbsGTarget = 0.0,
                fatGTarget = 0.0,
            ),
        )

        val goal = repository.goalForDate(LocalDate.parse("2024-02-01")).first()

        assertNull(goal)
    }

    private fun createRepository(scope: CoroutineScope): GoalsRepositoryImpl {
        val storageFile = tempFolder.newFile("settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { storageFile },
        )
        return GoalsRepositoryImpl(dataStore)
    }
}
