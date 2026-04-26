package com.nortano.comedhourlypricing.data

import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.ApiService

class PriceRepository(
    private val apiService: ApiService,
    private val cacheStore: PriceCacheStore,
) {
    suspend fun getCachedPrice(): CachedPrice? = cacheStore.getCachedPrice()

    suspend fun fetchCurrentHourAverage(): FetchResult =
        try {
            val response = apiService.getCurrentAvg()
            processResponse(response)
        } catch (exception: Exception) {
            FetchResult.Error(
                message = exception.message ?: "Failed to fetch data",
                cachedFallback = cacheStore.getCachedPrice(),
            )
        }

    suspend fun fetchFiveMinutePrice(): FetchResult =
        try {
            val response = apiService.getFiveMinutePrice()
            processResponse(response)
        } catch (exception: Exception) {
            FetchResult.Error(
                message = exception.message ?: "Failed to fetch data",
                cachedFallback = cacheStore.getCachedPrice(),
            )
        }

    private suspend fun processResponse(
        response: List<com.nortano.comedhourlypricing.data.remote.CurrentAvgDto>,
    ): FetchResult {
        val dto = response.firstOrNull()
        val price = dto?.price

        return if (price.isNullOrBlank()) {
            FetchResult.Error("No price data available", cacheStore.getCachedPrice())
        } else {
            val timestamp = dto.millisUtc?.toLongOrNull() ?: System.currentTimeMillis()
            val cachedPrice = CachedPrice(price = price, timestampMillisUtc = timestamp)

            val currentCache = cacheStore.getCachedPrice()
            if (currentCache?.timestampMillisUtc != timestamp) {
                cacheStore.save(cachedPrice)
            }

            FetchResult.Success(cachedPrice)
        }
    }
}
