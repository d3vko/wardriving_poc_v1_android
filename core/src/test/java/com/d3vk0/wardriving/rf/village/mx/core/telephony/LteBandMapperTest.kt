package com.d3vk0.wardriving.rf.village.mx.core.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LteBandMapperTest {
    @Test
    fun mapsCommonMexicoAndNorthAmericaBands() {
        val mapper = LteBandMapper()
        assertEquals("4", mapper.bandFromEarfcn(2100))
        assertEquals("7", mapper.bandFromEarfcn(3100))
        assertEquals("28", mapper.bandFromEarfcn(9300))
        assertNull(mapper.bandFromEarfcn(99_999))
    }

    @Test
    fun derivesFrequenciesFromEarfcn() {
        val mapper = LteBandMapper()
        assertEquals(2620.0, mapper.downlinkMhzFromEarfcn(2750))
        assertEquals(2500.0, mapper.uplinkMhzFromEarfcn(2750))
        assertEquals(2115.0, mapper.downlinkMhzFromEarfcn(2000))
        assertEquals(1715.0, mapper.uplinkMhzFromEarfcn(2000))
        assertNull(mapper.downlinkMhzFromEarfcn(99_999))
        assertNull(mapper.uplinkMhzFromEarfcn(99_999))
    }
}
