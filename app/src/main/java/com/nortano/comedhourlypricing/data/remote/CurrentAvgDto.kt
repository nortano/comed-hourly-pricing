package com.nortano.comedhourlypricing.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentAvgDto(
    @SerialName("millisUTC") val millisUtc: String? = null,
    @SerialName("price") val price: String? = null,
)
