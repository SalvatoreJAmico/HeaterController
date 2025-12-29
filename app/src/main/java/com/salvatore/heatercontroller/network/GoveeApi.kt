package com.salvatore.heatercontroller.network

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.salvatore.heatercontroller.network.NetMetrics
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

// Govee Developer API base
private const val BASE_URL = "https://developer-api.govee.com/"

// Minimal models based on public docs; properties can vary by device
data class DevicesResponse(
    val data: List<DeviceInfo>?
)

data class DeviceInfo(
    val device: String?,
    val model: String?,
    val deviceName: String?
)

data class DeviceStateResponse(
    val data: DeviceStateData?
)

data class DeviceStateData(
    val device: String?,
    val model: String?,
    val properties: List<Property>?
)

data class Property(
    val online: Boolean?,
    @Json(name = "powerState") val powerState: String?,
    val brightness: Int?,
    val color: Map<String, Any>?,
    // Thermo-hygrometer devices may expose temperature/humidity properties
    val temperature: Double?,
    val humidity: Double?
)

interface GoveeApiService {
    @GET("v1/devices")
    suspend fun getDevices(): Response<DevicesResponse>

    @GET("v1/devices/state")
    suspend fun getDeviceState(
        @Query("device") device: String,
        @Query("model") model: String
    ): Response<DeviceStateResponse>

    @PUT("v1/devices/control")
    suspend fun controlDevice(
        @Body body: ControlRequest
    ): Response<ControlResponse>
}

object GoveeApi {
    fun create(apiKey: String): GoveeApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val headerInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Govee-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                NetMetrics.recordResponse(response.header("Date"), response.isSuccessful)
                response
            }
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoveeApiService::class.java)
    }
}

// Control API models
data class ControlRequest(
    val device: String,
    val model: String,
    val cmd: ControlCommand
)

data class ControlCommand(
    val name: String,
    val value: String
)

data class ControlResponse(
    val message: String?
)
