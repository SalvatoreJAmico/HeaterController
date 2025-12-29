package com.salvatore.heatercontroller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salvatore.heatercontroller.data.OutputRepository
import com.salvatore.heatercontroller.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutputViewModel : ViewModel() {
    private val repo = OutputRepository()

    private val _heaterAOn = MutableStateFlow(false)
    val heaterAOn: StateFlow<Boolean> = _heaterAOn

    private val _heaterBOn = MutableStateFlow(false)
    val heaterBOn: StateFlow<Boolean> = _heaterBOn

    private val _lampOn = MutableStateFlow(false)
    val lampOn: StateFlow<Boolean> = _lampOn

    init {
        viewModelScope.launch {
            while (true) {
                refreshOnce()
                delay(60_000)
            }
        }
        viewModelScope.launch { refreshOnce() }
    }

    private suspend fun refreshOnce() {
        val ha = repo.getPowerState(BuildConfig.GOVEE_HEATER_A_DEVICE, BuildConfig.GOVEE_HEATER_A_MODEL)
        val hb = repo.getPowerState(BuildConfig.GOVEE_HEATER_B_DEVICE, BuildConfig.GOVEE_HEATER_B_MODEL)
        val lp = repo.getPowerState(BuildConfig.GOVEE_LAMP_DEVICE, BuildConfig.GOVEE_LAMP_MODEL)
        ha?.let { _heaterAOn.value = it }
        hb?.let { _heaterBOn.value = it }
        lp?.let { _lampOn.value = it }
    }

    fun setHeaterA(on: Boolean) {
        viewModelScope.launch {
            if (repo.setHeaterA(on)) {
                _heaterAOn.value = on
            }
        }
    }

    fun setHeaterB(on: Boolean) {
        viewModelScope.launch {
            if (repo.setHeaterB(on)) {
                _heaterBOn.value = on
            }
        }
    }

    fun setLamp(on: Boolean) {
        viewModelScope.launch {
            if (repo.setLamp(on)) {
                _lampOn.value = on
            }
        }
    }
}
