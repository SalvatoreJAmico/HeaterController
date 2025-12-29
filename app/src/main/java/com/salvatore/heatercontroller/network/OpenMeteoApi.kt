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

private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"

// Minimal Open-Meteo current weather response
// https://open-meteo.com/en/docs

data class OpenMeteoWeatherResponse(
    val latitude: Double?,
    val longitude: Double?,
    val current: OpenMeteoCurrent?
)

data class OpenMeteoCurrent(
    val temperature_2m: Double?,
    val relative_humidity_2m: Double?
)

interface OpenMeteoWeatherService {
    @GET("v1/forecast")
    suspend fun currentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m",
        @Query("temperature_unit") tempUnit: String = "fahrenheit"
    ): Response<OpenMeteoWeatherResponse>
}

object OpenMeteoApi {
    fun create(): OpenMeteoWeatherService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl(OPEN_METEO_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoWeatherService::class.java)
    }
}
