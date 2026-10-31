package com.d3vk0.wardriving.rf.village.mx.core.scan

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanCadenceTest {
    @Test
    fun migratesLegacyTwentySecondIntervalToWigleCadence() {
        assertEquals(2_000L, ScanCadence.effectiveWifiPeriod(null))
        assertEquals(2_000L, ScanCadence.effectiveWifiPeriod(20_000L))
        assertEquals(1_500L, ScanCadence.effectiveWifiPeriod(1_500L))
    }
}
