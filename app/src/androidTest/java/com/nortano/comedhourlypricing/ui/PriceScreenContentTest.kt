package com.nortano.comedhourlypricing.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.wear.compose.material.MaterialTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PriceScreenContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun successState_displaysPrices() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        // Verify prices
        composeTestRule.onNodeWithText("3.5 ¢").assertIsDisplayed()
        // Wait, hourly label formatting: Hourly: 3.6¢
        // For testing we check substring or exact text
        composeTestRule.onNodeWithText("Hourly: 3.6¢", substring = true).assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorLabel() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = "Some error",
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun loadingState_refreshButtonIsDisabled() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = true,
                errorMessage = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        // clickable(enabled = false) removes the OnClick semantic action entirely
        composeTestRule
            .onNodeWithContentDescription("Refresh")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun nullPriceText_showsEmptyPlaceholder() {
        val state =
            PriceUiState(
                priceText = null,
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.UNKNOWN,
                isRefreshing = false,
                errorMessage = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        // priceText = null → displayPrice = "--" → rendered as "-- ¢"
        composeTestRule.onNodeWithText("-- ¢").assertIsDisplayed()
    }

    @Test
    fun nullHourlyAvgPriceText_showsEmptyPlaceholder() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = null,
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        // hourlyAvgPriceText = null → hourlyPrice = "--" → rendered as "Hourly: --¢"
        composeTestRule.onNodeWithText("Hourly: --¢", substring = true).assertIsDisplayed()
    }

    @Test
    fun nullUpdatedAtMillis_showsNoData() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = null,
                updatedAtMillis = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        composeTestRule.onNodeWithText("No data").assertIsDisplayed()
    }

    @Test
    fun recentUpdatedAtMillis_showsUpdatedJustNow() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = null,
                updatedAtMillis = System.currentTimeMillis() - 10_000L, // 10 seconds ago
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        composeTestRule.onNodeWithText("Updated just now").assertIsDisplayed()
    }

    @Test
    fun blankErrorMessage_doesNotShowErrorLabel() {
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = "   ", // blank, not null
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = {})
            }
        }

        // isNullOrBlank() is true for "   " — the Error label must not appear
        composeTestRule.onNodeWithText("Error").assertDoesNotExist()
    }

    @Test
    fun refreshButton_invokesOnRefreshCallback() {
        var callbackInvoked = false
        val state =
            PriceUiState(
                priceText = "3.5",
                hourlyAvgPriceText = "3.6",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                errorMessage = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                PriceScreenContent(state = state, onRefresh = { callbackInvoked = true })
            }
        }

        composeTestRule.onNodeWithContentDescription("Refresh").performClick()

        assertTrue("onRefresh callback should have been invoked", callbackInvoked)
    }
}
