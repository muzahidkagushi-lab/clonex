package com.example

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityStartupTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMs: Long = 5000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.mainClock.advanceTimeBy(150L)
            shadowOf(Looper.getMainLooper()).idle()
            if (composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            Thread.sleep(50L)
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test
    fun `user can pass splash onboarding terms and navigate all main tabs`() {
        composeRule.mainClock.autoAdvance = false

        // Wait past splash delay(900) and DataStore load
        waitForTag("onboarding_skip_btn", timeoutMs = 6000L)
        composeRule.onNodeWithTag("onboarding_skip_btn").performClick()

        // Accept Responsible Use terms dialog
        waitForTag("terms_accept_btn", timeoutMs = 4000L)
        composeRule.onNodeWithTag("terms_accept_btn").performClick()

        // Home screen should now be visible
        waitForTag("home_search_input", timeoutMs = 6000L)
        composeRule.onNodeWithTag("home_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("home_quick_clone_fab").assertIsDisplayed()

        // Navigate through Clones, Settings, and About tabs
        composeRule.onNodeWithTag("nav_clones_tab").performClick()
        waitForTag("nav_settings_tab")

        composeRule.onNodeWithTag("nav_settings_tab").performClick()
        waitForTag("setting_language_row")

        composeRule.onNodeWithTag("nav_about_tab").performClick()
        waitForTag("about_tab_0")
    }
}
