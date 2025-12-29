package com.salvatore.heatercontroller.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val OPEN_METEO_GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"

// https://open-meteo.com/en/docs/geocoding-api

data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult>?
)

data class OpenMeteoGeocodingResult(
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val country_code: String?,
    val admin1: String?
)

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): Response<OpenMeteoGeocodingResponse>
}

object OpenMeteoGeocodingApi {
    fun create(): OpenMeteoGeocodingService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl(OPEN_METEO_GEOCODING_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingService::class.java)
    }
}
