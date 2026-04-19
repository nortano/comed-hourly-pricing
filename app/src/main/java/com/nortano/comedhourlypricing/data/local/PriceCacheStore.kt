package com.nortano.comedhourlypricing.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.tiles.TileUpdateRequester
import com.nortano.comedhourlypricing.data.CachedPrice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.priceCacheDataStore by preferencesDataStore(name = "price_cache")

class PriceCacheStore(private val context: Context) {
    private object Keys {
        val PRICE = stringPreferencesKey("last_price")
        val TIMESTAMP_UTC = longPreferencesKey("last_updated_millis")
    }

    suspend fun getCachedPrice(): CachedPrice? {
        return context.priceCacheDataStore.data.map { prefs ->
            val price = prefs[Keys.PRICE]
            val timestamp = prefs[Keys.TIMESTAMP_UTC]
            if (price != null && timestamp != null) CachedPrice(price, timestamp) else null
        }.first()
    }

    suspend fun save(price: CachedPrice) {
        context.priceCacheDataStore.edit { prefs ->
            prefs[Keys.PRICE] = price.price
            prefs[Keys.TIMESTAMP_UTC] = price.timestampMillisUtc
        }
        
        // Request complication update
        val component = android.content.ComponentName(
            context,
            com.nortano.comedhourlypricing.presentation.PriceComplicationService::class.java
        )
        val request = androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.create(
            context = context,
            complicationDataSourceComponent = component
        )
        request.requestUpdateAll()
        
        // Request tile update
        androidx.wear.tiles.TileService.getUpdater(context)
            .requestUpdate(com.nortano.comedhourlypricing.presentation.PriceTileService::class.java)
    }
}
