package com.openfuel.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import com.openfuel.app.data.datastore.SettingsKeys
import com.openfuel.app.data.datastore.settingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalProfileOnboardingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingShownOnce_thenHiddenAfterSkip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.GOAL_PROFILE_ONBOARDING_COMPLETED] = false
                preferences.remove(SettingsKeys.GOAL_PROFILE)
                preferences.remove(SettingsKeys.GOAL_PROFILE_OVERLAYS)
                preferences[SettingsKeys.GOALS_CUSTOMISED] = false
            }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("goal_profile_onboarding_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("goal_profile_skip_button").performClick()
        composeRule.waitForIdle()
        assertOnboardingDialogHidden()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        assertOnboardingDialogHidden()
    }

    @Test
    fun profileCanBeChangedInSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.GOAL_PROFILE_ONBOARDING_COMPLETED] = true
                preferences[SettingsKeys.GOAL_PROFILE] = "FAT_LOSS"
                preferences[SettingsKeys.GOAL_PROFILE_OVERLAYS] = emptySet()
            }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_edit_goal_profile_button").performScrollTo()
        composeRule.onNodeWithTag("settings_edit_goal_profile_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_goal_profile_save_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_goal_profile_option_muscle_gain").performClick()
        composeRule.onNodeWithTag("settings_goal_profile_save_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_goal_profile_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_today").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_goal_profile_summary").assertIsDisplayed()
    }

    private fun assertOnboardingDialogHidden() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("goal_profile_onboarding_dialog")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.onAllNodesWithTag("goal_profile_onboarding_dialog").assertCountEquals(0)
    }
}
