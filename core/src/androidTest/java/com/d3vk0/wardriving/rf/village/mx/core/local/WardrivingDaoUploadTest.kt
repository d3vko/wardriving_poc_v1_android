package com.d3vk0.wardriving.rf.village.mx.core.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WardrivingDaoUploadTest {
    private lateinit var database: WardrivingDatabase
    private lateinit var dao: WardrivingDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            WardrivingDatabase::class.java,
        ).build()
        dao = database.wardrivingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun processingFileBlocksEveryPendingUploadForThatSession() = runBlocking {
        dao.upsertPendingUpload(upload(1, "processing-session", "wifi", false))
        dao.upsertPendingUpload(upload(2, "processing-session", "lte", null, "HTTP 500"))
        dao.upsertPendingUpload(upload(3, "retryable-session", "wifi", null, "HTTP 500"))
        dao.upsertPendingUpload(upload(4, "never-sent-session", "wifi", null))

        assertTrue(dao.getPendingUploads("processing-session").isEmpty())
        assertEquals(
            listOf("retryable-session", "never-sent-session"),
            dao.getPendingUploads().map(PendingUploadEntity::sessionId),
        )
    }

    private fun upload(
        id: Long,
        sessionId: String,
        type: String,
        isProcessed: Boolean?,
        error: String? = null,
    ) = PendingUploadEntity(
        id = id,
        sessionId = sessionId,
        uploadType = type,
        filePath = "/tmp/$id.csv",
        createdAt = id,
        sampleCount = 1,
        retryCount = 0,
        lastError = error,
        remoteId = null,
        remoteSource = null,
        remoteHashSha256 = null,
        isProcessed = isProcessed,
        responseReceivedAt = null,
        uploadedAt = null,
    )
}
