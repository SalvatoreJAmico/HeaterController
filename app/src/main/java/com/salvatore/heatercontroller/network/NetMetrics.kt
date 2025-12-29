package com.salvatore.heatercontroller.network

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object NetMetrics {
    // Requests counted for the current server day (UTC-based from server 'Date' header)
    val requestsToday = MutableStateFlow(0)
    val serverDay = MutableStateFlow<LocalDate?>(null)
    val isConnected = MutableStateFlow(false)

    fun recordResponse(dateHeader: String?, success: Boolean) {
        // Parse RFC 1123 date header if present; else keep current day
        val headerDay: LocalDate? = try {
            if (dateHeader.isNullOrBlank()) null
            else ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate()
        } catch (_: Throwable) {
            null
        }

        val currentDay = serverDay.value
        val newDay = headerDay ?: currentDay ?: LocalDate.now(java.time.ZoneOffset.UTC)
        if (currentDay == null || newDay != currentDay) {
            serverDay.value = newDay
            requestsToday.value = 0
        }
        // Count this response toward the daily quota
        requestsToday.value = requestsToday.value + 1
        // Update connection state based on response success
        isConnected.value = success
    }
}
