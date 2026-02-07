package com.openfuel.app.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openfuel.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_switchesAllTabs_withoutCrash() {
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_history").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_foods").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_foods").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_insights").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_insights").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_today").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
    }
}
