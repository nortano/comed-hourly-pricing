package com.nortano.comedhourlypricing.data

sealed class FetchResult {
    data class Success(val data: CachedPrice) : FetchResult()
    data class Error(val message: String, val cachedFallback: CachedPrice?) : FetchResult()
}
