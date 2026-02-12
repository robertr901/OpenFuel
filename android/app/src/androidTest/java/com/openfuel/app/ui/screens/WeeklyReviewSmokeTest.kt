package com.openfuel.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import com.openfuel.app.OpenFuelApp
import com.openfuel.app.data.datastore.SettingsKeys
import com.openfuel.app.data.datastore.settingsDataStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeeklyReviewSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun weeklyReviewEntry_fromToday_opensReviewScreen_whenEligible() {
        resetWeeklyReviewState()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        logOneFoodForToday()
        assertTrue("Expected weekly review entry on a nearby day", ensureWeeklyReviewEntryVisible())

        composeRule.onNodeWithTag("home_weekly_review_entry_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("home_open_weekly_review_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_weekly_review").assertIsDisplayed()
    }

    @Test
    fun weeklyReviewEntry_fromInsights_opensReviewScreen_whenEligible() {
        resetWeeklyReviewState()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        logOneFoodForToday()
        assertTrue("Expected weekly review entry on a nearby day", ensureWeeklyReviewEntryVisible())
        composeRule.onNodeWithTag("tab_insights").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insights_weekly_review_entry_card").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_open_weekly_review_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_weekly_review").assertIsDisplayed()
    }

    private fun resetWeeklyReviewState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<OpenFuelApp>()
        runBlocking {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.GOAL_PROFILE_ONBOARDING_COMPLETED] = true
                preferences.remove(SettingsKeys.WEEKLY_REVIEW_DISMISSED_WEEK_START_EPOCH_DAY)
            }
            val today = LocalDate.now()
            val recentEntries = app.container.logRepository.entriesInRange(
                startDate = today.minusDays(6),
                endDateInclusive = today,
                zoneId = ZoneId.systemDefault(),
            ).first()
            recentEntries.forEach { mealEntryWithFood ->
                app.container.logRepository.deleteMealEntry(mealEntryWithFood.entry.id)
            }
        }
    }

    private fun logOneFoodForToday() {
        val foodName = "Weekly Review Food ${System.currentTimeMillis()}"

        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_button").performClick()
        composeRule.onNodeWithTag("add_food_quick_manual_toggle").performClick()
        composeRule.onNodeWithTag("add_food_quick_name_input").performTextInput(foodName)
        composeRule.onNodeWithTag("add_food_quick_calories_input").performTextInput("220")
        composeRule.onNodeWithTag("add_food_quick_protein_input").performTextInput("14")
        composeRule.onNodeWithTag("add_food_quick_carbs_input").performTextInput("24")
        composeRule.onNodeWithTag("add_food_quick_fat_input").performTextInput("6")
        composeRule.onNodeWithTag("add_food_quick_log_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        composeRule.waitForIdle()
    }

    private fun ensureWeeklyReviewEntryVisible(): Boolean {
        repeat(31) { attempt ->
            val nodes = composeRule.onAllNodesWithTag("home_weekly_review_entry_card")
                .fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                return true
            }
            if (attempt < 15) {
                composeRule.onNodeWithContentDescription("Previous day").performClick()
            } else {
                composeRule.onNodeWithContentDescription("Next day").performClick()
            }
            composeRule.waitForIdle()
        }
        return false
    }
}
