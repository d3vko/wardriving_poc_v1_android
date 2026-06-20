package com.d3vk0.wardriving.rf.village.mx.core.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientFactoryTest {
    @Test
    fun onlyAuthenticated401And403RejectAuthentication() {
        assertTrue(isAuthenticatedRejection("Bearer jwt", 401))
        assertTrue(isAuthenticatedRejection("Bearer jwt", 403))

        listOf(400, 404, 500).forEach { code ->
            assertFalse(isAuthenticatedRejection("Bearer jwt", code))
        }
        assertFalse(isAuthenticatedRejection(null, 401))
        assertFalse(isAuthenticatedRejection("Basic credentials", 403))
    }
}
