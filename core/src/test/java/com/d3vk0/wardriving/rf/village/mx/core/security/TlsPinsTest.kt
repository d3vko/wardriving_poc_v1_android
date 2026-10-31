package com.d3vk0.wardriving.rf.village.mx.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TlsPinsTest {
    @Test
    fun pinsVillageHostsAndKeepsBackupChain() {
        assertEquals(3, TlsPins.SPKI_SHA256_BASE64.size)
        assertTrue(TlsPins.isPinnedHost("wardriving-ctf.rf-village-mx.com"))
        assertTrue(TlsPins.isPinnedHost("error-handlers.rf-village-mx.com"))
        assertFalse(TlsPins.isPinnedHost("demotiles.maplibre.org"))
    }
}
