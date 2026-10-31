package com.d3vk0.wardriving.rf.village.mx.core.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity

class WifiScanner(
    private val context: Context,
    private val wifiManager: WifiManager,
) {
    @Suppress("DEPRECATION")
    private val wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "wardriving-scan")
    @Volatile
    private var lastResults: List<ScanResult> = emptyList()
    @Volatile
    private var receiverRegistered = false

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lastResults = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
        }
    }

    @Synchronized
    fun acquire() {
        if (!wifiLock.isHeld) {
            runCatching { wifiLock.acquire() }
        }
        if (!receiverRegistered) {
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(scanReceiver, filter)
            }
            receiverRegistered = true
        }
    }

    @Synchronized
    fun release() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(scanReceiver) }
            receiverRegistered = false
        }
        if (wifiLock.isHeld) {
            runCatching { wifiLock.release() }
        }
    }

    @Suppress("DEPRECATION")
    fun scan(sessionId: String, location: GeoLocation?): List<WifiBleSampleEntity> {
        acquire()
        val now = System.currentTimeMillis()
        val started = runCatching { wifiManager.startScan() }.getOrDefault(false)
        val results = runCatching { wifiManager.scanResults }
            .getOrDefault(emptyList())
            .ifEmpty { lastResults }
        lastResults = results
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
