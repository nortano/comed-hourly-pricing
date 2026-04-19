package com.nortano.comedhourlypricing.presentation

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage

import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.data.FetchResult
import com.nortano.comedhourlypricing.data.PriceRepository
import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.RetrofitClient
import com.nortano.comedhourlypricing.ui.PriceTier
import com.nortano.comedhourlypricing.ui.color
import androidx.compose.ui.graphics.toArgb

class PriceComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val cacheStore = PriceCacheStore(applicationContext)
        
        // Complications should use local cache to avoid exceeding the system timeout
        val cachedData = cacheStore.getCachedPrice()
        val priceText = cachedData?.price ?: getString(R.string.empty_price)

        val tier = PriceTier.fromPrice(priceText)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val icon = Icon.createWithResource(this, R.drawable.ic_bolt)
                
                // Note: SmallImageType.PHOTO is required to preserve colors in watch faces.
                // However, many watch faces still aggressively tint icons or strip colors from PHOTO types 
                // if they are configured to be purely monochromatic.
                icon.setTint(tier.color.toArgb())

                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text = getString(R.string.price_with_unit, priceText)).build(),
                    contentDescription = PlainComplicationText.Builder(text = getString(R.string.tile_label)).build()
                )
                .setMonochromaticImage(
                    androidx.wear.watchface.complications.data.MonochromaticImage.Builder(
                        image = icon
                    )
                    .setAmbientImage(icon)
                    .build()
                )
                .setTapAction(pendingIntent)
                .build()
            }
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        
        val icon = Icon.createWithResource(this, R.drawable.ic_bolt)

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = getString(R.string.price_with_unit, "2.4")).build(),
            contentDescription = PlainComplicationText.Builder(text = getString(R.string.tile_label)).build()
        )
        .setMonochromaticImage(
            androidx.wear.watchface.complications.data.MonochromaticImage.Builder(
                image = icon
            )
            .setAmbientImage(icon)
            .build()
        )
        .build()
    }
}