package com.openfuel.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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

    @Test
    fun unifiedSearch_onlineButtonUsesDeterministicProviderExecution() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_unified_query_input").performTextInput("oat")
        composeRule.onNodeWithTag("add_food_unified_search_online").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_online_result_sample-oatmeal-1"))
        composeRule.onNodeWithTag("add_food_unified_online_result_sample-oatmeal-1").assertIsDisplayed()

        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_provider_debug"))
        composeRule.onNodeWithTag("add_food_unified_provider_debug_execution_count")
            .assertTextContains("Execution #1")
        composeRule.onNodeWithTag("add_food_unified_provider_debug_toggle")
            .assertTextContains("Show advanced")
        composeRule.onNodeWithTag("add_food_unified_provider_debug_toggle").performClick()
        composeRule.onNodeWithTag("add_food_unified_provider_debug_toggle")
            .assertTextContains("Hide advanced")

        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_refresh_online"))
        composeRule.onNodeWithTag("add_food_unified_refresh_online").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_food_unified_results_list")
            .performScrollToNode(hasTestTag("add_food_unified_provider_debug"))
        composeRule.onNodeWithTag("add_food_unified_provider_debug_execution_count")
            .assertTextContains("Execution #2")
    }

    @Test
    fun quickAddText_prefillsUnifiedSearchQuery() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_button").performClick()
        composeRule.onNodeWithTag("add_food_quick_add_text_input").performTextInput("2 eggs and banana")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_food_quick_add_text_preview_list").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_preview_item_0").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_preview_item_1").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("add_food_quick_add_text_preview_item_0")
                .and(hasText("eggs", substring = true)),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("add_food_quick_add_text_preview_item_1")
                .and(hasText("banana", substring = true)),
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("add_food_quick_add_text_preview_item_0").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_food_unified_query_input").assertTextContains("eggs")
    }

    @Test
    fun quickAddVoice_prefillsTextAndSearchQuery() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_food_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_button").performClick()
        composeRule.onNodeWithTag("add_food_quick_add_voice_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("add_food_quick_add_voice_listening")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_food_quick_add_voice_listening").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_voice_cancel").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("add_food_quick_add_voice_listening")
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_food_quick_add_text_input")
            .assertTextContains("2 eggs and banana")
        composeRule.onNodeWithTag("add_food_quick_add_text_preview_list").assertIsDisplayed()
        composeRule.onNodeWithTag("add_food_quick_add_text_preview_item_0")
            .assertTextContains("eggs", substring = true)
        composeRule.onNodeWithTag("add_food_quick_add_text_preview_item_1")
            .assertTextContains("banana", substring = true)

        composeRule.onNodeWithTag("add_food_quick_add_text_preview_item_0").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_food_unified_query_input").assertTextContains("eggs")
    }
}
