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
            val cachedPrice = CachedPrice("2.5", 1000L)
            coEvery { repository.getCachedPrice() } returns cachedPrice
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Success(CachedPrice("3.0", 2000L))
            coEvery { repository.fetchCurrentHourAverage() } returns FetchResult.Success(CachedPrice("3.2", 2000L))

            val viewModel = PriceViewModel(repository)

            // Before refresh finishes, state should have cached data
            advanceUntilIdle() // This will process init block coroutines

            val state = viewModel.uiState.value
            assertEquals("3.0", state.priceText)
            assertEquals("3.2", state.hourlyAvgPriceText)
            assertEquals(PriceTier.NORMAL, state.priceTier)
            assertEquals(2000L, state.updatedAtMillis)
            assertFalse(state.isRefreshing)
        }

    @Test
    fun `refresh emits isRefreshing true then false`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Success(CachedPrice("3.0", 2000L))
            coEvery { repository.fetchCurrentHourAverage() } returns FetchResult.Success(CachedPrice("3.2", 2000L))

            val viewModel = PriceViewModel(repository)

            viewModel.uiState.test {
                // Initial default state emitted by MutableStateFlow construction
                assertFalse(awaitItem().isRefreshing)
                // init block sets isRefreshing = true before any fetch completes
                assertTrue(awaitItem().isRefreshing)
                // Fetches complete — isRefreshing flips back to false
                assertFalse(awaitItem().isRefreshing)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh shows error when five minute fetch fails`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchFiveMinutePrice() } returns
                FetchResult.Error("Network Error", CachedPrice("1.0", 1000L))
            coEvery { repository.fetchCurrentHourAverage() } returns FetchResult.Success(CachedPrice("1.5", 2000L))

            val viewModel = PriceViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals("1.0", state.priceText) // Fallback price
            assertEquals("1.5", state.hourlyAvgPriceText)
            assertEquals("Network Error", state.errorMessage)
        }

    @Test
    fun `refresh uses previous state when error has no fallback`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns CachedPrice("4.0", 500L)
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Success(CachedPrice("4.0", 500L))
            coEvery { repository.fetchCurrentHourAverage() } returns FetchResult.Success(CachedPrice("4.0", 500L))
            val viewModel = PriceViewModel(repository)
            advanceUntilIdle() // Initializes with 4.0

            // Now fail refresh
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Error("Timeout", null)
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
            coEvery { repository.fetchFiveMinutePrice() } coAnswers {
                // Suspend until the test coroutine advances
                kotlinx.coroutines.awaitCancellation()
            }
            coEvery { repository.fetchCurrentHourAverage() } coAnswers {
                kotlinx.coroutines.awaitCancellation()
            }

            val viewModel = PriceViewModel(repository)
            // Advance past the init cache read but not past the suspended fetch calls —
            // the viewModel is now mid-refresh with isRefreshing = true.
            testScheduler.advanceTimeBy(1)

            assertTrue(viewModel.uiState.value.isRefreshing)

            // A second refresh() call while already refreshing must be a no-op.
            viewModel.refresh()

            // fetchFiveMinutePrice should only have been called once (from init's refresh).
            coVerify(exactly = 1) { repository.fetchFiveMinutePrice() }
        }

    @Test
    fun `refresh shows error when only hourly fetch fails`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Success(CachedPrice("5.0", 4000L))
            coEvery { repository.fetchCurrentHourAverage() } returns
                FetchResult.Error("Hourly fetch failed", CachedPrice("4.5", 3000L))

            val viewModel = PriceViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            // Five-minute price succeeded — it should be reflected
            assertEquals("5.0", state.priceText)
            // Hourly failed — fallback from the error's cachedFallback
            assertEquals("4.5", state.hourlyAvgPriceText)
            assertEquals("Hourly fetch failed", state.errorMessage)
        }

    @Test
    fun `refresh shows error when both fetches fail`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns CachedPrice("6.0", 500L)
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Success(CachedPrice("6.0", 500L))
            coEvery { repository.fetchCurrentHourAverage() } returns FetchResult.Success(CachedPrice("6.0", 500L))
            val viewModel = PriceViewModel(repository)
            advanceUntilIdle()

            // Now make both fail
            coEvery { repository.fetchFiveMinutePrice() } returns
                FetchResult.Error("5-min timeout", CachedPrice("6.0", 500L))
            coEvery { repository.fetchCurrentHourAverage() } returns
                FetchResult.Error("Hourly timeout", CachedPrice("6.0", 500L))
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            // Five-minute error message takes precedence in the when{} expression
            assertEquals("5-min timeout", state.errorMessage)
            // Prices fall back to cachedFallback values from each error
            assertEquals("6.0", state.priceText)
            assertEquals("6.0", state.hourlyAvgPriceText)
        }
}
