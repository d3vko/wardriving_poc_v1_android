package com.d3vk0.wardriving.rf.village.mx.core.domain

import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFilterTest {
    @Test
    fun filtersByUploadedAndAlwaysSortsNewestFirst() {
        val sessions = listOf(
            session("old-processed", startedAt = 10, uploaded = true),
            session("new-unprocessed", startedAt = 30, uploaded = false),
            session("middle-processed", startedAt = 20, uploaded = true),
        )

        assertEquals(listOf("new-unprocessed", "middle-processed", "old-processed"), filterSessions(sessions, SessionFilter.ALL).map { it.id })
        assertEquals(listOf("middle-processed", "old-processed"), filterSessions(sessions, SessionFilter.PROCESSED).map { it.id })
        assertEquals(listOf("new-unprocessed"), filterSessions(sessions, SessionFilter.UNPROCESSED).map { it.id })
    }

    @Test
    fun serializesAndRestoresFromStorageValue() {
        SessionFilter.entries.forEach { filter ->
            assertEquals(filter, SessionFilter.fromStorageValue(filter.toStorageValue()))
        }
        assertEquals(SessionFilter.ALL, SessionFilter.fromStorageValue(null))
        assertEquals(SessionFilter.ALL, SessionFilter.fromStorageValue("invalid"))
    }

    private fun session(id: String, startedAt: Long, uploaded: Boolean) = WardrivingSessionEntity(
        id = id,
        startedAt = startedAt,
        endedAt = startedAt + 1,
        status = SessionStatus.STOPPED.name,
        wifiEnabled = true,
        bleEnabled = true,
        lteEnabled = true,
        deviceSource = "test",
        uploaded = uploaded,
        localExportPath = null,
        notes = null,
    )
}
