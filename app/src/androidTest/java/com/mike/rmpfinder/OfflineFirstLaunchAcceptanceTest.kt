package com.mike.rmpfinder

import android.provider.Settings
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFirstLaunchAcceptanceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshFirstLaunchAndAllBrowseFiltersWorkInAirplaneMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val airplaneMode = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        assumeTrue("Run this acceptance test with airplane mode enabled", airplaneMode == 1)

        waitForText("241 of 241 RMP locations")

        val search = composeRule.onNode(hasSetTextAction())
        search.performTextInput("2370 Grand Concourse")
        waitForText("Dunkin Donuts")
        composeRule.onNodeWithText("Dunkin Donuts").assertIsDisplayed()
        search.performTextClearance()
        waitForText("241 of 241 RMP locations")

        composeRule.onNodeWithText("Bronx").performClick().assertIsSelected()
        waitForText("38 of 241 RMP locations")
        composeRule.onNodeWithText("All").performClick().assertIsSelected()
        waitForText("241 of 241 RMP locations")

        val openNowChip = composeRule.onNode(
            hasText("Open now") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
        )
        openNowChip.performClick().assertIsSelected()
        openNowChip.performClick()

        composeRule.onNodeWithText("Status notes").performClick().assertIsSelected()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("241 of 241 RMP locations").fetchSemanticsNodes().isEmpty()
        }
        check(composeRule.onAllNodesWithText("⚠ Check details before traveling").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Status notes").performClick()

        search.performTextInput("A Daughter and Two Sons")
        waitForText("A Daughter and Two Sons")
        composeRule.onNodeWithText("1807 Archer Street, Bronx, NY 10460", substring = true).performClick()
        composeRule.onNodeWithContentDescription("Favorite").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        search.performTextClearance()
        waitForText("241 of 241 RMP locations")

        composeRule.onNodeWithText("Favorites").performClick().assertIsSelected()
        waitForText("1 of 241 RMP locations")
        composeRule.onNodeWithText("A Daughter and Two Sons").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
