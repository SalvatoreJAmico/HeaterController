package com.salvatore.heatercontroller.data

import com.salvatore.heatercontroller.ApiKeys
import com.salvatore.heatercontroller.BuildConfig
import com.salvatore.heatercontroller.network.GoveeApi
import com.salvatore.heatercontroller.network.GoveeOpenApi
import com.salvatore.heatercontroller.network.OpenDeviceStatePayload
import com.salvatore.heatercontroller.network.OpenDeviceStateRequest
import com.salvatore.heatercontroller.network.DeviceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorRepository {
    private val service by lazy { GoveeApi.create(ApiKeys.main) }
    private val openService by lazy { GoveeOpenApi.create(ApiKeys.main) }

    private fun anyToDouble(value: Any?): Double? {
        return when (value) {
            null -> null
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            is Map<*, *> -> {
                // Try common keys that may hold the numeric value
                val keys = listOf("current", "value", "temperature", "humidity")
                val candidate = keys.firstNotNullOfOrNull { k ->
                    val v = value[k]
                    when (v) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull()
                        else -> null
                    }
                }
                candidate ?: value.values.firstNotNullOfOrNull {
                    when (it) {
                        is Number -> it.toDouble()
                        is String -> it.toDoubleOrNull()
                        else -> null
                    }
                }
            }
            else -> null
        }
    }

    private fun findCapabilityValue(caps: List<com.salvatore.heatercontroller.network.OpenCapability>, keyword: String): Double? {
        val cap = caps.firstOrNull {
            val inst = it.instance?.lowercase()
            val type = it.type?.lowercase()
            inst?.contains(keyword) == true || type?.contains(keyword) == true
        }
        return anyToDouble(cap?.state?.value)
    }

    suspend fun readTemperature(device: String, model: String): Double? = withContext(Dispatchers.IO) {
        if (device.isBlank() || model.isBlank()) return@withContext null
        // H5100 sensors are not supported by Developer API state; use OpenAPI.
        if (model.equals("H5100", ignoreCase = true)) {
            val req = OpenDeviceStateRequest(
                requestId = java.util.UUID.randomUUID().toString(),
                payload = OpenDeviceStatePayload(sku = model, device = device)
            )
            val resp = openService.getDeviceState(req)
            if (!resp.isSuccessful) return@withContext null
            val caps = resp.body()?.payload?.capabilities ?: emptyList()
            val temp = findCapabilityValue(caps, "temperature")
            Log.d("SensorRepository", "Temperature parsed (OpenAPI) for $model/$device = $temp")
            temp
        } else {
            val resp = service.getDeviceState(device = device, model = model)
            if (!resp.isSuccessful) return@withContext null
            val properties = resp.body()?.data?.properties ?: emptyList()
            properties.firstNotNullOfOrNull { it.temperature }
        }
    }

    suspend fun readInside(): Double? = readTemperature(BuildConfig.GOVEE_INSIDE_DEVICE, BuildConfig.GOVEE_INSIDE_MODEL)
    suspend fun readTanks(): Double? = readTemperature(BuildConfig.GOVEE_TANKS_DEVICE, BuildConfig.GOVEE_TANKS_MODEL)
    suspend fun readWater(): Double? = readTemperature(BuildConfig.GOVEE_WATER_DEVICE, BuildConfig.GOVEE_WATER_MODEL)

    suspend fun readHumidity(device: String, model: String): Double? = withContext(Dispatchers.IO) {
        if (device.isBlank() || model.isBlank()) return@withContext null
        if (model.equals("H5100", ignoreCase = true)) {
            val req = OpenDeviceStateRequest(
                requestId = java.util.UUID.randomUUID().toString(),
                payload = OpenDeviceStatePayload(sku = model, device = device)
            )
            val resp = openService.getDeviceState(req)
            if (!resp.isSuccessful) return@withContext null
            val caps = resp.body()?.payload?.capabilities ?: emptyList()
            val hum = findCapabilityValue(caps, "humidity")
            Log.d("SensorRepository", "Humidity parsed (OpenAPI) for $model/$device = $hum")
            hum
        } else {
            val resp = service.getDeviceState(device = device, model = model)
            if (!resp.isSuccessful) return@withContext null
            val properties = resp.body()?.data?.properties ?: emptyList()
            properties.firstNotNullOfOrNull { it.humidity?.toDouble() }
        }
    }

    suspend fun readInsideHumidity(): Double? = readHumidity(BuildConfig.GOVEE_INSIDE_DEVICE, BuildConfig.GOVEE_INSIDE_MODEL)
    suspend fun readTanksHumidity(): Double? = readHumidity(BuildConfig.GOVEE_TANKS_DEVICE, BuildConfig.GOVEE_TANKS_MODEL)
    suspend fun readWaterHumidity(): Double? = readHumidity(BuildConfig.GOVEE_WATER_DEVICE, BuildConfig.GOVEE_WATER_MODEL)

    // Combined read to reduce request count
    suspend fun readTempHumidity(device: String, model: String): Pair<Double?, Double?> = withContext(Dispatchers.IO) {
        if (device.isBlank() || model.isBlank()) return@withContext Pair(null, null)
        if (model.equals("H5100", ignoreCase = true)) {
            val req = OpenDeviceStateRequest(
                requestId = java.util.UUID.randomUUID().toString(),
                payload = OpenDeviceStatePayload(sku = model, device = device)
            )
            val resp = openService.getDeviceState(req)
            if (!resp.isSuccessful) return@withContext Pair(null, null)
            val caps = resp.body()?.payload?.capabilities ?: emptyList()
            val temp = findCapabilityValue(caps, "temperature")
            val hum = findCapabilityValue(caps, "humidity")
            Log.d("SensorRepository", "Temp/Humidity parsed (OpenAPI) for $model/$device = $temp / $hum")
            Pair(temp, hum)
        } else {
            val resp = service.getDeviceState(device = device, model = model)
            if (!resp.isSuccessful) return@withContext Pair(null, null)
            val props = resp.body()?.data?.properties ?: emptyList()
            val temp = props.firstNotNullOfOrNull { it.temperature }
            val hum = props.firstNotNullOfOrNull { it.humidity?.toDouble() }
            Pair(temp, hum)
        }
    }

    suspend fun listDevices(): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val resp = service.getDevices()
        if (!resp.isSuccessful) return@withContext emptyList<DeviceInfo>()
        resp.body()?.data ?: emptyList()
    }
}
