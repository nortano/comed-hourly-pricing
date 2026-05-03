package com.nortano.comedhourlypricing.presentation

import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.data.CachedPrice

class HourlyPriceTileService : BasePriceTileService() {
    override fun getPriceText(cachedData: CachedPrice?): String =
        cachedData?.hourlyAvgPrice ?: getString(R.string.empty_price)

    override fun getLabelResId(): Int = R.string.tile_label_hourly
}
