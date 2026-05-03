package com.nortano.comedhourlypricing.ui

enum class PriceTier {
    FREE,
    NORMAL,
    ELEVATED,
    HIGH,
    UNKNOWN,
    ;

    companion object {
        fun fromPrice(priceText: String?): PriceTier {
            val price = priceText?.toDoubleOrNull() ?: return UNKNOWN
            if (price.isNaN() || price.isInfinite()) return UNKNOWN
            return when {
                price <= 0.0 -> FREE
                price <= 5.0 -> NORMAL
                price <= 9.9 -> ELEVATED
                else -> HIGH
            }
        }
    }
}

enum class PriceTrend {
    UP,
    DOWN,
    STAGNANT,
    UNKNOWN,
    ;

    companion object {
        fun calculate(
            fiveMinPrice: String?,
            hourlyAvgPrice: String?,
        ): PriceTrend {
            val fiveMin = fiveMinPrice?.toDoubleOrNull() ?: return UNKNOWN
            val hourly = hourlyAvgPrice?.toDoubleOrNull() ?: return UNKNOWN

            return when {
                fiveMin > hourly -> UP
                fiveMin < hourly -> DOWN
                else -> STAGNANT
            }
        }
    }
}

data class PriceUiState(
    val isRefreshing: Boolean = false,
    val priceText: String? = null,
    val hourlyAvgPriceText: String? = null,
    val priceTier: PriceTier = PriceTier.UNKNOWN,
    val priceTrend: PriceTrend = PriceTrend.UNKNOWN,
    val updatedAtMillis: Long? = null,
    val errorMessage: String? = null,
)
