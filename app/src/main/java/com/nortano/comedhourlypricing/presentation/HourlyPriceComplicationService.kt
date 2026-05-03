package com.nortano.comedhourlypricing.presentation

import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.data.CachedPrice

class HourlyPriceComplicationService : BasePriceComplicationService() {
    override fun getPriceText(cachedData: CachedPrice?): String =
        cachedData?.hourlyAvgPrice ?: getString(R.string.empty_price)

    override fun getLabelResId(): Int = R.string.complication_label_hourly
}
