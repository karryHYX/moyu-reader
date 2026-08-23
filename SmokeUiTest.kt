package com.moyu.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SmokeUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun onboardingOrLibraryIsUsable() {
        val library = composeRule.onNodeWithText("我的书架")
        val continueButton = composeRule.onNodeWithText("继续")
        if (runCatching { library.assertIsDisplayed(); true }.getOrDefault(false)) return
        repeat(2) { continueButton.performClick() }
        composeRule.onNodeWithText("进入书架").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("我的书架").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("我的书架").assertIsDisplayed()
    }
}
