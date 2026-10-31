package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WigleCsvFormatterTest {
    @Test
    fun formatsWifiAndBleRowsWithType() {
        val csv = WigleCsvFormatter().format(
            listOf(
                sample(
                    type = "WIFI",
                    mac = "AA:BB:CC:DD:EE:FF",
                    ssid = "lab,ap",
                    authMode = "[WPA2-PSK-CCMP][ESS]",
                    channel = "6",
                    rssi = -45,
                    latitude = 1.2345678,
                    longitude = -9.8765432,
                    altitudeMeters = 2200.0,
                    accuracyMeters = 5f,
                ),
                sample(
                    type = "BLE",
                    mac = "11:22:33:44:55:66",
                    ssid = "",
                    authMode = "BLE",
                    channel = "",
                    rssi = -70,
                    latitude = 1.2345678,
                    longitude = -9.8765432,
                    altitudeMeters = null,
                    accuracyMeters = null,
                ),
            ),
        )

        assertTrue(csv.startsWith(WigleCsvFormatter.HEADER))
        assertTrue(csv.contains("\"lab,ap\""))
        assertTrue(csv.contains("WIFI"))
        assertTrue(csv.contains("1.2345678"))
        assertTrue(csv.contains("-9.8765432"))
        assertTrue(
            csv.contains(
                "AA:BB:CC:DD:EE:FF,\"lab,ap\",[WPA2-PSK-CCMP][ESS],${formatLocal(0L)},6,-45,1.2345678,-9.8765432,2200.00,5.00,WIFI",
            ),
        )
        assertTrue(
            csv.contains("11:22:33:44:55:66,,BLE,${formatLocal(0L)},,-70,1.2345678,-9.8765432,,,BLE"),
        )
    }

    @Test
    fun writesZeroCoordsWhenGpsMissing() {
        val csv = WigleCsvFormatter().format(
            listOf(
                sample(
                    type = "BLE",
                    mac = "11:22:33:44:55:66",
                    ssid = "",
                    authMode = "BLE",
                    latitude = null,
                    longitude = null,
                ),
            ),
        )
        assertTrue(csv.contains("0.0000000,0.0000000"))
    }

    @Test
    fun writesZeroCoordsWhenMacEmpty() {
        val csv = WigleCsvFormatter().format(
            listOf(
                sample(
                    mac = "",
                    ssid = "x",
                    latitude = 1.2345678,
                    longitude = -9.8765432,
                ),
            ),
        )
        assertTrue(csv.contains("0.0000000,0.0000000"))
    }

    @Test
    fun writesInvalidDecimalValuesAsEmptyFields() {
        val csv = WigleCsvFormatter().format(
            listOf(
                sample(
                    latitude = 99.99999996,
                    longitude = -999.99999996,
                    altitudeMeters = Double.NaN,
                    accuracyMeters = Float.POSITIVE_INFINITY,
                ),
            ),
        ).lines()[1]

        assertEquals("AA:BB:CC:DD:EE:FF,lab,WPA2,${formatLocal(0L)},6,-45,,,,,WIFI", csv)
    }

    @Test
    fun firstSeenNeverPredatesSessionButPreservesValidSampleTime() {
        val startedAt = 1_000_000L
        val rows = WigleCsvFormatter().format(
            listOf(sample(timestamp = 500_000L, mac = "AA"), sample(timestamp = 2_000_000L, mac = "BB")),
            sessionStartedAt = startedAt,
        ).lines()

        assertTrue(rows[1].contains(formatLocal(startedAt)))
        assertTrue(rows[2].contains(formatLocal(2_000_000L)))
    }

    private fun sample(
        timestamp: Long = 0L,
        type: String = "WIFI",
        mac: String = "AA:BB:CC:DD:EE:FF",
        ssid: String = "lab",
        authMode: String = "WPA2",
        channel: String = "6",
        rssi: Int? = -45,
        latitude: Double? = 1.2345678,
        longitude: Double? = -9.8765432,
        altitudeMeters: Double? = null,
        accuracyMeters: Float? = null,
    ) = WifiBleSampleEntity(
        sessionId = "s1",
        timestamp = timestamp,
        type = type,
        mac = mac,
        ssid = ssid,
        authMode = authMode,
        channel = channel,
        rssi = rssi,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        accuracyMeters = accuracyMeters,
        rawPayload = null,
    )

    private fun formatLocal(epochMs: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getDefault()
        return format.format(Date(epochMs))
    }
}
