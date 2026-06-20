package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.local.PendingUploadEntity
import com.d3vk0.wardriving.rf.village.mx.core.remote.RemoteFileDto
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class UploadContractTest {
    @Test
    fun parsesBackendArrayAndExactFieldNames() {
        val result = Gson().fromJson(
            """[{"id":"9","source":"renamed.csv","is_procesed":false,"hash_sha256":"abc123"}]""",
            Array<RemoteFileDto>::class.java,
        ).toList()

        assertEquals(1, result.size)
        assertEquals(false, result.single().isProcessed)
        assertEquals("abc123", result.single().hashSha256)
    }

    @Test
    fun missingIsProcessedRemainsNullAndCannotMasqueradeAsFalse() {
        val result = Gson().fromJson(
            """[{"id":"9","hash_sha256":"abc123"}]""",
            Array<RemoteFileDto>::class.java,
        ).single()

        assertEquals(null, result.isProcessed)
        assertThrows(UploadException::class.java) {
            validateUploadResponse(Response.success(201, listOf(result)), "abc123")
        }
    }

    @Test
    fun acceptsOnly201WithMatchingHashAndRequiredFields() {
        val wifi = RemoteFileDto("1", "wifi.csv", true, "wifi-hash")
        val lte = RemoteFileDto("2", "lte.csv", true, "lte-hash")

        assertEquals(wifi, validateUploadResponse(Response.success(201, listOf(wifi, lte)), "WIFI-HASH"))
        assertThrows(UploadException::class.java) {
            validateUploadResponse(Response.success(200, listOf(wifi)), "wifi-hash")
        }
        assertThrows(UploadException::class.java) {
            validateUploadResponse(Response.success<List<RemoteFileDto>>(201, null), "wifi-hash")
        }
        assertThrows(UploadException::class.java) {
            validateUploadResponse(Response.success(201, listOf(wifi)), "missing")
        }
    }

    @Test
    fun rejectsMalformedJsonAndMissingHash() {
        assertThrows(JsonSyntaxException::class.java) {
            Gson().fromJson("[{", Array<RemoteFileDto>::class.java)
        }
        val missingHash = RemoteFileDto("1", "wifi.csv", true, null)
        assertThrows(UploadException::class.java) {
            validateUploadResponse(Response.success(201, listOf(missingHash)), "wifi-hash")
        }
    }

    @Test
    fun includesHttpCodeAndServerMessageForExpectedFailures() {
        listOf(400, 401, 403, 404, 500).forEach { code ->
            val response = Response.error<List<RemoteFileDto>>(
                code,
                """{"message":"server detail"}""".toResponseBody("application/json".toMediaType()),
            )
            val error = assertThrows(UploadException::class.java) {
                validateUploadResponse(response, "wifi-hash")
            }
            assertEquals("HTTP $code: server detail", error.message)
        }
    }

    @Test
    fun matchesByShaEvenWhenBackendRenamesSource() {
        val remote = RemoteFileDto("1", "backend-name.csv", true, "AABB")

        assertNotNull(matchRemoteFile("aabb", listOf(remote)))
        assertEquals(null, matchRemoteFile("ccdd", listOf(remote)))
    }

    @Test
    fun derivesPersistentProcessingProcessedAndPartialErrorStates() {
        assertEquals("No subido", sessionUploadState(emptyList()).label)
        assertEquals("Procesando", sessionUploadState(listOf(upload(isProcessed = false))).label)
        assertFalse(sessionUploadState(listOf(upload(isProcessed = false))).canUpload)
        assertEquals("Procesado", sessionUploadState(listOf(upload(isProcessed = true))).label)
        assertFalse(sessionUploadState(listOf(upload(isProcessed = true))).canUpload)
        assertEquals("Procesando", sessionUploadState(listOf(upload(true), upload(false, type = "lte"))).label)
        val individual = sessionUploadState(listOf(upload(true), upload(false, type = "lte")))
        assertFalse(individual.canUpload)
        assertEquals("Procesado", individual.wifiBleLabel)
        assertEquals("Procesando", individual.lteLabel)
        assertEquals("Error", sessionUploadState(listOf(upload(true), upload(null, "HTTP 400", "lte"))).label)
        val processingWithError = sessionUploadState(listOf(upload(false), upload(null, "HTTP 500", "lte")))
        assertEquals("Procesando", processingWithError.label)
        assertFalse(processingWithError.canUpload)
    }

    @Test
    fun identifiesOnPlatformStates() {
        assertTrue(sessionUploadState(listOf(upload(isProcessed = false))).isOnPlatform())
        assertTrue(sessionUploadState(listOf(upload(isProcessed = true))).isOnPlatform())
        assertFalse(sessionUploadState(emptyList()).isOnPlatform())
        assertFalse(sessionUploadState(listOf(upload(null, "HTTP 400"))).isOnPlatform())
    }

    private fun upload(
        isProcessed: Boolean?,
        error: String? = null,
        type: String = "wifi",
    ) = PendingUploadEntity(
        id = 1,
        sessionId = "s1",
        uploadType = type,
        filePath = "/tmp/file.csv",
        createdAt = 1,
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
