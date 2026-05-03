package com.nortano.comedhourlypricing.data

import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class PriceRepository(
    private val apiService: ApiService,
    private val cacheStore: PriceCacheStore,
) {
    suspend fun getCachedPrice(): CachedPrice? = cacheStore.getCachedPrice()

    suspend fun fetchPricesCombined(): FetchResult =
        try {
            coroutineScope {
                val fiveMinDeferred = async { apiService.getFiveMinutePrice() }
                val hourlyAvgDeferred = async { apiService.getCurrentAvg() }

                val fiveMinResponse = fiveMinDeferred.await()
                val hourlyAvgResponse = hourlyAvgDeferred.await()

                val fiveMinDto = fiveMinResponse.firstOrNull()
                val hourlyAvgDto = hourlyAvgResponse.firstOrNull()

                val fiveMinPrice = fiveMinDto?.price
                val hourlyAvgPrice = hourlyAvgDto?.price

                if (fiveMinPrice.isNullOrBlank() || hourlyAvgPrice.isNullOrBlank()) {
                    FetchResult.Error("Missing price data", cacheStore.getCachedPrice())
                } else {
                    val timestamp = fiveMinDto.millisUtc?.toLongOrNull() ?: System.currentTimeMillis()
                    val cachedPrice =
                        CachedPrice(
                            price = fiveMinPrice,
                            timestampMillisUtc = timestamp,
                            hourlyAvgPrice = hourlyAvgPrice,
                            fiveMinPrice = fiveMinPrice,
                        )

                    val currentCache = cacheStore.getCachedPrice()
                    if (currentCache?.timestampMillisUtc != timestamp ||
                        currentCache.hourlyAvgPrice != hourlyAvgPrice ||
                        currentCache.fiveMinPrice != fiveMinPrice
                    ) {
                        cacheStore.save(cachedPrice)
                    }

                    FetchResult.Success(cachedPrice)
                }
            }
        } catch (exception: Exception) {
            FetchResult.Error(
                message = exception.message ?: "Failed to fetch data",
                cachedFallback = cacheStore.getCachedPrice(),
            )
        }

    suspend fun fetchCurrentHourAverage(): FetchResult = fetchPricesCombined()

    suspend fun fetchFiveMinutePrice(): FetchResult = fetchPricesCombined()
}
