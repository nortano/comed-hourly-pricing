package com.nortano.comedhourlypricing.ui

import com.nortano.comedhourlypricing.data.CachedPrice
import com.nortano.comedhourlypricing.data.FetchResult
import com.nortano.comedhourlypricing.data.PriceRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
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
    fun `refresh updates state successfully`() =
        runTest {
            coEvery { repository.getCachedPrice() } returns null
            coEvery { repository.fetchFiveMinutePrice() } returns FetchResult.Success(CachedPrice("10.0", 3000L))
            coEvery { repository.fetchCurrentHourAverage() } returns FetchResult.Success(CachedPrice("8.5", 3000L))

            val viewModel = PriceViewModel(repository)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals("10.0", state.priceText)
            assertEquals("8.5", state.hourlyAvgPriceText)
            assertEquals(PriceTier.HIGH, state.priceTier) // 10.0 is HIGH tier
            assertEquals(3000L, state.updatedAtMillis)
            assertNull(state.errorMessage)
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
}
