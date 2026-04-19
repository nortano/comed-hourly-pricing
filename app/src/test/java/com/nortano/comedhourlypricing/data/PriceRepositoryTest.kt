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
    fun `fetchCurrentHourAverage returns success and saves to cache when data is valid`() = runTest {
        val dto = CurrentAvgDto(price = "2.5", millisUtc = "1672531200000")
        coEvery { apiService.getCurrentAvg() } returns listOf(dto)
        
        val result = repository.fetchCurrentHourAverage()
        
        assertTrue(result is FetchResult.Success)
        val successResult = result as FetchResult.Success
        assertEquals("2.5", successResult.data.price)
        assertEquals(1672531200000L, successResult.data.timestampMillisUtc)
        
        coVerify { cacheStore.save(any()) }
    }

    @Test
    fun `fetchCurrentHourAverage returns error with fallback when api throws exception`() = runTest {
        coEvery { apiService.getCurrentAvg() } throws RuntimeException("Network error")
        val fallbackPrice = CachedPrice("1.5", 1000L)
        coEvery { cacheStore.getCachedPrice() } returns fallbackPrice
        
        val result = repository.fetchCurrentHourAverage()
        
        assertTrue(result is FetchResult.Error)
        val errorResult = result as FetchResult.Error
        assertEquals("Network error", errorResult.message)
        assertEquals(fallbackPrice, errorResult.cachedFallback)
    }

    @Test
    fun `fetchCurrentHourAverage returns error when response list is empty`() = runTest {
        coEvery { apiService.getCurrentAvg() } returns emptyList()
        coEvery { cacheStore.getCachedPrice() } returns null
        
        val result = repository.fetchCurrentHourAverage()
        
        assertTrue(result is FetchResult.Error)
        assertEquals("No price data available", (result as FetchResult.Error).message)
    }

    @Test
    fun `fetchCurrentHourAverage returns error when price is null`() = runTest {
        coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = null, millisUtc = "100"))
        coEvery { cacheStore.getCachedPrice() } returns null
        
        val result = repository.fetchCurrentHourAverage()
        
        assertTrue(result is FetchResult.Error)
        assertEquals("No price data available", (result as FetchResult.Error).message)
    }

    @Test
    fun `fetchCurrentHourAverage returns error when price is blank`() = runTest {
        coEvery { apiService.getCurrentAvg() } returns listOf(CurrentAvgDto(price = "   ", millisUtc = "100"))
        coEvery { cacheStore.getCachedPrice() } returns null
        
        val result = repository.fetchCurrentHourAverage()
        
        assertTrue(result is FetchResult.Error)
        assertEquals("No price data available", (result as FetchResult.Error).message)
    }

    // --- fetchFiveMinutePrice Tests ---

    @Test
    fun `fetchFiveMinutePrice returns success and saves to cache when data is valid`() = runTest {
        val dto = CurrentAvgDto(price = "3.1", millisUtc = "1672531300000")
        coEvery { apiService.getFiveMinutePrice() } returns listOf(dto)
        
        val result = repository.fetchFiveMinutePrice()
        
        assertTrue(result is FetchResult.Success)
        val successResult = result as FetchResult.Success
        assertEquals("3.1", successResult.data.price)
        assertEquals(1672531300000L, successResult.data.timestampMillisUtc)
        
        coVerify { cacheStore.save(any()) }
    }

    @Test
    fun `fetchFiveMinutePrice returns error with fallback when api throws exception`() = runTest {
        coEvery { apiService.getFiveMinutePrice() } throws RuntimeException("Timeout error")
        val fallbackPrice = CachedPrice("2.0", 2000L)
        coEvery { cacheStore.getCachedPrice() } returns fallbackPrice
        
        val result = repository.fetchFiveMinutePrice()
        
        assertTrue(result is FetchResult.Error)
        val errorResult = result as FetchResult.Error
        assertEquals("Timeout error", errorResult.message)
        assertEquals(fallbackPrice, errorResult.cachedFallback)
    }

    @Test
    fun `fetchFiveMinutePrice returns error when response list is empty`() = runTest {
        coEvery { apiService.getFiveMinutePrice() } returns emptyList()
        coEvery { cacheStore.getCachedPrice() } returns null
        
        val result = repository.fetchFiveMinutePrice()
        
        assertTrue(result is FetchResult.Error)
        assertEquals("No price data available", (result as FetchResult.Error).message)
    }

    @Test
    fun `fetchFiveMinutePrice returns error when price is null`() = runTest {
        coEvery { apiService.getFiveMinutePrice() } returns listOf(CurrentAvgDto(price = null, millisUtc = "100"))
        coEvery { cacheStore.getCachedPrice() } returns null
        
        val result = repository.fetchFiveMinutePrice()
        
        assertTrue(result is FetchResult.Error)
        assertEquals("No price data available", (result as FetchResult.Error).message)
    }

    @Test
    fun `fetchFiveMinutePrice returns error when price is blank`() = runTest {
        coEvery { apiService.getFiveMinutePrice() } returns listOf(CurrentAvgDto(price = "   ", millisUtc = "100"))
        coEvery { cacheStore.getCachedPrice() } returns null
        
        val result = repository.fetchFiveMinutePrice()
        
        assertTrue(result is FetchResult.Error)
        assertEquals("No price data available", (result as FetchResult.Error).message)
    }
}
