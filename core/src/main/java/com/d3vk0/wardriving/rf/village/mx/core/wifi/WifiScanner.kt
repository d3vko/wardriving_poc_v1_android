package com.d3vk0.wardriving.rf.village.mx.core.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity

class WifiScanner(private val wifiManager: WifiManager) {
    fun scan(sessionId: String, location: GeoLocation?): List<WifiBleSampleEntity> {
        val now = System.currentTimeMillis()
        val started = runCatching { wifiManager.startScan() }.getOrDefault(false)
        val results = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
        return results.mapNotNull { result ->
            val bssid = result.BSSID ?: return@mapNotNull null
            WifiBleSampleEntity(
                sessionId = sessionId,
                timestamp = now,
                type = "WIFI",
                mac = bssid,
                ssid = result.SSID.orEmpty(),
                authMode = result.capabilities.orEmpty(),
                channel = frequencyToChannel(result.frequency).toString(),
                rssi = result.level,
                latitude = location?.latitude,
                longitude = location?.longitude,
                altitudeMeters = location?.altitudeMeters,
                accuracyMeters = location?.accuracyMeters,
                rawPayload = "scanStarted=$started;frequency=${result.frequency}",
            )
        }
    }

    fun frequencyToChannel(frequencyMhz: Int): Int {
        return when {
            frequencyMhz == 2484 -> 14
            frequencyMhz in 2412..2472 -> ((frequencyMhz - 2412) / 5) + 1
            frequencyMhz in 5180..5885 -> ((frequencyMhz - 5000) / 5)
            frequencyMhz in 5955..7115 -> ((frequencyMhz - 5950) / 5)
            else -> 0
        }
    }
}
