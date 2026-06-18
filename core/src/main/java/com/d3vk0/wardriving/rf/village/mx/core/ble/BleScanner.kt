package com.d3vk0.wardriving.rf.village.mx.core.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BleScanner(private val bluetoothAdapter: BluetoothAdapter?) {
    suspend fun scan(
        sessionId: String,
        location: GeoLocation?,
        anonymizeBleName: Boolean,
        scanWindowMillis: Long = 5_000L,
    ): List<WifiBleSampleEntity> = suspendCancellableCoroutine { continuation ->
        val adapter = bluetoothAdapter
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null || !adapter.isEnabled) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val now = System.currentTimeMillis()
        val results = linkedMapOf<String, WifiBleSampleEntity>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device?.address ?: return
                results[address] = result.toEntity(sessionId, now, location, anonymizeBleName)
            }

            override fun onBatchScanResults(batchResults: MutableList<ScanResult>) {
                batchResults.forEach { result ->
                    val address = result.device?.address ?: return@forEach
                    results[address] = result.toEntity(sessionId, now, location, anonymizeBleName)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }

        val handler = Handler(Looper.getMainLooper())
        val stop = Runnable {
            runCatching { scanner.stopScan(callback) }
            if (continuation.isActive) continuation.resume(results.values.toList())
        }
        continuation.invokeOnCancellation {
            handler.removeCallbacks(stop)
            runCatching { scanner.stopScan(callback) }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { scanner.startScan(null, settings, callback) }
            .onFailure {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        handler.postDelayed(stop, scanWindowMillis)
    }

    private fun ScanResult.toEntity(
        sessionId: String,
        timestamp: Long,
        location: GeoLocation?,
        anonymizeBleName: Boolean,
    ): WifiBleSampleEntity {
        val name = scanRecord?.deviceName ?: device?.name.orEmpty()
        return WifiBleSampleEntity(
            sessionId = sessionId,
            timestamp = timestamp,
            type = "BLE",
            mac = device?.address.orEmpty(),
            ssid = if (anonymizeBleName) "" else name,
            authMode = "BLE",
            channel = "",
            rssi = rssi,
            latitude = location?.latitude,
            longitude = location?.longitude,
            altitudeMeters = location?.altitudeMeters,
            accuracyMeters = location?.accuracyMeters,
            rawPayload = scanRecord?.bytes?.joinToString("") { "%02X".format(it) },
        )
    }
}
