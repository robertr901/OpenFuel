package com.openfuel.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodsFlowSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun foodsScreen_showsRecentAndFavoritesWhenQueryBlank() {
        seedFoodViaQuickAdd("Foods recent seed ${System.currentTimeMillis()}")

        composeRule.onNodeWithTag("tab_foods").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_foods").assertIsDisplayed()
        composeRule.onNodeWithTag("foods_results_list")
            .performScrollToNode(hasTestTag("foods_recent_section"))
        composeRule.onAllNodesWithTag("foods_recent_section").assertCountEquals(1)
        composeRule.onNodeWithTag("foods_results_list")
            .performScrollToNode(hasTestTag("foods_favorites_section"))
        composeRule.onAllNodesWithTag("foods_favorites_section").assertCountEquals(1)
    }

    @Test
    fun foodsRow_portionActionOpensFoodDetail() {
        val foodName = "Foods portion seed ${System.currentTimeMillis()}"
        seedFoodViaQuickAdd(foodName)

        composeRule.onNodeWithTag("tab_foods").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("foods_query_input").performTextInput(foodName)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag("foods_local_0_portion").assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithTag("foods_local_0_portion").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Food detail").assertIsDisplayed()
    }

    @Test
    fun foodsRow_logAction_keepsFlowDeterministic() {
        val foodName = "Foods log seed ${System.currentTimeMillis()}"
        seedFoodViaQuickAdd(foodName)

        composeRule.onNodeWithTag("tab_foods").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("foods_query_input").performTextInput(foodName)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag("foods_local_0_log").assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithTag("foods_local_0_log").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Food logged.").assertIsDisplayed()
        composeRule.onNodeWithTag("screen_foods").assertIsDisplayed()
    }

    private fun seedFoodViaQuickAdd(foodName: String) {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_button").performClick()
        composeRule.onNodeWithTag("add_food_quick_manual_toggle").performClick()
        composeRule.onNodeWithTag("add_food_quick_name_input").performTextInput(foodName)
        composeRule.onNodeWithTag("add_food_quick_calories_input").performTextInput("210")
        composeRule.onNodeWithTag("add_food_quick_protein_input").performTextInput("12")
        composeRule.onNodeWithTag("add_food_quick_carbs_input").performTextInput("25")
        composeRule.onNodeWithTag("add_food_quick_fat_input").performTextInput("8")
        composeRule.onNodeWithTag("add_food_quick_log_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        composeRule.waitForIdle()
    }
}
