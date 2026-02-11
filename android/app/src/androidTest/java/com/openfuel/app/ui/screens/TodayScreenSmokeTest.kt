package com.openfuel.app.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
        composeRule.onAllNodesWithTag("home_empty_meal_sections_content").assertCountEquals(0)

        composeRule.onNodeWithTag("home_empty_meal_sections_toggle").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_empty_meal_sections_content").assertIsDisplayed()
    }

    @Test
    fun primaryAction_fabNavigatesToAddFood() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
    }
}
