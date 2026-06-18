package com.d3vk0.wardriving.rf.village.mx.core.duplicate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFilterTest {
    @Test
    fun rejectsDuplicateInsideWindow() {
        val filter = DuplicateFilter(windowMillis = 20_000)
        assertTrue(filter.shouldKeep("wifi:aa", 1_000))
        assertFalse(filter.shouldKeep("wifi:aa", 5_000))
        assertTrue(filter.shouldKeep("wifi:aa", 25_000))
    }
}
