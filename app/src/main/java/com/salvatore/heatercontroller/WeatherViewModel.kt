package com.salvatore.heatercontroller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvatore.heatercontroller.data.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repo = WeatherRepository()

    private val _outsideTemp = MutableStateFlow<Double?>(null)
    val outsideTemp: StateFlow<Double?> = _outsideTemp

    private val _outsideHumidity = MutableStateFlow<Double?>(null)
    val outsideHumidity: StateFlow<Double?> = _outsideHumidity

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    private val _outsideLabel = MutableStateFlow<String?>(null)
    val outsideLabel: StateFlow<String?> = _outsideLabel

    init {
        viewModelScope.launch {
            while (true) {
                refreshOnce()
                // Refresh every 15 minutes
                delay(15 * 60_000)
            }
        }
        viewModelScope.launch { refreshOnce() }
    }

    private suspend fun refreshOnce() {
        val (t, h) = repo.readOutside()
        _outsideTemp.value = t
        _outsideHumidity.value = h
        _outsideLabel.value = repo.resolveLocationLabel()
        _lastUpdated.value = System.currentTimeMillis()
    }
}
