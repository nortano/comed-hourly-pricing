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
    private val repository: PriceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceUiState())
    val uiState: StateFlow<PriceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = repository.getCachedPrice()
            _uiState.update {
                it.copy(
                    priceText = cached?.price,
                    priceTier = PriceTier.fromPrice(cached?.price),
                    updatedAtMillis = cached?.timestampMillisUtc
                )
            }
            refresh()
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            val fiveMinResult = repository.fetchFiveMinutePrice()
            val hourlyAvgResult = repository.fetchCurrentHourAverage()

            if (fiveMinResult is FetchResult.Success && hourlyAvgResult is FetchResult.Success) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        priceText = fiveMinResult.data.price,
                        hourlyAvgPriceText = hourlyAvgResult.data.price,
                        priceTier = PriceTier.fromPrice(fiveMinResult.data.price),
                        updatedAtMillis = fiveMinResult.data.timestampMillisUtc,
                        errorMessage = null
                    )
                }
            } else {
                val errorMsg = when {
                    fiveMinResult is FetchResult.Error -> fiveMinResult.message
                    hourlyAvgResult is FetchResult.Error -> hourlyAvgResult.message
                    else -> "Update failed"
                }

                _uiState.update {
                    val fallbackPrice = when (fiveMinResult) {
                        is FetchResult.Success -> fiveMinResult.data.price
                        is FetchResult.Error -> fiveMinResult.cachedFallback?.price ?: it.priceText
                    }
                    val hourlyFallbackPrice = when (hourlyAvgResult) {
                        is FetchResult.Success -> hourlyAvgResult.data.price
                        is FetchResult.Error -> hourlyAvgResult.cachedFallback?.price ?: it.hourlyAvgPriceText
                    }
                    val fallbackTimestamp = when (fiveMinResult) {
                        is FetchResult.Success -> fiveMinResult.data.timestampMillisUtc
                        is FetchResult.Error -> fiveMinResult.cachedFallback?.timestampMillisUtc ?: it.updatedAtMillis
                    }

                    it.copy(
                        isRefreshing = false,
                        priceText = fallbackPrice,
                        hourlyAvgPriceText = hourlyFallbackPrice,
                        priceTier = PriceTier.fromPrice(fallbackPrice),
                        updatedAtMillis = fallbackTimestamp,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    class Factory(private val repository: PriceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PriceViewModel(repository) as T
        }
    }
}
