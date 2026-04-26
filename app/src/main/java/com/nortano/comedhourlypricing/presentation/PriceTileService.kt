package com.nortano.comedhourlypricing.presentation

import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.tiles.SuspendingTileService
import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.data.PriceRepository
import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.RetrofitClient
import com.nortano.comedhourlypricing.ui.PriceTier
import com.nortano.comedhourlypricing.ui.color
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(com.google.android.horologist.annotations.ExperimentalHorologistApi::class)
class PriceTileService : SuspendingTileService() {
    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val cacheStore = PriceCacheStore(applicationContext)

        var showNoNewDataMessage = false

        if (requestParams.currentState.lastClickableId == "refresh") {
            val oldTimestamp = cacheStore.getCachedPrice()?.timestampMillisUtc
            val repository = PriceRepository(RetrofitClient.apiService, cacheStore)
            repository.fetchCurrentHourAverage()
            val newTimestamp = cacheStore.getCachedPrice()?.timestampMillisUtc

            if (oldTimestamp != null && oldTimestamp == newTimestamp) {
                showNoNewDataMessage = true
            }
        }

        val cachedData = cacheStore.getCachedPrice()

        // As a best practice, tiles shouldn't block rendering waiting for the network.
        // We'll read from cache. A background Worker handles refreshing the cache.
        val priceText = cachedData?.price ?: getString(R.string.empty_price)
        val timestamp = cachedData?.timestampMillisUtc

        val tier = PriceTier.fromPrice(priceText)
        val deviceParams = requestParams.deviceConfiguration

        if (showNoNewDataMessage) {
            // Schedule a refresh in 5 seconds to clear the "no new data" text
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                kotlinx.coroutines.delay(5000)
                androidx.wear.tiles.TileService
                    .getUpdater(applicationContext)
                    .requestUpdate(PriceTileService::class.java)
            }
        }

        return TileBuilders.Tile
            .Builder()
            .setResourcesVersion("2")
            .setTileTimeline(
                TimelineBuilders.Timeline
                    .Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry
                            .Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout
                                    .Builder()
                                    .setRoot(
                                        createLayout(priceText, timestamp, tier, deviceParams, showNoNewDataMessage),
                                    ).build(),
                            ).build(),
                    ).build(),
            ).setFreshnessIntervalMillis(0) // Let SyncWorker or manual refresh handle updates
            .build()
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources =
        ResourceBuilders.Resources
            .Builder()
            .setVersion("2")
            .build()

    private fun createLayout(
        priceText: String,
        timestamp: Long?,
        tier: PriceTier,
        deviceParameters: DeviceParameters,
        showNoNewDataMessage: Boolean = false,
    ): LayoutElementBuilders.LayoutElement {
        val clickAction =
            ActionBuilders.LaunchAction
                .Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity
                        .Builder()
                        .setClassName(MainActivity::class.java.name)
                        .setPackageName(this.packageName)
                        .build(),
                ).build()

        val modifiers =
            ModifiersBuilders.Modifiers
                .Builder()
                .setClickable(
                    ModifiersBuilders.Clickable
                        .Builder()
                        .setId("open_app")
                        .setOnClick(clickAction)
                        .build(),
                ).build()

        val formatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
        val timeText = timestamp?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: getString(R.string.empty_price)

        val contentColumn =
            LayoutElementBuilders.Column
                .Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    Text
                        .Builder(applicationContext, getString(R.string.price_with_unit, priceText))
                        .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                        .setColor(ColorBuilders.argb(tier.color.toArgb()))
                        .build(),
                ).addContent(
                    LayoutElementBuilders.Spacer
                        .Builder()
                        .setHeight(dp(4f))
                        .build(),
                ).addContent(
                    Text
                        .Builder(applicationContext, getString(R.string.as_of_time, timeText))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(ContextCompat.getColor(this, android.R.color.white)))
                        .build(),
                ).build()

        val primaryLayout =
            PrimaryLayout
                .Builder(deviceParameters)
                .setResponsiveContentInsetEnabled(true)
                .setPrimaryLabelTextContent(
                    Text
                        .Builder(applicationContext, getString(R.string.tile_label))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb("#AAAAAA".toColorInt()))
                        .build(),
                ).setContent(contentColumn)

        val refreshClickable =
            ModifiersBuilders.Clickable
                .Builder()
                .setId("refresh")
                .setOnClick(
                    ActionBuilders.LoadAction.Builder().build(),
                ).build()

        val refreshChip =
            CompactChip
                .Builder(
                    applicationContext,
                    getString(R.string.refresh_description),
                    refreshClickable,
                    deviceParameters,
                ).build()

        if (showNoNewDataMessage) {
            primaryLayout.setSecondaryLabelTextContent(
                Text
                    .Builder(applicationContext, getString(R.string.no_new_data))
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .setItalic(true)
                    .setColor(ColorBuilders.argb("#AAAAAA".toColorInt()))
                    .build(),
            )
        }

        primaryLayout.setPrimaryChipContent(refreshChip)

        return LayoutElementBuilders.Box
            .Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(primaryLayout.build())
            .setModifiers(modifiers)
            .build()
    }
}
