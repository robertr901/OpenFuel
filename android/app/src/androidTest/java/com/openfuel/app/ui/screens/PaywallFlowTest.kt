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
class PaywallFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun insightsLocked_paywallDismissesOnBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.ENTITLEMENT_IS_PRO] = false
            }
        }
        composeRule.activityRule.scenario.recreate()

        try {
            composeRule.onNodeWithTag("tab_insights").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithTag("insights_open_paywall_button")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("insights_open_paywall_button").performClick()
            composeRule.onNodeWithTag("paywall_dialog").assertIsDisplayed()

            composeRule.activityRule.scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.waitForIdle()
            composeRule.onAllNodesWithTag("paywall_dialog").assertCountEquals(0)
        } finally {
            runBlocking {
                context.settingsDataStore.edit { preferences ->
                    preferences[SettingsKeys.ENTITLEMENT_IS_PRO] = false
                }
            }
        }
    }

    @Test
    fun settingsPaywall_restoreUnlocksAdvancedExport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.ENTITLEMENT_IS_PRO] = false
            }
        }
        composeRule.activityRule.scenario.recreate()

        try {
            composeRule.onNodeWithTag("tab_settings").performClick()
            composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
            composeRule.onNodeWithTag("settings_open_paywall_button").performScrollTo().performClick()
            composeRule.onNodeWithTag("paywall_dialog").assertIsDisplayed()
            composeRule.onNodeWithTag("paywall_restore_button").performClick()
            composeRule.waitForIdle()

            composeRule.onAllNodesWithTag("paywall_dialog").assertCountEquals(0)
            composeRule.onNodeWithTag("settings_advanced_export_button").performScrollTo().assertIsDisplayed()
        } finally {
            runBlocking {
                context.settingsDataStore.edit { preferences ->
                    preferences[SettingsKeys.ENTITLEMENT_IS_PRO] = false
                }
            }
        }
    }
}
