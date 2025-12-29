package com.salvatore.heatercontroller.data

import com.salvatore.heatercontroller.ApiKeys
import com.salvatore.heatercontroller.BuildConfig
import com.salvatore.heatercontroller.network.ControlCommand
import com.salvatore.heatercontroller.network.ControlRequest
import com.salvatore.heatercontroller.network.GoveeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class OutputRepository {
    private val service by lazy { GoveeApi.create(ApiKeys.main) }

    private suspend fun sendPower(device: String, model: String, on: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (device.isBlank() || model.isBlank()) return@withContext false
        val body = ControlRequest(
            device = device,
            model = model,
            cmd = ControlCommand(name = "turn", value = if (on) "on" else "off")
        )
        val resp: Response<com.salvatore.heatercontroller.network.ControlResponse> = service.controlDevice(body)
        resp.isSuccessful
    }

    suspend fun setHeaterA(on: Boolean): Boolean = sendPower(BuildConfig.GOVEE_HEATER_A_DEVICE, BuildConfig.GOVEE_HEATER_A_MODEL, on)
    suspend fun setHeaterB(on: Boolean): Boolean = sendPower(BuildConfig.GOVEE_HEATER_B_DEVICE, BuildConfig.GOVEE_HEATER_B_MODEL, on)
    suspend fun setLamp(on: Boolean): Boolean = sendPower(BuildConfig.GOVEE_LAMP_DEVICE, BuildConfig.GOVEE_LAMP_MODEL, on)

    suspend fun getPowerState(device: String, model: String): Boolean? = withContext(Dispatchers.IO) {
        if (device.isBlank() || model.isBlank()) return@withContext null
        val resp: Response<com.salvatore.heatercontroller.network.DeviceStateResponse> = service.getDeviceState(device = device, model = model)
        if (!resp.isSuccessful) return@withContext null
        val props = resp.body()?.data?.properties ?: emptyList()
        val power = props.firstNotNullOfOrNull { it.powerState }
        when (power?.lowercase()) {
            "on" -> true
            "off" -> false
            else -> null
        }
    }
}
