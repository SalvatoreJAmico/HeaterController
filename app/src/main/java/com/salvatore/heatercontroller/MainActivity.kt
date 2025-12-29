package com.salvatore.heatercontroller

import android.os.Bundle
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
// Button removed: using auto-refresh countdown instead
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import com.salvatore.heatercontroller.ui.theme.HeaterControllerTheme
import com.salvatore.heatercontroller.data.OutputRepository
import com.salvatore.heatercontroller.network.NetMetrics
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HeaterControllerTheme {
                AppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Heater Controller") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        HeaterControlScreen(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun HeaterControlScreen(modifier: Modifier = Modifier, vm: SensorViewModel = viewModel(), ovm: OutputViewModel = viewModel(), wvm: WeatherViewModel = viewModel()) {
    // Three temp sensors (mock inputs for now)
    val inside by vm.inside.collectAsState()
    val insideHumidity by vm.insideHumidity.collectAsState()
    val tanksHumidity by vm.tanksHumidity.collectAsState()
    val waterHumidity by vm.waterHumidity.collectAsState()
    val tanks by vm.tanks.collectAsState()
    val water by vm.water.collectAsState()
    val lastUpdatedMs by vm.lastUpdated.collectAsState()
    val outsideTemp by wvm.outsideTemp.collectAsState()
    val outsideHumidity by wvm.outsideHumidity.collectAsState()
    val outsideLabel by wvm.outsideLabel.collectAsState()
    val weatherUpdatedMs by wvm.lastUpdated.collectAsState()

    // Target ranges and mode (persisted)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("heater_prefs", Context.MODE_PRIVATE) }
    var target1Start by rememberSaveable { mutableStateOf(prefs.getFloat("heaterA_start", 40f)) }
    var target1End by rememberSaveable { mutableStateOf(prefs.getFloat("heaterA_end", 60f)) }
    // Second Tanks range
    var target1bStart by rememberSaveable { mutableStateOf(prefs.getFloat("heaterB_start", 55f)) }
    var target1bEnd by rememberSaveable { mutableStateOf(prefs.getFloat("heaterB_end", 75f)) }
    var target2Start by rememberSaveable { mutableStateOf(prefs.getFloat("lamp_start", 55f)) }
    var target2End by rememberSaveable { mutableStateOf(prefs.getFloat("lamp_end", 75f)) }
    var isAutoMode by rememberSaveable { mutableStateOf(false) }

    // Collapsible sections for sliders
    var heaterAExpanded by rememberSaveable { mutableStateOf(true) }
    var heaterBExpanded by rememberSaveable { mutableStateOf(true) }
    var lampExpanded by rememberSaveable { mutableStateOf(true) }

    // Outputs: two heaters + one heat lamp
    val heaterAOn by ovm.heaterAOn.collectAsState()
    val heaterBOn by ovm.heaterBOn.collectAsState()
    val heatLampOn by ovm.lampOn.collectAsState()

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sensors: read-only label + value
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sensors", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Inside")
                    Text(text = formatTempHumidity(inside, insideHumidity))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tanks")
                    Text(text = formatTempHumidity(tanks, tanksHumidity))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Water")
                    Text(text = formatTempHumidity(water, waterHumidity))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val outsideTitle = if (!outsideLabel.isNullOrBlank()) {
                        "Outside (${outsideLabel})"
                    } else {
                        "Outside"
                    }
                    Text(outsideTitle)
                    Text(text = formatTempHumidity(outsideTemp, outsideHumidity))
                }
                val outsideUpdatedText = formatUpdated(weatherUpdatedMs)
                if (outsideUpdatedText.isNotEmpty()) {
                    Text(outsideUpdatedText, style = MaterialTheme.typography.bodySmall)
                }
                val updatedText = formatUpdated(lastUpdatedMs)
                if (updatedText.isNotEmpty()) {
                    Text(updatedText, style = MaterialTheme.typography.bodySmall)
                }
                var nextUpdateIn by remember { mutableStateOf<Int?>(null) }
                LaunchedEffect(Unit) {
                    while (true) {
                        val ms = lastUpdatedMs
                        if (ms != null) {
                            val remaining = 60_000 - (System.currentTimeMillis() - ms)
                            nextUpdateIn = if (remaining > 0) (remaining / 1000).toInt() else 0
                        } else {
                            nextUpdateIn = null
                        }
                        delay(1000)
                    }
                }
                val nextText = nextUpdateIn?.let { "Next update in: ${it}s" } ?: ""
                if (nextText.isNotEmpty()) {
                    Text(nextText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Target temperature ranges
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Heater A: ${target1Start.toInt()}–${target1End.toInt()}°F",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { heaterAExpanded = !heaterAExpanded }) {
                        Text(if (heaterAExpanded) "Hide" else "Show")
                    }
                }
                if (heaterAExpanded) {
                    RangeSlider(
                        value = target1Start..target1End,
                        onValueChange = { range ->
                            target1Start = range.start
                            target1End = range.endInclusive
                            prefs.edit()
                                .putFloat("heaterA_start", target1Start)
                                .putFloat("heaterA_end", target1End)
                                .apply()
                        },
                        valueRange = -30f..120f,
                        steps = 150
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Heater B: ${target1bStart.toInt()}–${target1bEnd.toInt()}°F",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { heaterBExpanded = !heaterBExpanded }) {
                        Text(if (heaterBExpanded) "Hide" else "Show")
                    }
                }
                if (heaterBExpanded) {
                    RangeSlider(
                        value = target1bStart..target1bEnd,
                        onValueChange = { range ->
                            target1bStart = range.start
                            target1bEnd = range.endInclusive
                            prefs.edit()
                                .putFloat("heaterB_start", target1bStart)
                                .putFloat("heaterB_end", target1bEnd)
                                .apply()
                        },
                        valueRange = -30f..120f,
                        steps = 150
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Heat Lamp: ${target2Start.toInt()}–${target2End.toInt()}°F",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { lampExpanded = !lampExpanded }) {
                        Text(if (lampExpanded) "Hide" else "Show")
                    }
                }
                if (lampExpanded) {
                    RangeSlider(
                        value = target2Start..target2End,
                        onValueChange = { range ->
                            target2Start = range.start
                            target2End = range.endInclusive
                            prefs.edit()
                                .putFloat("lamp_start", target2Start)
                                .putFloat("lamp_end", target2End)
                                .apply()
                        },
                        valueRange = -30f..120f,
                        steps = 150
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isAutoMode, onCheckedChange = { isAutoMode = it })
                    Spacer(Modifier.width(12.dp))
                    Text(if (isAutoMode) "Mode: Auto" else "Mode: Manual")
                }
            }
        }


        // Outputs: Heater A, Heater B, Heat Lamp
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = heaterAOn, onCheckedChange = { checked ->
                        ovm.setHeaterA(checked)
                    })
                    Spacer(Modifier.width(12.dp))
                    Text("Heater A: " + if (heaterAOn) "ON" else "OFF")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = heaterBOn, onCheckedChange = { checked ->
                        ovm.setHeaterB(checked)
                    })
                    Spacer(Modifier.width(12.dp))
                    Text("Heater B: " + if (heaterBOn) "ON" else "OFF")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = heatLampOn, onCheckedChange = { checked ->
                        ovm.setLamp(checked)
                    })
                    Spacer(Modifier.width(12.dp))
                    Text("Heat Lamp: " + if (heatLampOn) "ON" else "OFF")
                }
            }
        }
        // Auto mode control logic
        LaunchedEffect(
            isAutoMode,
            tanks,
            water,
            target1Start,
            target1End,
            target1bStart,
            target1bEnd,
            target2Start,
            target2End
        ) {
            if (!isAutoMode) return@LaunchedEffect

            suspend fun decide(current: Double?, lower: Float, upper: Float, previous: Boolean): Boolean {
                if (current == null) return previous
                val h = 0.5 // °F hysteresis
                return when {
                    current <= lower - h -> true
                    current >= upper + h -> false
                    else -> previous
                }
            }

            // Heaters use Tanks sensor
            val desiredA = decide(tanks, target1Start, target1End, heaterAOn)
            if (desiredA != heaterAOn) {
                ovm.setHeaterA(desiredA)
            }

            val desiredB = decide(tanks, target1bStart, target1bEnd, heaterBOn)
            if (desiredB != heaterBOn) {
                ovm.setHeaterB(desiredB)
            }

            // Lamp uses Water sensor
            val desiredLamp = decide(water, target2Start, target2End, heatLampOn)
            if (desiredLamp != heatLampOn) {
                ovm.setLamp(desiredLamp)
            }
        }
        // Footer: Request counter (server-day) and connection status
        val reqsToday by NetMetrics.requestsToday.collectAsState()
        val connected by NetMetrics.isConnected.collectAsState()
        val footer = buildString {
            append("Requests today: ")
            append(reqsToday)
            append(" · Connection: ")
            append(if (connected) "Online" else "Offline")
        }
        Text(footer, style = MaterialTheme.typography.bodySmall)
    }
}


private fun formatTemp(value: Double?): String =
    if (value == null) "—" else "${value.toInt()} °F"

private fun formatHumidity(value: Double?): String =
    if (value == null) "—" else "${value.toInt()} %"

private fun formatTempHumidity(temp: Double?, humidity: Double?): String =
    listOf(formatTemp(temp), formatHumidity(humidity)).joinToString(", ")

private fun formatUpdated(ms: Long?): String {
    if (ms == null) return ""
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return "Updated " + sdf.format(Date(ms))
}

@Preview(showBackground = true)
@Composable
private fun PreviewLight() {
    HeaterControllerTheme(darkTheme = false) {
        AppScaffold()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDark() {
    HeaterControllerTheme(darkTheme = true) {
        AppScaffold()
    }
}
