package com.openfuel.app.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureLoopSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun quickAddCaptureLoop_logsFoodAndReturnsToToday() {
        val foodName = "Smoke Food ${System.currentTimeMillis()}"

        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_manual_toggle").performClick()
        composeRule.onNodeWithTag("add_food_quick_name_input").performTextInput(foodName)
        composeRule.onNodeWithTag("add_food_quick_calories_input").performTextInput("245")
        composeRule.onNodeWithTag("add_food_quick_protein_input").performTextInput("12")
        composeRule.onNodeWithTag("add_food_quick_carbs_input").performTextInput("28")
        composeRule.onNodeWithTag("add_food_quick_fat_input").performTextInput("7")
        composeRule.onNodeWithTag("add_food_quick_log_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithText(foodName).assertIsDisplayed()
    }
}
