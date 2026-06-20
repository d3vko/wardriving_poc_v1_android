package com.d3vk0.wardriving.rf.village.mx

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun liveIsAuthenticatedStartAndFirstNavigationItem() {
        assertEquals("live", AUTHENTICATED_START_ROUTE)
        assertEquals(
            listOf("live", "dashboard", "sessions", "export", "settings"),
            FIELD_NAVIGATION_ITEMS.map { it.first },
        )
    }
}
