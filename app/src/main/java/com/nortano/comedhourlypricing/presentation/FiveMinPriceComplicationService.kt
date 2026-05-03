package com.nortano.comedhourlypricing.presentation

import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.data.CachedPrice

class FiveMinPriceComplicationService : BasePriceComplicationService() {
    override fun getPriceText(cachedData: CachedPrice?): String =
        cachedData?.fiveMinPrice ?: getString(R.string.empty_price)

    override fun getLabelResId(): Int = R.string.complication_label_5min
}
