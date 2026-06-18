package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPinType
import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPinCombinerTest {
    @Test
    fun `excludes records without coordinates`() {
        val pins = combineMapPins(
            wifiBle = listOf(
                wifiBle(id = 1, latitude = 19.1, longitude = -99.1),
                wifiBle(id = 2, latitude = null, longitude = -99.2),
                wifiBle(id = 3, latitude = 19.3, longitude = null),
            ),
            lte = listOf(
                lte(id = 1, latitude = null, longitude = -99.4),
                lte(id = 2, latitude = 19.5, longitude = -99.5),
            ),
        )

        assertEquals(2, pins.size)
        assertTrue(pins.all { it.latitude != 0.0 && it.longitude != 0.0 })
    }

    @Test
    fun `sorts mixed wifi ble and lte pins newest first`() {
        val pins = combineMapPins(
            wifiBle = listOf(
                wifiBle(id = 1, timestamp = 100, type = "WIFI"),
                wifiBle(id = 2, timestamp = 300, type = "BLE"),
            ),
            lte = listOf(lte(id = 1, timestamp = 200)),
        )

        assertEquals(listOf(300L, 200L, 100L), pins.map { it.timestamp })
        assertEquals(listOf(MapPinType.BLE, MapPinType.LTE, MapPinType.WIFI), pins.map { it.type })
    }

    @Test
    fun `caps combined pins at one hundred`() {
        val wifi = (1L..80L).map { wifiBle(id = it, timestamp = it) }
        val lte = (81L..140L).map { lte(id = it, timestamp = it) }

        val pins = combineMapPins(wifi, lte)

        assertEquals(100, pins.size)
        assertEquals(140L, pins.first().timestamp)
        assertEquals(41L, pins.last().timestamp)
    }

    private fun wifiBle(
        id: Long,
        timestamp: Long = id,
        type: String = "WIFI",
        latitude: Double? = 19.0 + id / 1000.0,
        longitude: Double? = -99.0 - id / 1000.0,
    ) = WifiBleSampleEntity(
        id = id,
        sessionId = "session-1",
        timestamp = timestamp,
        type = type,
        mac = "00:11:22:33:44:${id.toString().padStart(2, '0')}",
        ssid = if (type == "WIFI") "AP $id" else "",
        authMode = "WPA2",
        channel = "1",
        rssi = -50,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = null,
        accuracyMeters = null,
        rawPayload = null,
    )

    private fun lte(
        id: Long,
        timestamp: Long = id,
        latitude: Double? = 19.0 + id / 1000.0,
        longitude: Double? = -99.0 - id / 1000.0,
    ) = LteSampleEntity(
        id = id,
        sessionId = "session-1",
        timestamp = timestamp,
        technology = "LTE",
        state = "registered",
        mcc = "334",
        mnc = "020",
        lac = 10,
        cellId = id.toInt(),
        band = "B4",
        rssi = -70,
        rsrp = -95,
        rsrq = null,
        sinr = null,
        operator = "TestNet",
        longitude = longitude,
        latitude = latitude,
        rawPayload = null,
    )
}
