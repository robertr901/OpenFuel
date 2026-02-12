package com.openfuel.app.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyMealSections_collapsedByDefault_andExpandable() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        ensureEmptyMealSlotsToggleVisible()
        composeRule.onAllNodesWithTag("home_empty_meal_sections_content").assertCountEquals(0)

        composeRule
            .onNodeWithTag("home_empty_meal_sections_toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_empty_meal_sections_content").assertIsDisplayed()
    }

    private fun ensureEmptyMealSlotsToggleVisible() {
        repeat(7) {
            val toggleNodes = composeRule
                .onAllNodesWithTag("home_empty_meal_sections_toggle")
                .fetchSemanticsNodes()
            if (toggleNodes.isNotEmpty()) return
            composeRule.onNodeWithContentDescription("Next day").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun primaryAction_fabNavigatesToAddFood() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
    }

    @Test
    fun today_hasSinglePrimaryAddFoodCta_andSecondaryAffordancesAreLinks() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onAllNodesWithTag("home_add_food_fab").assertCountEquals(1)
        composeRule.onAllNodesWithTag("home_primary_log_action").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home_fast_log_reminder_action").assertCountEquals(0)

        val emptyStateNodes = composeRule.onAllNodesWithTag("home_empty_day_state").fetchSemanticsNodes()
        if (emptyStateNodes.isNotEmpty()) {
            composeRule.onNodeWithTag("home_empty_day_add_food_link").assertIsDisplayed()
        }

        val reminderNodes = composeRule.onAllNodesWithTag("home_fast_log_reminder_card").fetchSemanticsNodes()
        if (reminderNodes.isNotEmpty()) {
            composeRule.onNodeWithTag("home_fast_log_reminder_open_link").assertIsDisplayed()
            composeRule
                .onNodeWithText("No meals logged today yet. Add food when you are ready.")
                .assertIsDisplayed()
        }
    }
}
