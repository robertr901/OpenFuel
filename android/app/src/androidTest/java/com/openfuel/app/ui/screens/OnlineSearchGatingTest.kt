package com.openfuel.app.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
class OnlineSearchGatingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onlineSearch_whenDisabled_showsFriendlyMessageAndSkipsLoading() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] = false
            }
        }
        composeRule.activityRule.scenario.recreate()

        try {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("screen_today").assertIsDisplayed()
            composeRule.onNodeWithTag("home_add_food_fab").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("screen_add_food").assertIsDisplayed()
            composeRule.onNodeWithTag("add_food_unified_query_input").performTextInput("coke zero")
            composeRule.onNodeWithTag("add_food_unified_search_online").performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithTag("add_food_unified_online_loading").assertCountEquals(0)
        } finally {
            runBlocking {
                context.settingsDataStore.edit { preferences ->
                    preferences[SettingsKeys.ONLINE_LOOKUP_ENABLED] = true
                }
            }
        }
    }
}
