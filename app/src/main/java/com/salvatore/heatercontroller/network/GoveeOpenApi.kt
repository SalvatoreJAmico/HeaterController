package com.salvatore.heatercontroller.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.salvatore.heatercontroller.network.NetMetrics
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private const val OPENAPI_BASE_URL = "https://openapi.api.govee.com/"

// Request and response models for OpenAPI device state
data class OpenDeviceStateRequest(
    val requestId: String,
    val payload: OpenDeviceStatePayload
)

data class OpenDeviceStatePayload(
    val sku: String,
    val device: String
)

data class OpenDeviceStateResponse(
    val requestId: String?,
    val msg: String?,
    val code: Int?,
    val payload: OpenDeviceStateResult?
)

data class OpenDeviceStateResult(
    val sku: String?,
    val device: String?,
    val capabilities: List<OpenCapability>?
)

data class OpenCapability(
    val type: String?,
    val instance: String?,
    val state: OpenCapabilityState?
)

data class OpenCapabilityState(
    val value: Any?
)

interface GoveeOpenApiService {
    @POST("router/api/v1/device/state")
    suspend fun getDeviceState(
        @Body body: OpenDeviceStateRequest
    ): Response<OpenDeviceStateResponse>
}

object GoveeOpenApi {
    fun create(apiKey: String): GoveeOpenApiService {
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
            .baseUrl(OPENAPI_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoveeOpenApiService::class.java)
    }
}
