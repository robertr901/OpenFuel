package com.openfuel.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnifiedSearchSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun unifiedSearch_rendersSingleInputAndExplicitOnlineAction() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_unified_query_input").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_unified_search_online").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_unified_scan_barcode").assertIsDisplayed()
    }

    @Test
    fun unifiedSearch_filterChipsControlSectionVisibility() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_unified_query_input").performTextInput("oat")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_food_filter_all").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_local_section"))
        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_online_section"))

        composeRule.onNodeWithTag("add_food_filter_local").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_local_section"))
        val onlineInLocalFilterException = runCatching {
            composeRule.onNodeWithTag("add_food_unified_results_list")
                .performScrollToNode(hasTestTag("add_food_unified_online_section"))
        }.exceptionOrNull()
        assertNotNull(onlineInLocalFilterException)

        composeRule.onNodeWithTag("add_food_filter_online").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_online_section"))
        val localInOnlineFilterException = runCatching {
            composeRule.onNodeWithTag("add_food_unified_results_list")
                .performScrollToNode(hasTestTag("add_food_unified_local_section"))
        }.exceptionOrNull()
        assertNotNull(localInOnlineFilterException)
    }
}
