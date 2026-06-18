package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class WigleCsvFormatterTest {
    @Test
    fun formatsWifiAndBleRowsWithType() {
        val csv = WigleCsvFormatter().format(
            listOf(
                WifiBleSampleEntity(
                    sessionId = "s1",
                    timestamp = 0L,
                    type = "WIFI",
                    mac = "AA:BB:CC:DD:EE:FF",
                    ssid = "lab,ap",
                    authMode = "[WPA2-PSK-CCMP][ESS]",
                    channel = "6",
                    rssi = -45,
                    latitude = 19.0,
                    longitude = -99.0,
                    altitudeMeters = 2200.0,
                    accuracyMeters = 5f,
                    rawPayload = null,
                ),
                WifiBleSampleEntity(
                    sessionId = "s1",
                    timestamp = 0L,
                    type = "BLE",
                    mac = "11:22:33:44:55:66",
                    ssid = "",
                    authMode = "BLE",
                    channel = "",
                    rssi = -70,
                    latitude = 19.0,
                    longitude = -99.0,
                    altitudeMeters = null,
                    accuracyMeters = null,
                    rawPayload = null,
                ),
            ),
        )

        assertTrue(csv.startsWith(WigleCsvFormatter.HEADER))
        assertTrue(csv.contains("AA:BB:CC:DD:EE:FF,\"lab,ap\",[WPA2-PSK-CCMP][ESS],1970-01-01 00:00:00,6,-45,19.0,-99.0,2200.0,5.0,WIFI"))
        assertTrue(csv.contains("11:22:33:44:55:66,,BLE,1970-01-01 00:00:00,,-70,19.0,-99.0,,,BLE"))
    }
}
