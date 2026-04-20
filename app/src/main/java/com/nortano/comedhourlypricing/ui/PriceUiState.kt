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

data class PriceUiState(
    val isRefreshing: Boolean = false,
    val priceText: String? = null,
    val hourlyAvgPriceText: String? = null,
    val priceTier: PriceTier = PriceTier.UNKNOWN,
    val updatedAtMillis: Long? = null,
    val errorMessage: String? = null,
)
