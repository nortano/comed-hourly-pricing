package com.nortano.comedhourlypricing.presentation

import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.SpanText
import androidx.wear.protolayout.LayoutElementBuilders.Spannable
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
import com.nortano.comedhourlypricing.data.CachedPrice
import com.nortano.comedhourlypricing.data.PriceRepository
import com.nortano.comedhourlypricing.data.local.PriceCacheStore
import com.nortano.comedhourlypricing.data.remote.RetrofitClient
import com.nortano.comedhourlypricing.ui.PriceTier
import com.nortano.comedhourlypricing.ui.PriceTrend
import com.nortano.comedhourlypricing.ui.color
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(com.google.android.horologist.annotations.ExperimentalHorologistApi::class)
abstract class BasePriceTileService : SuspendingTileService() {
    abstract fun getPriceText(cachedData: CachedPrice?): String

    abstract fun getLabelResId(): Int

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val cacheStore = PriceCacheStore(applicationContext)

        var showNoNewDataMessage = false

        if (requestParams.currentState.lastClickableId == "refresh") {
            val oldTimestamp = cacheStore.getCachedPrice()?.timestampMillisUtc
            val repository = PriceRepository(RetrofitClient.apiService, cacheStore)
            repository.fetchPricesCombined()
            val newTimestamp = cacheStore.getCachedPrice()?.timestampMillisUtc

            if (oldTimestamp != null && oldTimestamp == newTimestamp) {
                showNoNewDataMessage = true
            }
        }

        val cachedData = cacheStore.getCachedPrice()
        val priceText = getPriceText(cachedData)
        val timestamp = cachedData?.timestampMillisUtc
        val trend = PriceTrend.calculate(cachedData?.fiveMinPrice, cachedData?.hourlyAvgPrice)

        val tier = PriceTier.fromPrice(priceText)
        val deviceParams = requestParams.deviceConfiguration

        if (showNoNewDataMessage) {
            // Schedule a refresh in 5 seconds to clear the "no new data" text
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                kotlinx.coroutines.delay(5000)
                getUpdater(applicationContext)
                    .requestUpdate(this@BasePriceTileService::class.java)
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
                                        createLayout(
                                            priceText = priceText,
                                            timestamp = timestamp,
                                            tier = tier,
                                            trend = trend,
                                            deviceParameters = deviceParams,
                                            showNoNewDataMessage = showNoNewDataMessage,
                                        ),
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
            .addIdToImageMapping(
                "ic_trending_up",
                ResourceBuilders.ImageResource
                    .Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId
                            .Builder()
                            .setResourceId(R.drawable.ic_trending_up)
                            .build(),
                    ).build(),
            ).addIdToImageMapping(
                "ic_trending_down",
                ResourceBuilders.ImageResource
                    .Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId
                            .Builder()
                            .setResourceId(R.drawable.ic_trending_down)
                            .build(),
                    ).build(),
            ).build()

    private fun createLayout(
        priceText: String,
        timestamp: Long?,
        tier: PriceTier,
        trend: PriceTrend,
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

        val priceRow =
            LayoutElementBuilders.Row
                .Builder()
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

        if (trend == PriceTrend.DOWN) {
            @Suppress("DEPRECATION")
            val image =
                LayoutElementBuilders.Image
                    .Builder()
                    .setResourceId("ic_trending_down")
                    .setWidth(dp(20f))
                    .setHeight(dp(20f))
                    .build()
            priceRow
                .addContent(image)
                .addContent(
                    LayoutElementBuilders.Spacer
                        .Builder()
                        .setWidth(dp(4f))
                        .build(),
                )
        }

        val color = ColorBuilders.argb(tier.color.toArgb())

        val priceSpannable =
            Spannable
                .Builder()
                .addSpan(
                    SpanText
                        .Builder()
                        .setText(priceText)
                        .setFontStyle(
                            FontStyle
                                .Builder()
                                .setSize(sp(48f))
                                .setColor(color)
                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                .build(),
                        ).build(),
                ).addSpan(
                    SpanText
                        .Builder()
                        .setText(" ¢")
                        .setFontStyle(
                            FontStyle
                                .Builder()
                                .setSize(sp(24f))
                                .setColor(color)
                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                .build(),
                        ).build(),
                ).build()

        priceRow.addContent(priceSpannable)

        if (trend == PriceTrend.UP) {
            @Suppress("DEPRECATION")
            val image =
                LayoutElementBuilders.Image
                    .Builder()
                    .setResourceId("ic_trending_up")
                    .setWidth(dp(20f))
                    .setHeight(dp(20f))
                    .build()
            priceRow
                .addContent(
                    LayoutElementBuilders.Spacer
                        .Builder()
                        .setWidth(dp(4f))
                        .build(),
                ).addContent(image)
        }

        val contentColumnBuilder =
            LayoutElementBuilders.Column
                .Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(priceRow.build())
                .addContent(
                    LayoutElementBuilders.Spacer
                        .Builder()
                        .setHeight(dp(2f))
                        .build(),
                ).addContent(
                    Text
                        .Builder(applicationContext, getString(R.string.as_of_time, timeText))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(ContextCompat.getColor(this, android.R.color.white)))
                        .build(),
                )

        if (showNoNewDataMessage) {
            contentColumnBuilder
                .addContent(
                    LayoutElementBuilders.Spacer
                        .Builder()
                        .setHeight(dp(2f))
                        .build(),
                ).addContent(
                    Text
                        .Builder(applicationContext, getString(R.string.no_new_data))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                        .setItalic(true)
                        .setColor(ColorBuilders.argb("#AAAAAA".toColorInt()))
                        .build(),
                )
        }

        val primaryLayout =
            PrimaryLayout
                .Builder(deviceParameters)
                .setResponsiveContentInsetEnabled(true)
                .setPrimaryLabelTextContent(
                    Text
                        .Builder(applicationContext, getString(getLabelResId()))
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb("#AAAAAA".toColorInt()))
                        .build(),
                ).setContent(contentColumnBuilder.build())

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
