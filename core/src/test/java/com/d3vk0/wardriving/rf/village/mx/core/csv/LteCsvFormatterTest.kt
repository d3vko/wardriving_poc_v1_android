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
                    pci = 123,
                    earfcn = 2750,
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
        assertTrue(csv.contains("LTE,LTE,1,334,20,245,2705,10,145,123,7,2750,2620.0,2500.0,-68,-99,-12,10,Telcel,-99.0412598,19.5308285"))
    }

    @Test
    fun derivesBandAndFrequenciesFromKnownEarfcnWhenBandIsNull() {
        val fields = formatRow(
            sample(
                cellId = 2705,
                pci = 123,
                earfcn = 2750,
                band = null,
            ),
        )

        assertEquals("10", fields[8])
        assertEquals("145", fields[9])
        assertEquals("123", fields[10])
        assertEquals("7", fields[11])
        assertEquals("2750", fields[12])
        assertEquals("2620.0", fields[13])
        assertEquals("2500.0", fields[14])
    }

    @Test
    fun writesEmptyBandAndFrequenciesForUnmappedEarfcn() {
        val fields = formatRow(
            sample(
                pci = 123,
                earfcn = 99_999,
                band = null,
            ),
        )

        assertEquals("123", fields[10])
        assertEquals("", fields[11])
        assertEquals("99999", fields[12])
        assertEquals("", fields[13])
        assertEquals("", fields[14])
    }

    @Test
    fun writesEmptyEnodebAndSectorWhenCellIdIsNull() {
        val fields = formatRow(sample(cellId = null))

        assertEquals("", fields[7])
        assertEquals("", fields[8])
        assertEquals("", fields[9])
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
                    pci = null,
                    earfcn = null,
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

        assertEquals("1970-01-01 00:00:00,LTE,LTE,0,,,,,,,,,,,,,,,,,,", csv)
    }

    @Test
    fun outputDoesNotContainUnavailableSentinelText() {
        val csv = LteCsvFormatter().format(
            listOf(
                sample(
                    mcc = null,
                    mnc = null,
                    lac = null,
                    cellId = null,
                    pci = null,
                    earfcn = null,
                    band = null,
                    rssi = null,
                    rsrp = null,
                    rsrq = null,
                    sinr = null,
                ),
            ),
        )

        assertTrue(!csv.contains("UNAVAILABLE"))
    }

    @Test
    fun writesInvalidCoordinatesAsEmptyFields() {
        val csv = LteCsvFormatter().format(
            listOf(
                LteSampleEntity(
                    sessionId = "s1",
                    timestamp = 0L,
                    technology = "LTE",
                    state = "1",
                    mcc = "334",
                    mnc = "20",
                    lac = 245,
                    cellId = 2705,
                    pci = 123,
                    earfcn = 2750,
                    band = "7",
                    rssi = -68,
                    rsrp = -99,
                    rsrq = -12,
                    sinr = 10,
                    operator = "Telcel",
                    longitude = Double.NEGATIVE_INFINITY,
                    latitude = 99.99999996,
                    rawPayload = null,
                ),
            ),
        ).lines()[1]

        assertTrue(csv.endsWith("Telcel,,"))
    }

    private fun formatRow(sample: LteSampleEntity): List<String> {
        return LteCsvFormatter().format(listOf(sample)).lines()[1].split(",")
    }

    private fun sample(
        timestamp: Long = 0L,
        technology: String = "LTE",
        state: String = "1",
        mcc: String? = "334",
        mnc: String? = "20",
        lac: Int? = 245,
        cellId: Int? = 2705,
        pci: Int? = 123,
        earfcn: Int? = 2750,
        band: String? = "7",
        rssi: Int? = -68,
        rsrp: Int? = -99,
        rsrq: Int? = -12,
        sinr: Int? = 10,
        operator: String? = "Telcel",
        longitude: Double? = -99.0412598,
        latitude: Double? = 19.5308285,
    ) = LteSampleEntity(
        sessionId = "s1",
        timestamp = timestamp,
        technology = technology,
        state = state,
        mcc = mcc,
        mnc = mnc,
        lac = lac,
        cellId = cellId,
        pci = pci,
        earfcn = earfcn,
        band = band,
        rssi = rssi,
        rsrp = rsrp,
        rsrq = rsrq,
        sinr = sinr,
        operator = operator,
        longitude = longitude,
        latitude = latitude,
        rawPayload = null,
    )
}
