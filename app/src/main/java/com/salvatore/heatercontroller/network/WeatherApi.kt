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

private const val WEATHER_BASE_URL = "https://api.openweathermap.org/"

// Minimal subset of OpenWeatherMap current weather response
data class WeatherResponse(
    val main: WeatherMain?
)

data class WeatherMain(
    val temp: Double?,
    val humidity: Int?
)

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun weatherByCity(
        @Query("q") city: String,
        @Query("appid") appId: String,
        @Query("units") units: String = "imperial"
    ): Response<WeatherResponse>

    @GET("data/2.5/weather")
    suspend fun weatherByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appId: String,
        @Query("units") units: String = "imperial"
    ): Response<WeatherResponse>
}

object WeatherApi {
    fun create(): WeatherApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApiService::class.java)
    }
}
