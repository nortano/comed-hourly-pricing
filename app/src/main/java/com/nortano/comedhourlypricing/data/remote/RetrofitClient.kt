package com.nortano.comedhourlypricing.data.remote

import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://hourlypricing.comed.com/"

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    private val okHttpClient: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(10.seconds)
            .readTimeout(10.seconds)
            .writeTimeout(10.seconds)
            .build()

    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
