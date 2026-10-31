package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LteCsvFormatterTest {
    @Test
    fun formatsHeaderAndSyntheticCoords() {
        val csv = LteCsvFormatter().format(listOf(sample()))

        assertTrue(csv.startsWith(LteCsvFormatter.HEADER))
        assertTrue(csv.contains("LTE,LTE,1,334,20,245,2705,10,145,123,7,2750,2620.0,2500.0,-68,-99,-12,10,Telcel,-9.8765432,1.2345678"))
    }

    @Test
    fun derivesBandAndFrequenciesFromKnownEarfcnWhenBandIsNull() {
        val fields = formatRow(sample(earfcn = 2750, band = null))

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
        val fields = formatRow(sample(earfcn = 99_999, band = null))

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
    fun writesZeroCoordsWhenGpsMissing() {
        val csv = LteCsvFormatter().format(listOf(sample(latitude = null, longitude = null)))
        assertTrue(csv.contains("0.0000000,0.0000000"))
    }

    @Test
    fun writesZeroCoordsWhenMccMncAreZero() {
        val csv = LteCsvFormatter().format(listOf(sample(mcc = "0", mnc = "0")))
        assertTrue(csv.contains("0.0000000,0.0000000"))
    }

    @Test
    fun writesZeroCoordsForCellIdSentinel() {
        val csv = LteCsvFormatter().format(
            listOf(sample(cellId = CsvExportCoords.CELL_ID_UNAVAILABLE_SENTINEL)),
        )
        assertTrue(csv.contains("0.0000000,0.0000000"))
    }

    @Test
    fun writesZeroCoordsForLacSentinel() {
        val csv = LteCsvFormatter().format(
            listOf(sample(lac = CsvExportCoords.LAC_UNAVAILABLE_SENTINEL)),
        )
        assertTrue(csv.contains("0.0000000,0.0000000"))
    }

    @Test
    fun clampsTimestampBelowSessionStartedAt() {
        val sessionStart = 1_710_000_000_000L
        val csv = LteCsvFormatter().format(
            listOf(sample(timestamp = 150_000_000L)),
            sessionStartedAt = sessionStart,
        )
        assertTrue(csv.contains(formatLocal(sessionStart)))
        assertFalse(csv.contains("1970-"))
    }

    @Test
    fun writesUnavailableIdentityAsEmptyFieldsAndZeroCoords() {
        val csv = LteCsvFormatter().format(
            listOf(
                sample(
                    timestamp = 0L,
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
                ),
            ),
        ).lines()[1]

        assertEquals("${formatLocal(0L)},LTE,LTE,0,,,,,,,,,,,,,,,,,0.0000000,0.0000000", csv)
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

        assertFalse(csv.contains("UNAVAILABLE"))
    }

    private fun formatRow(sample: LteSampleEntity): List<String> {
        return LteCsvFormatter().format(listOf(sample)).lines()[1].split(",")
    }

    private fun sample(
        timestamp: Long = 1_710_000_000_000L,
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
        longitude: Double? = -9.8765432,
        latitude: Double? = 1.2345678,
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

    private fun formatLocal(epochMs: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getDefault()
        return format.format(Date(epochMs))
    }
}
