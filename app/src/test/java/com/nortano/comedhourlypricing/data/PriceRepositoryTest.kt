package com.nortano.comedhourlypricing.data

import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.ApiService
import com.nortano.comedhourlypricing.data.remote.CurrentAvgDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceRepositoryTest {
    private val apiService = mockk<ApiService>()
    private val cacheStore = mockk<PriceCacheStore>(relaxed = true)

    private val repository = PriceRepository(apiService, cacheStore)

    @Test
    fun `fetchCurrentHourAverage delegates to fetchPricesCombined and returns both prices`() =
        runTest {
            val fiveMinDto = CurrentAvgDto(price = "2.5", millisUtc = "1672531200000")
            val hourlyDto = CurrentAvgDto(price = "2.3", millisUtc = "1672531200000")
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Success)
            val successResult = result as FetchResult.Success
            assertEquals("2.5", successResult.data.fiveMinPrice)
            assertEquals("2.3", successResult.data.hourlyAvgPrice)
            assertEquals(1672531200000L, successResult.data.timestampMillisUtc)

            coVerify { cacheStore.save(any()) }
        }

    @Test
    fun `fetchPricesCombined returns success and saves both prices to cache`() =
        runTest {
            val fiveMinDto = CurrentAvgDto(price = "3.1", millisUtc = "1672531300000")
            val hourlyDto = CurrentAvgDto(price = "2.8", millisUtc = "1672531200000")
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)

            val result = repository.fetchPricesCombined()

            assertTrue(result is FetchResult.Success)
            val successResult = result as FetchResult.Success
            assertEquals("3.1", successResult.data.price)
            assertEquals("3.1", successResult.data.fiveMinPrice)
            assertEquals("2.8", successResult.data.hourlyAvgPrice)
            assertEquals(1672531300000L, successResult.data.timestampMillisUtc)

            coVerify { cacheStore.save(any()) }
        }

    @Test
    fun `fetchPricesCombined returns error when one fetch fails`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } returns listOf(CurrentAvgDto(price = "3.1"))
            coEvery { apiService.getCurrentAvg() } returns emptyList()
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchPricesCombined()

            assertTrue(result is FetchResult.Error)
            assertEquals("Missing price data", (result as FetchResult.Error).message)
        }

    @Test
    fun `fetchCurrentHourAverage returns error with fallback when api throws exception`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } throws RuntimeException("Network error")
            coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = "2.3"))
            val fallbackPrice = CachedPrice("1.5", 1000L, hourlyAvgPrice = "1.4", fiveMinPrice = "1.5")
            coEvery { cacheStore.getCachedPrice() } returns fallbackPrice

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Error)
            val errorResult = result as FetchResult.Error
            assertEquals("Network error", errorResult.message)
            assertEquals(fallbackPrice, errorResult.cachedFallback)
        }

    @Test
    fun `fetchCurrentHourAverage returns error when response list is empty`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } returns listOf(CurrentAvgDto(price = "3.1"))
            coEvery { apiService.getCurrentAvg() } returns emptyList()
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Error)
            assertEquals("Missing price data", (result as FetchResult.Error).message)
        }

    @Test
    fun `fetchCurrentHourAverage returns error when price is null`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } returns listOf(CurrentAvgDto(price = null, millisUtc = "100"))
            coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = "2.3", millisUtc = "100"))
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Error)
            assertEquals("Missing price data", (result as FetchResult.Error).message)
        }

    @Test
    fun `fetchCurrentHourAverage returns error when price is blank`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } returns listOf(CurrentAvgDto(price = "   ", millisUtc = "100"))
            coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = "2.3", millisUtc = "100"))
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Error)
            assertEquals("Missing price data", (result as FetchResult.Error).message)
        }

    // --- fetchFiveMinutePrice Tests ---

    @Test
    fun `fetchFiveMinutePrice delegates to fetchPricesCombined and returns both prices`() =
        runTest {
            val fiveMinDto = CurrentAvgDto(price = "3.1", millisUtc = "1672531300000")
            val hourlyDto = CurrentAvgDto(price = "2.8", millisUtc = "1672531300000")
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)

            val result = repository.fetchFiveMinutePrice()

            assertTrue(result is FetchResult.Success)
            val successResult = result as FetchResult.Success
            assertEquals("3.1", successResult.data.fiveMinPrice)
            assertEquals("2.8", successResult.data.hourlyAvgPrice)
            assertEquals(1672531300000L, successResult.data.timestampMillisUtc)

            coVerify { cacheStore.save(any()) }
        }

    @Test
    fun `fetchFiveMinutePrice returns error with fallback when api throws exception`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } throws RuntimeException("Timeout error")
            coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = "2.0"))
            val fallbackPrice = CachedPrice("2.0", 2000L, hourlyAvgPrice = "1.9", fiveMinPrice = "2.0")
            coEvery { cacheStore.getCachedPrice() } returns fallbackPrice

            val result = repository.fetchFiveMinutePrice()

            assertTrue(result is FetchResult.Error)
            val errorResult = result as FetchResult.Error
            assertEquals("Timeout error", errorResult.message)
            assertEquals(fallbackPrice, errorResult.cachedFallback)
        }

    // --- Cache deduplication tests ---

    @Test
    fun `fetchCurrentHourAverage does not save to cache when all fields match`() =
        runTest {
            val existingTimestamp = 1672531200000L
            val fiveMinDto = CurrentAvgDto(price = "2.5", millisUtc = existingTimestamp.toString())
            val hourlyDto = CurrentAvgDto(price = "2.3", millisUtc = existingTimestamp.toString())
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            coEvery { cacheStore.getCachedPrice() } returns
                CachedPrice(
                    price = "2.5",
                    timestampMillisUtc = existingTimestamp,
                    hourlyAvgPrice = "2.3",
                    fiveMinPrice = "2.5",
                )

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Success)
            coVerify(exactly = 0) { cacheStore.save(any()) }
        }

    @Test
    fun `fetchCurrentHourAverage saves to cache when timestamp changes`() =
        runTest {
            val newTimestamp = 1672534800000L
            val fiveMinDto = CurrentAvgDto(price = "3.0", millisUtc = newTimestamp.toString())
            val hourlyDto = CurrentAvgDto(price = "2.8", millisUtc = newTimestamp.toString())
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            coEvery { cacheStore.getCachedPrice() } returns
                CachedPrice(
                    price = "2.5",
                    timestampMillisUtc = 1672531200000L,
                    hourlyAvgPrice = "2.3",
                    fiveMinPrice = "2.5",
                )

            val result = repository.fetchCurrentHourAverage()

            assertTrue(result is FetchResult.Success)
            coVerify(exactly = 1) { cacheStore.save(any()) }
        }

    @Test
    fun `fetchCurrentHourAverage uses current time when millisUtc is null`() =
        runTest {
            val beforeCall = System.currentTimeMillis()
            val fiveMinDto = CurrentAvgDto(price = "4.0", millisUtc = null)
            val hourlyDto = CurrentAvgDto(price = "3.8", millisUtc = null)
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchCurrentHourAverage()
            val afterCall = System.currentTimeMillis()

            assertTrue(result is FetchResult.Success)
            val timestamp = (result as FetchResult.Success).data.timestampMillisUtc
            assertTrue(
                "Timestamp should be close to System.currentTimeMillis()",
                timestamp in beforeCall..afterCall,
            )
        }

    @Test
    fun `fetchCurrentHourAverage uses current time when millisUtc is non-numeric`() =
        runTest {
            val beforeCall = System.currentTimeMillis()
            val fiveMinDto = CurrentAvgDto(price = "4.0", millisUtc = "not-a-long")
            val hourlyDto = CurrentAvgDto(price = "3.8", millisUtc = "not-a-long")
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchCurrentHourAverage()
            val afterCall = System.currentTimeMillis()

            assertTrue(result is FetchResult.Success)
            val timestamp = (result as FetchResult.Success).data.timestampMillisUtc
            assertTrue(
                "Timestamp should be close to System.currentTimeMillis()",
                timestamp in beforeCall..afterCall,
            )
        }

    // --- fetchPricesCombined deduplication and edge-case tests ---

    @Test
    fun `fetchPricesCombined does not save to cache when all fields match`() =
        runTest {
            val timestamp = 1672531200000L
            val fiveMinDto = CurrentAvgDto(price = "3.1", millisUtc = timestamp.toString())
            val hourlyDto = CurrentAvgDto(price = "2.8", millisUtc = timestamp.toString())
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            // Cache already contains the exact same data
            coEvery { cacheStore.getCachedPrice() } returns
                CachedPrice(
                    price = "3.1",
                    timestampMillisUtc = timestamp,
                    hourlyAvgPrice = "2.8",
                    fiveMinPrice = "3.1",
                )

            val result = repository.fetchPricesCombined()

            assertTrue(result is FetchResult.Success)
            coVerify(exactly = 0) { cacheStore.save(any()) }
        }

    @Test
    fun `fetchPricesCombined saves to cache when timestamp matches but a price field changed`() =
        runTest {
            val timestamp = 1672531200000L
            val fiveMinDto = CurrentAvgDto(price = "3.5", millisUtc = timestamp.toString())
            val hourlyDto = CurrentAvgDto(price = "2.8", millisUtc = timestamp.toString())
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            // Cache has the same timestamp but stale fiveMinPrice
            coEvery { cacheStore.getCachedPrice() } returns
                CachedPrice(
                    price = "3.1",
                    timestampMillisUtc = timestamp,
                    hourlyAvgPrice = "2.8",
                    fiveMinPrice = "3.1",
                )

            val result = repository.fetchPricesCombined()

            assertTrue(result is FetchResult.Success)
            coVerify(exactly = 1) { cacheStore.save(any()) }
        }

    @Test
    fun `fetchPricesCombined uses current time when millisUtc is null`() =
        runTest {
            val beforeCall = System.currentTimeMillis()
            val fiveMinDto = CurrentAvgDto(price = "3.1", millisUtc = null)
            val hourlyDto = CurrentAvgDto(price = "2.8", millisUtc = null)
            coEvery { apiService.getFiveMinutePrice() } returns listOf(fiveMinDto)
            coEvery { apiService.getCurrentAvg() } returns listOf(hourlyDto)
            coEvery { cacheStore.getCachedPrice() } returns null

            val result = repository.fetchPricesCombined()
            val afterCall = System.currentTimeMillis()

            assertTrue(result is FetchResult.Success)
            val timestamp = (result as FetchResult.Success).data.timestampMillisUtc
            assertTrue(
                "Timestamp should be close to System.currentTimeMillis()",
                timestamp in beforeCall..afterCall,
            )
        }

    @Test
    fun `fetchPricesCombined returns error with fallback when api throws exception`() =
        runTest {
            coEvery { apiService.getFiveMinutePrice() } throws RuntimeException("Timeout")
            coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = "2.8"))
            val fallback = CachedPrice("2.0", 1000L)
            coEvery { cacheStore.getCachedPrice() } returns fallback

            val result = repository.fetchPricesCombined()

            assertTrue(result is FetchResult.Error)
            val errorResult = result as FetchResult.Error
            assertEquals("Timeout", errorResult.message)
            assertEquals(fallback, errorResult.cachedFallback)
        }
}
