package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LteCsvFormatterTest {
    @Test
    fun formatsEnglishHeaderInRequestedOrder() {
        val csv = LteCsvFormatter().format(
            listOf(
                LteSampleEntity(
                    sessionId = "s1",
                    timestamp = 1_768_941_212_000L,
                    technology = "LTE",
                    state = "1",
                    mcc = "334",
                    mnc = "20",
                    lac = 245,
                    cellId = 2705,
                    band = "7",
                    rssi = -68,
                    rsrp = -99,
                    rsrq = -12,
                    sinr = 10,
                    operator = "Telcel",
                    longitude = -99.0412598,
                    latitude = 19.5308285,
                    rawPayload = null,
                ),
            ),
        )

        assertTrue(csv.startsWith(LteCsvFormatter.HEADER))
        assertTrue(csv.contains("LTE,1,334,20,245,2705,7,-68,-99,-12,10,Telcel,-99.0412598,19.5308285"))
    }

    @Test
    fun writesUnavailableValuesAsEmptyFields() {
        val csv = LteCsvFormatter().format(
            listOf(
                LteSampleEntity(
                    sessionId = "s1",
                    timestamp = 0L,
                    technology = "LTE",
                    state = "0",
                    mcc = null,
                    mnc = null,
                    lac = null,
                    cellId = null,
                    band = null,
                    rssi = null,
                    rsrp = null,
                    rsrq = null,
                    sinr = null,
                    operator = null,
                    longitude = null,
                    latitude = null,
                    rawPayload = null,
                ),
            ),
        ).lines()[1]

        assertEquals("1970-01-01 00:00:00,LTE,0,,,,,,,,,,,,", csv)
    }
}
