package com.nortano.comedhourlypricing.presentation

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.ui.PriceTier
import com.nortano.comedhourlypricing.ui.color

@Preview
fun tilePreview(context: Context): TilePreviewData {
    val deviceParameters = DeviceParametersBuilders.DeviceParameters.Builder().build()
    
    val contentColumn = LayoutElementBuilders.Column.Builder()
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder(context, context.getString(R.string.price_with_unit, "2.4"))
                .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                .setColor(ColorBuilders.argb(PriceTier.NORMAL.color.toArgb()))
                .build()
        )
        .addContent(
            LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build()
        )
        .addContent(
            Text.Builder(context, context.getString(R.string.as_of_time, "10:30 AM"))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(ContextCompat.getColor(context, android.R.color.white)))
                .build()
        )
        .build()

    val primaryLayout = PrimaryLayout.Builder(deviceParameters)
        .setResponsiveContentInsetEnabled(true)
        .setPrimaryLabelTextContent(
            Text.Builder(context, context.getString(R.string.tile_label))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .setColor(ColorBuilders.argb(ContextCompat.getColor(context, android.R.color.white)))
                .build()
        )
        .setContent(contentColumn)
        .build()

    val box = LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(primaryLayout)
        .build()

    return TilePreviewData(
        onTileRequest = { request -> 
            TilePreviewHelper.singleTimelineEntryTileBuilder(box).build()
        }
    )
}
