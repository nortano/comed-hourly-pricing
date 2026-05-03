package com.nortano.comedhourlypricing.data

data class CachedPrice(
    val price: String,
    val timestampMillisUtc: Long,
    val hourlyAvgPrice: String? = null,
    val fiveMinPrice: String? = null,
)
