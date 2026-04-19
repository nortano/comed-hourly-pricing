package com.nortano.comedhourlypricing.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nortano.comedhourlypricing.data.PriceRepository
import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.RetrofitClient
import com.nortano.comedhourlypricing.presentation.theme.ComedHourlyPricingTheme
import com.nortano.comedhourlypricing.ui.PriceScreen
import com.nortano.comedhourlypricing.ui.PriceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val repository = PriceRepository(
            apiService = RetrofitClient.apiService,
            cacheStore = PriceCacheStore(applicationContext)
        )

        setContent {
            ComedHourlyPricingTheme {
                val viewModel: PriceViewModel = viewModel(
                    factory = PriceViewModel.Factory(repository)
                )

                // Keep the splash screen on-screen until the initial refresh finishes
                splashScreen.setKeepOnScreenCondition {
                    viewModel.uiState.value.isRefreshing
                }

                PriceScreen(viewModel = viewModel)
            }
        }
    }
}
