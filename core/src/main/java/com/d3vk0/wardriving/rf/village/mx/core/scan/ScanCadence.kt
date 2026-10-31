package com.d3vk0.wardriving.rf.village.mx.core.scan

/** Cadencia tipo Wigle Android: Wi-Fi ~2 s, BLE continuo, GPS 1 s. */
object ScanCadence {
    const val WIFI_PERIOD_MS = 2_000L
    const val DEDUPE_WINDOW_MS = 1_500L
    const val GPS_PERIOD_MS = 1_000L
    private const val LEGACY_SLOW_INTERVAL_MS = 10_000L

    fun effectiveWifiPeriod(stored: Long?): Long {
        val value = stored ?: WIFI_PERIOD_MS
        return if (value >= LEGACY_SLOW_INTERVAL_MS) WIFI_PERIOD_MS else value.coerceAtLeast(1_000L)
    }
}
