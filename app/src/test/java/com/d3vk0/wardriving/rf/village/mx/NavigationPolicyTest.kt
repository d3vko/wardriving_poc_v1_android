package com.d3vk0.wardriving.rf.village.mx

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun peceraIsFieldStartWithoutRequiringLogin() {
        assertEquals("live", FIELD_START_ROUTE)
        assertEquals("live", AUTHENTICATED_START_ROUTE)
        assertEquals("Pecera", FIELD_NAVIGATION_ITEMS.first().second)
        assertEquals(
            listOf("live", "dashboard", "sessions", "export", "settings"),
            FIELD_NAVIGATION_ITEMS.map { it.first },
        )
    }
}
