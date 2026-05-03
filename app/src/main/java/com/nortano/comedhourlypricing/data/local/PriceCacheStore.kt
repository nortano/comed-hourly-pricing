package com.nortano.comedhourlypricing.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nortano.comedhourlypricing.data.CachedPrice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.priceCacheDataStore by preferencesDataStore(name = "price_cache")

class PriceCacheStore(
    private val context: Context,
) {
    private object Keys {
        val PRICE = stringPreferencesKey("last_price")
        val HOURLY_AVG = stringPreferencesKey("last_hourly_avg")
        val FIVE_MIN = stringPreferencesKey("last_five_min")
        val TIMESTAMP_UTC = longPreferencesKey("last_updated_millis")
    }

    suspend fun getCachedPrice(): CachedPrice? =
        context.priceCacheDataStore.data
            .map { prefs ->
                val price = prefs[Keys.PRICE]
                val hourlyAvg = prefs[Keys.HOURLY_AVG]
                val fiveMin = prefs[Keys.FIVE_MIN]
                val timestamp = prefs[Keys.TIMESTAMP_UTC]
                if (price != null && timestamp != null) {
                    CachedPrice(
                        price = price,
                        timestampMillisUtc = timestamp,
                        hourlyAvgPrice = hourlyAvg,
                        fiveMinPrice = fiveMin,
                    )
                } else {
                    null
                }
            }.first()

    suspend fun save(price: CachedPrice) {
        context.priceCacheDataStore.edit { prefs ->
            prefs[Keys.PRICE] = price.price
            if (price.hourlyAvgPrice != null) {
                prefs[Keys.HOURLY_AVG] = price.hourlyAvgPrice
            } else {
                prefs.remove(Keys.HOURLY_AVG)
            }
            if (price.fiveMinPrice != null) {
                prefs[Keys.FIVE_MIN] = price.fiveMinPrice
            } else {
                prefs.remove(Keys.FIVE_MIN)
            }
            prefs[Keys.TIMESTAMP_UTC] = price.timestampMillisUtc
        }

        // Request complication updates
        val services =
            listOf(
                com.nortano.comedhourlypricing.presentation.FiveMinPriceComplicationService::class.java,
                com.nortano.comedhourlypricing.presentation.HourlyPriceComplicationService::class.java,
            )
        services.forEach { serviceClass ->
            val component = android.content.ComponentName(context, serviceClass)
            val request =
                androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.create(
                    context = context,
                    complicationDataSourceComponent = component,
                )
            request.requestUpdateAll()
        }

        // Request tile updates
        val tileUpdater =
            androidx.wear.tiles.TileService
                .getUpdater(context)
        tileUpdater.requestUpdate(com.nortano.comedhourlypricing.presentation.FiveMinPriceTileService::class.java)
        tileUpdater.requestUpdate(com.nortano.comedhourlypricing.presentation.HourlyPriceTileService::class.java)
    }
}
