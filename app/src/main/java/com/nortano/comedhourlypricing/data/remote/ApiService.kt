package com.nortano.comedhourlypricing.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api?type=currenthouraverage")
    suspend fun getCurrentAvg(): List<CurrentAvgDto>

    @GET("api?type=5minutefeed")
    suspend fun getFiveMinutePrice(
        @Query("datestart") start: String? = null,
        @Query("dateend") end: String? = null,
    ): List<CurrentAvgDto>
}
