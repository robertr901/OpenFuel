package com.openfuel.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.openfuel.app.MainDispatcherRule
import com.openfuel.app.data.datastore.SettingsKeys
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.GoalProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun profileFields_whenUnset_returnSafeDefaults() = runTest {
        val fixture = createFixture(backgroundScope)

        assertNull(fixture.repository.goalProfile.first())
        assertEquals(emptySet<DietaryOverlay>(), fixture.repository.goalProfileOverlays.first())
        assertFalse(fixture.repository.goalProfileOnboardingCompleted.first())
        assertFalse(fixture.repository.goalsCustomised.first())
        assertNull(fixture.repository.weeklyReviewDismissedWeekStartEpochDay.first())
    }

    @Test
    fun profileFields_roundTrip_throughDataStore() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.repository.setGoalProfile(GoalProfile.MUSCLE_GAIN)
        fixture.repository.setGoalProfileOverlays(
            setOf(DietaryOverlay.LOW_FODMAP, DietaryOverlay.LOW_SODIUM),
        )
        fixture.repository.setGoalProfileOnboardingCompleted(true)
        fixture.repository.setGoalsCustomised(true)
        fixture.repository.setWeeklyReviewDismissedWeekStartEpochDay(20_128L)

        assertEquals(GoalProfile.MUSCLE_GAIN, fixture.repository.goalProfile.first())
        assertEquals(
            setOf(DietaryOverlay.LOW_FODMAP, DietaryOverlay.LOW_SODIUM),
            fixture.repository.goalProfileOverlays.first(),
        )
        assertEquals(true, fixture.repository.goalProfileOnboardingCompleted.first())
        assertEquals(true, fixture.repository.goalsCustomised.first())
        assertEquals(20_128L, fixture.repository.weeklyReviewDismissedWeekStartEpochDay.first())
    }

    @Test
    fun unknownStoredValues_areIgnoredSafely() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.dataStore.edit { preferences ->
            preferences[SettingsKeys.GOAL_PROFILE] = "UNKNOWN_PROFILE"
            preferences[SettingsKeys.GOAL_PROFILE_OVERLAYS] = setOf("BAD_OVERLAY", DietaryOverlay.LOW_SODIUM.name)
        }

        assertNull(fixture.repository.goalProfile.first())
        assertEquals(setOf(DietaryOverlay.LOW_SODIUM), fixture.repository.goalProfileOverlays.first())
    }

    @Test
    fun weeklyReviewDismissedWeekStartEpochDay_canBeCleared() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.repository.setWeeklyReviewDismissedWeekStartEpochDay(20_128L)

        fixture.repository.setWeeklyReviewDismissedWeekStartEpochDay(null)

        assertNull(fixture.repository.weeklyReviewDismissedWeekStartEpochDay.first())
    }

    private fun createFixture(scope: CoroutineScope): SettingsRepositoryFixture {
        val storageFile = tempFolder.newFile("settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { storageFile },
        )
        return SettingsRepositoryFixture(
            repository = SettingsRepositoryImpl(dataStore),
            dataStore = dataStore,
        )
    }
}

private data class SettingsRepositoryFixture(
    val repository: SettingsRepositoryImpl,
    val dataStore: DataStore<Preferences>,
)
