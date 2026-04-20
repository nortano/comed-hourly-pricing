package com.nortano.comedhourlypricing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun PriceScreenPreview() {
    MaterialTheme {
        PriceScreenContent(
            state = PriceUiState(
                priceText = "2.4",
                hourlyAvgPriceText = "2.5",
                priceTier = PriceTier.NORMAL,
                isRefreshing = false,
                updatedAtMillis = System.currentTimeMillis() - 1000 * 60 * 5 // 5 minutes ago
            ),
            onRefresh = {}
        )
    }
}
