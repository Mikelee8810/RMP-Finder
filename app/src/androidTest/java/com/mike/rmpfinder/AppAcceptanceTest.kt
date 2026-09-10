package com.mike.rmpfinder

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppAcceptanceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun deniedLocationStillShowsBundledBrowseAndMapList() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotEquals(
            PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION),
        )
        waitForDirectory()
        composeRule.onNodeWithText("241 of 241 RMP locations").assertIsDisplayed()
        composeRule.onNodeWithText("Map").performClick()
        composeRule.onNodeWithText("Nearby list · Bronx-first").assertIsDisplayed()
        composeRule.onNodeWithText("The restaurant list still works offline.").assertIsDisplayed()
        composeRule.onNodeWithText("1807 Archer Street, Bronx, NY 10460", substring = true).performClick()
        composeRule.onNodeWithText("Official RMP record").assertIsDisplayed()
    }

    @Test
    fun knownConflictShowsOfficialAndCurrentAddressesSeparately() {
        waitForDirectory()
        composeRule.onNode(hasSetTextAction()).performTextInput("2370 Grand Concourse")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Dunkin Donuts").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Dunkin Donuts").performClick()
        composeRule.onNodeWithText("Official RMP record").assertIsDisplayed()
        composeRule.onNodeWithText("2370 Grand Concourse Road, Bronx, NY 10458").assertIsDisplayed()
        composeRule.onNodeWithText("Current business information").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2366 Grand Concourse, Bronx, NY 10458").assertIsDisplayed()
        composeRule.onNodeWithText("Official RMP address").assertIsDisplayed()
        composeRule.onNodeWithText("Current business address").assertIsDisplayed()
        composeRule.onAllNodesWithText("Transit", substring = true).assertCountEquals(2)
        composeRule.onAllNodesWithText("Walk", substring = true).assertCountEquals(2)
    }

    @Test
    fun missingWebsiteAndMenuDoNotRenderActions() {
        waitForDirectory()
        composeRule.onNode(hasSetTextAction()).performTextInput("A Daughter and Two Sons")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("A Daughter and Two Sons").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("1807 Archer Street, Bronx, NY 10460", substring = true).performClick()
        composeRule.onNodeWithText("Actions").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Website").assertCountEquals(0)
        composeRule.onAllNodesWithText("Menu").assertCountEquals(0)
    }

    @Test
    fun missingPhoneDoesNotRenderCallAction() {
        waitForDirectory()
        composeRule.onNode(hasSetTextAction()).performTextInput("162-02 Jamaica Avenue")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Jamaican Flavors").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Jamaican Flavors").performClick()
        composeRule.onNodeWithText("Actions").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Call").assertCountEquals(0)
        composeRule.onAllNodesWithText("Menu").assertCountEquals(0)
        composeRule.onNodeWithText("Website", substring = true).assertIsDisplayed()
    }

    private fun waitForDirectory() {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("241 of 241 RMP locations").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
