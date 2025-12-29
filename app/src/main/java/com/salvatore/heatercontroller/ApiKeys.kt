package com.salvatore.heatercontroller

object ApiKeys {
    // Centralized accessor for API keys exposed via BuildConfig
    val main: String
        get() = BuildConfig.API_KEY
}
