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
}
