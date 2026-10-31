package com.d3vk0.wardriving.rf.village.mx.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtClaimsTest {
    @Test
    fun readsUsernameClaimFromAccessToken() {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"token_type":"access","username":"d3vnullv01d"}""".toByteArray())
        val token = "eyJhbGciOiJub25lIn0.$payload.sig"
        assertEquals("d3vnullv01d", JwtClaims.username(token))
    }

    @Test
    fun ignoresMalformedToken() {
        assertNull(JwtClaims.username("not-a-jwt"))
        assertNull(JwtClaims.username(""))
    }
}
