package com.nortano.comedhourlypricing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nortano.comedhourlypricing.data.FetchResult
import com.nortano.comedhourlypricing.data.PriceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PriceViewModel(
    private val repository: PriceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PriceUiState())
    val uiState: StateFlow<PriceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = repository.getCachedPrice()
            _uiState.update {
                it.copy(
                    priceText = cached?.price,
                    hourlyAvgPriceText = cached?.hourlyAvgPrice,
                    priceTier = PriceTier.fromPrice(cached?.price),
                    priceTrend = PriceTrend.calculate(cached?.fiveMinPrice, cached?.hourlyAvgPrice),
                    updatedAtMillis = cached?.timestampMillisUtc,
                )
            }
            refresh()
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            val result = repository.fetchPricesCombined()

            if (result is FetchResult.Success) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        priceText = result.data.price,
                        hourlyAvgPriceText = result.data.hourlyAvgPrice,
                        priceTier = PriceTier.fromPrice(result.data.price),
                        priceTrend = PriceTrend.calculate(result.data.fiveMinPrice, result.data.hourlyAvgPrice),
                        updatedAtMillis = result.data.timestampMillisUtc,
                        errorMessage = null,
                    )
                }
            } else {
                val errorMsg = (result as? FetchResult.Error)?.message ?: "Update failed"
                val fallback = (result as? FetchResult.Error)?.cachedFallback

                _uiState.update {
                    val price = fallback?.price ?: it.priceText
                    val hourly = fallback?.hourlyAvgPrice ?: it.hourlyAvgPriceText
                    val fiveMin = fallback?.fiveMinPrice ?: it.priceText

                    it.copy(
                        isRefreshing = false,
                        priceText = price,
                        hourlyAvgPriceText = hourly,
                        priceTier = PriceTier.fromPrice(price),
                        priceTrend = PriceTrend.calculate(fiveMin, hourly),
                        updatedAtMillis = fallback?.timestampMillisUtc ?: it.updatedAtMillis,
                        errorMessage = errorMsg,
                    )
                }
            }
        }
    }

    class Factory(
        private val repository: PriceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PriceViewModel(repository) as T
    }
}
