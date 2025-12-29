package com.salvatore.heatercontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salvatore.heatercontroller.data.SensorRepository
import com.salvatore.heatercontroller.network.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SensorRepository()

    private val _inside = MutableStateFlow<Double?>(null)
    val inside: StateFlow<Double?> = _inside

    private val _insideHumidity = MutableStateFlow<Double?>(null)
    val insideHumidity: StateFlow<Double?> = _insideHumidity

    private val _tanks = MutableStateFlow<Double?>(null)
    val tanks: StateFlow<Double?> = _tanks

    private val _tanksHumidity = MutableStateFlow<Double?>(null)
    val tanksHumidity: StateFlow<Double?> = _tanksHumidity

    private val _water = MutableStateFlow<Double?>(null)
    val water: StateFlow<Double?> = _water

    private val _waterHumidity = MutableStateFlow<Double?>(null)
    val waterHumidity: StateFlow<Double?> = _waterHumidity

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    // Selected device-model bindings for sensor roles
    private val _insideDevice = MutableStateFlow<Pair<String?, String?>?>(null)
    val insideDevice: StateFlow<Pair<String?, String?>?> = _insideDevice

    private val _tanksDevice = MutableStateFlow<Pair<String?, String?>?>(null)
    val tanksDevice: StateFlow<Pair<String?, String?>?> = _tanksDevice

    private val _waterDevice = MutableStateFlow<Pair<String?, String?>?>(null)
    val waterDevice: StateFlow<Pair<String?, String?>?> = _waterDevice

    private val prefs = getApplication<Application>().getSharedPreferences("device_prefs", 0)

    init {
        // Load persisted selections
        _insideDevice.value = readPair("INSIDE_DEVICE", "INSIDE_MODEL")
        _tanksDevice.value = readPair("TANKS_DEVICE", "TANKS_MODEL")
        _waterDevice.value = readPair("WATER_DEVICE", "WATER_MODEL")

        // Periodically refresh sensor values (every 60s to reduce API usage)
        viewModelScope.launch {
            while (true) {
                refreshOnce()
                delay(60_000)
            }
        }
        // Initial load
        viewModelScope.launch { refreshOnce() }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            _devices.value = repo.listDevices()
        }
    }

    fun setInsideDevice(d: DeviceInfo) {
        _insideDevice.value = Pair(d.device, d.model)
        savePair("INSIDE_DEVICE", d.device ?: "", "INSIDE_MODEL", d.model ?: "")
    }

    fun setTanksDevice(d: DeviceInfo) {
        _tanksDevice.value = Pair(d.device, d.model)
        savePair("TANKS_DEVICE", d.device ?: "", "TANKS_MODEL", d.model ?: "")
    }

    fun setWaterDevice(d: DeviceInfo) {
        _waterDevice.value = Pair(d.device, d.model)
        savePair("WATER_DEVICE", d.device ?: "", "WATER_MODEL", d.model ?: "")
    }

    private fun readPair(devKey: String, modelKey: String): Pair<String, String>? {
        val dev = prefs.getString(devKey, null)
        val model = prefs.getString(modelKey, null)
        return if (!dev.isNullOrBlank() && !model.isNullOrBlank()) Pair(dev, model) else null
    }

    private fun savePair(devKey: String, dev: String, modelKey: String, model: String) {
        prefs.edit().putString(devKey, dev).putString(modelKey, model).apply()
    }

    private suspend fun refreshOnce() {
        val (i, ih) = _insideDevice.value?.let { sel ->
            val dev = sel.first
            val mod = sel.second
            if (!dev.isNullOrBlank() && !mod.isNullOrBlank()) repo.readTempHumidity(dev, mod) else Pair(null, null)
        } ?: repo.readTempHumidity(BuildConfig.GOVEE_INSIDE_DEVICE, BuildConfig.GOVEE_INSIDE_MODEL)
        val (t, th) = _tanksDevice.value?.let { sel ->
            val dev = sel.first
            val mod = sel.second
            if (!dev.isNullOrBlank() && !mod.isNullOrBlank()) repo.readTempHumidity(dev, mod) else Pair(null, null)
        } ?: repo.readTempHumidity(BuildConfig.GOVEE_TANKS_DEVICE, BuildConfig.GOVEE_TANKS_MODEL)
        val (w, wh) = _waterDevice.value?.let { sel ->
            val dev = sel.first
            val mod = sel.second
            if (!dev.isNullOrBlank() && !mod.isNullOrBlank()) repo.readTempHumidity(dev, mod) else Pair(null, null)
        } ?: repo.readTempHumidity(BuildConfig.GOVEE_WATER_DEVICE, BuildConfig.GOVEE_WATER_MODEL)
        _inside.value = i
        _insideHumidity.value = ih
        _tanks.value = t
        _tanksHumidity.value = th
        _water.value = w
        _waterHumidity.value = wh
        _lastUpdated.value = System.currentTimeMillis()
    }

    fun refreshNow() {
        viewModelScope.launch { refreshOnce() }
    }
}
