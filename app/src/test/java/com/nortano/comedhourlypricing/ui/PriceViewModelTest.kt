package com.nortano.comedhourlypricing.ui

import app.cash.turbine.test
import com.nortano.comedhourlypricing.data.CachedPrice
import com.nortano.comedhourlypricing.data.FetchResult
import com.nortano.comedhourlypricing.data.PriceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PriceViewModelTest {
    private val repository = mockk<PriceRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads cached price and then refreshes`() =
        runTest {
            val cachedPrice = CachedPrice("2.5", 1000L, hourlyAvgPrice = "2.4", fiveMinPrice = "2.5")
            coEvery { repository.getCachedPrice() } returns cachedPrice
            coEvery { repository.fetchPricesCombined() } returns
                FetchResult.Success(
                    CachedPrice("3.0", 2000L, hourlyAvgPrice = "2.8", fiveMinPrice = "3.0"),
                )

            val viewModel = PriceViewModel(repository)

            // Before refresh finishes, state should have cached data
            advanceUntilIdle() // This will process init block coroutines

            val state = viewModel.uiState.value
            assertEquals("3.0", state.priceText)
            assertEquals("2.8", state.hourlyAvgPriceText)
            assertEquals(PriceTier.NORMAL, state.priceTier)
            assertEquals(PriceTrend.UP, state.priceTrend)
            assertEquals(2000L, state.updatedAtMillis)
            assertFalse(state.isRefreshing)
        }

    @Test
    fun `refresh emits isRefreshing true then false`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchPricesCombined() } returns
                FetchResult.Success(
                    CachedPrice("3.0", 2000L, hourlyAvgPrice = "3.2", fiveMinPrice = "3.0"),
                )

            val viewModel = PriceViewModel(repository)

            viewModel.uiState.test {
                // Initial default state
                assertFalse(awaitItem().isRefreshing)
                // init block sets isRefreshing = true
                assertTrue(awaitItem().isRefreshing)
                // Fetches complete
                assertFalse(awaitItem().isRefreshing)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh shows error when fetch fails`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchPricesCombined() } returns
                FetchResult.Error(
                    "Network Error",
                    CachedPrice("1.0", 1000L, hourlyAvgPrice = "1.5", fiveMinPrice = "1.0"),
                )

            val viewModel = PriceViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals("1.0", state.priceText) // Fallback price
            assertEquals("1.5", state.hourlyAvgPriceText)
            assertEquals("Network Error", state.errorMessage)
            assertEquals(PriceTrend.DOWN, state.priceTrend)
        }

    @Test
    fun `refresh uses previous state when error has no fallback`() =
        runTest {
            val cached = CachedPrice("4.0", 500L, hourlyAvgPrice = "4.0", fiveMinPrice = "4.0")
            coEvery { repository.getCachedPrice() } returns cached
            coEvery { repository.fetchPricesCombined() } returns FetchResult.Success(cached)
            val viewModel = PriceViewModel(repository)
            advanceUntilIdle() // Initializes with 4.0

            // Now fail refresh
            coEvery { repository.fetchPricesCombined() } returns FetchResult.Error("Timeout", null)
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals("4.0", state.priceText) // Keeps old price
            assertEquals(PriceTier.NORMAL, state.priceTier)
            assertEquals("Timeout", state.errorMessage)
        }

    @Test
    fun `refresh is a no-op when already refreshing`() =
        runTest {
            // Use a never-completing stub so isRefreshing stays true indefinitely
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchPricesCombined() } coAnswers {
                // Suspend until the test coroutine advances
                kotlinx.coroutines.awaitCancellation()
            }

            val viewModel = PriceViewModel(repository)
            // Advance past the init cache read but not past the suspended fetch calls
            testScheduler.advanceTimeBy(1)

            assertTrue(viewModel.uiState.value.isRefreshing)

            // A second refresh() call while already refreshing must be a no-op.
            viewModel.refresh()

            // fetchPricesCombined should only have been called once (from init's refresh).
            coVerify(exactly = 1) { repository.fetchPricesCombined() }
        }

    @Test
    fun `refresh success with stagnant trend when fiveMin equals hourly`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchPricesCombined() } returns
                FetchResult.Success(
                    CachedPrice("2.5", 1000L, hourlyAvgPrice = "2.5", fiveMinPrice = "2.5"),
                )

            val viewModel = PriceViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals("2.5", state.priceText)
            assertEquals(PriceTrend.STAGNANT, state.priceTrend)
            assertEquals(PriceTier.NORMAL, state.priceTier)
        }

    @Test
    fun `refresh error uses prior priceText as fiveMin when fallback fiveMinPrice is null`() =
        runTest {
            // Pre-load state with a known price of "4.0"
            val initial = CachedPrice("4.0", 500L, hourlyAvgPrice = "4.0", fiveMinPrice = "4.0")
            coEvery { repository.getCachedPrice() } returns initial
            coEvery { repository.fetchPricesCombined() } returns FetchResult.Success(initial)
            val viewModel = PriceViewModel(repository)
            advanceUntilIdle() // state: priceText = "4.0"

            // Fail with a fallback that has a different price but no fiveMinPrice.
            // The ViewModel falls back to it.priceText ("4.0") for fiveMin.
            // fallback.price ("6.0") is used as priceText; hourly is "3.0".
            // PriceTrend.calculate("4.0", "3.0") → UP.
            val fallback = CachedPrice("6.0", 600L, hourlyAvgPrice = "3.0", fiveMinPrice = null)
            coEvery { repository.fetchPricesCombined() } returns FetchResult.Error("Server error", fallback)
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals("6.0", state.priceText)
            assertEquals("3.0", state.hourlyAvgPriceText)
            assertEquals("Server error", state.errorMessage)
            // fiveMin="4.0" (prior priceText), hourly="3.0" → UP
            assertEquals(PriceTrend.UP, state.priceTrend)
        }
}
