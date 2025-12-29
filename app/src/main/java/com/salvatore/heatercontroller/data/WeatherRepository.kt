package com.salvatore.heatercontroller.data

import com.salvatore.heatercontroller.BuildConfig
import com.salvatore.heatercontroller.network.WeatherApi
import com.salvatore.heatercontroller.network.OpenMeteoApi
import com.salvatore.heatercontroller.network.OpenMeteoGeocodingApi
import com.salvatore.heatercontroller.network.OpenMeteoGeocodingResponse
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {
    private val owmService = WeatherApi.create()
    private val meteoService = OpenMeteoApi.create()
    private val geocodeService = OpenMeteoGeocodingApi.create()

    suspend fun readOutside(): Pair<Double?, Double?> = withContext(Dispatchers.IO) {
        val key = BuildConfig.WEATHER_API_KEY
        val loc = BuildConfig.WEATHER_LOCATION
        // If location is missing, we can't fetch weather
        if (loc.isBlank()) return@withContext Pair(null, null)
        val result: Pair<Double?, Double?> = if (key.isBlank()) {
            // Fallback: free Open-Meteo (no key). Resolve coords then fetch.
            val coords = resolveCoordinates(loc)
            if (coords != null) {
                val resp = meteoService.currentWeather(coords.first, coords.second)
                val cur = resp.body()?.current
                Pair(cur?.temperature_2m, cur?.relative_humidity_2m)
            } else Pair(null, null)
        } else {
            val resp = parseLocationAndFetch(loc, key)
            val main = resp?.body()?.main
            val temp = main?.temp
            val hum = main?.humidity?.toDouble()
            Pair(temp, hum)
        }
        return@withContext result
    }

    suspend fun resolveLocationLabel(): String? = withContext(Dispatchers.IO) {
        val loc = BuildConfig.WEATHER_LOCATION
        if (loc.isBlank()) return@withContext null
        val tokens = loc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        // If lat,lon provided, just echo
        if (tokens.size == 2 && tokens[0].toDoubleOrNull() != null && tokens[1].toDoubleOrNull() != null) {
            return@withContext "${tokens[0]}, ${tokens[1]}"
        }
        val city = tokens.getOrNull(0) ?: return@withContext null
        val stateToken = tokens.getOrNull(1)
        val countryToken = tokens.getOrNull(2)
        val resp = geocodeService.search(city, count = 10)
        val results = resp.body()?.results ?: emptyList()
        if (results.isEmpty()) return@withContext city

        fun stateNameFromAbbrev(abbrev: String): String? {
            return when (abbrev.uppercase(Locale.getDefault())) {
                "IL" -> "Illinois"
                else -> null
            }
        }

        val expectedCountry = countryToken?.uppercase(Locale.getDefault())
        val expectedStateName = stateToken?.let { st -> stateNameFromAbbrev(st) ?: st }
        val best = results.firstOrNull { r ->
            val countryOk = expectedCountry?.let { ec -> r.country_code?.equals(ec, ignoreCase = true) } ?: true
            val stateOk = expectedStateName?.let { es ->
                val admin = r.admin1 ?: ""
                admin.equals(es, ignoreCase = true) || admin.contains(es, ignoreCase = true)
            } ?: true
            countryOk == true && stateOk
        } ?: results.first()

        val name = listOfNotNull(best.name, best.admin1, best.country_code).joinToString(", ")
        return@withContext name.ifBlank { city }
    }

    private suspend fun parseLocationAndFetch(loc: String, key: String): retrofit2.Response<com.salvatore.heatercontroller.network.WeatherResponse>? {
        // If loc looks like "lat,lon", use coords; otherwise treat as city query
        val parts = loc.split(",").map { it.trim() }
        return try {
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lon = parts[1].toDouble()
                owmService.weatherByCoords(lat, lon, key)
            } else {
                owmService.weatherByCity(loc, key)
            }
        } catch (_: Exception) {
            owmService.weatherByCity(loc, key)
        }
    }

    private suspend fun resolveCoordinates(loc: String): Pair<Double, Double>? {
        // If already lat,lon, return directly
        val tokens = loc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size == 2) {
            val lat = tokens[0].toDoubleOrNull()
            val lon = tokens[1].toDoubleOrNull()
            if (lat != null && lon != null) return Pair(lat, lon)
        }
        val city = tokens.getOrNull(0) ?: return null
        val stateToken = tokens.getOrNull(1)
        val countryToken = tokens.getOrNull(2)

        // Resolve more than one candidate, then filter by state/country when provided
        val resp = geocodeService.search(city, count = 10)
        val results = resp.body()?.results ?: emptyList()
        if (results.isEmpty()) return null

        fun stateNameFromAbbrev(abbrev: String): String? {
            return when (abbrev.uppercase(Locale.getDefault())) {
                "IL" -> "Illinois"
                else -> null
            }
        }

        val expectedCountry = countryToken?.uppercase(Locale.getDefault())
        val expectedStateName = stateToken?.let { st ->
            stateNameFromAbbrev(st) ?: st
        }

        val filtered = results.filter { r ->
            val countryOk = expectedCountry?.let { ec -> r.country_code?.equals(ec, ignoreCase = true) } ?: true
            val stateOk = expectedStateName?.let { es ->
                val admin = r.admin1 ?: ""
                admin.equals(es, ignoreCase = true) || admin.contains(es, ignoreCase = true)
            } ?: true
            countryOk == true && stateOk
        }

        val best = (filtered.firstOrNull() ?: results.first())
        val lat = best.latitude
        val lon = best.longitude
        return if (lat != null && lon != null) Pair(lat, lon) else null
    }
}
