package com.d3vk0.wardriving.rf.village.mx.core.repository

import android.util.Log
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.domain.resolvedUploadUrl
import com.d3vk0.wardriving.rf.village.mx.core.local.PendingUploadEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDao
import com.d3vk0.wardriving.rf.village.mx.core.remote.UploadPartsFactory
import com.d3vk0.wardriving.rf.village.mx.core.remote.RemoteFileDto
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.JsonParseException
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.security.MessageDigest

class UploadRepository(
    private val api: WardrivingApiService,
    private val config: ApiConfig,
    private val dao: WardrivingDao,
    private val uploadPartsFactory: UploadPartsFactory = UploadPartsFactory(),
) {
    suspend fun enqueuePending(
        sessionId: String,
        uploadType: String,
        file: File,
        sampleCount: Int,
    ): Long {
        require(sampleCount > 0) { "Sin datos disponibles" }
        val existing = dao.getPendingUpload(sessionId, uploadType)
        return dao.upsertPendingUpload(
            PendingUploadEntity(
                id = existing?.id ?: 0,
                sessionId = sessionId,
                uploadType = uploadType,
                filePath = file.absolutePath,
                createdAt = System.currentTimeMillis(),
                sampleCount = sampleCount,
                retryCount = 0,
                lastError = null,
                remoteId = existing?.remoteId,
                remoteSource = existing?.remoteSource,
                remoteHashSha256 = existing?.remoteHashSha256,
                isProcessed = existing?.isProcessed,
                responseReceivedAt = existing?.responseReceivedAt,
                uploadedAt = null,
            ),
        )
    }

    suspend fun uploadPending(upload: PendingUploadEntity): Boolean {
        val file = File(upload.filePath)
        require(file.exists()) { "CSV file no longer exists: ${upload.filePath}" }
        val localHash = file.sha256()
        val parts = uploadPartsFactory.create(
            file = file,
            deviceSource = upload.uploadType,
        )
        Log.i(
            TAG,
            "Uploading files=[${file.name}] device_source=${upload.uploadType} to ${config.resolvedUploadUrl()}",
        )
        val response = runCatching {
            api.upload(
                path = config.uploadPath,
                files = parts.files,
                deviceSource = parts.deviceSource,
            )
        }.getOrElse { error ->
            throw error.toActionableUploadException()
        }
        val remoteFiles = response.body()
        val match = remoteFiles?.let { matchRemoteFile(localHash, it) }
        Log.i(
            TAG,
            "Upload response code=${response.code()} type=${upload.uploadType} local_hash=$localHash " +
                "received_hashes=${remoteFiles?.map { it.hashSha256 }} matched=${match != null} " +
                "is_procesed=${match?.isProcessed}",
        )
        val validatedMatch = validateUploadResponse(response, localHash)
        val isProcessed = requireNotNull(validatedMatch.isProcessed)
        dao.markUploadAccepted(
            id = upload.id,
            remoteId = validatedMatch.id,
            remoteSource = validatedMatch.source,
            remoteHashSha256 = requireNotNull(validatedMatch.hashSha256),
            isProcessed = isProcessed,
            receivedAt = System.currentTimeMillis(),
        )
        return isProcessed
    }

    suspend fun uploadAllPending(): List<UploadAttemptResult> {
        val pendingUploads = dao.getPendingUploads()
        if (pendingUploads.isEmpty()) {
            Log.i(TAG, "No pending uploads for ${config.resolvedUploadUrl()}")
            return emptyList()
        }

        val results = uploadRecords(pendingUploads)
        pendingUploads.map { it.sessionId }.distinct().forEach { sessionId ->
            refreshSessionUploaded(sessionId)
        }
        return results
    }

    suspend fun uploadSessionPending(sessionId: String): List<UploadAttemptResult> {
        val results = uploadRecords(dao.getPendingUploads(sessionId))
        refreshSessionUploaded(sessionId)
        return results
    }

    fun observeSessionUploadState(sessionId: String): Flow<SessionUploadState> =
        dao.observePendingUploads(sessionId).map { uploads ->
            sessionUploadState(uploads, config.wifiBleUploadType, config.lteUploadType)
        }

    private suspend fun uploadRecords(pendingUploads: List<PendingUploadEntity>): List<UploadAttemptResult> {
        return pendingUploads.map { upload ->
            runCatching { uploadPending(upload) }
                .fold(
                    onSuccess = { processed -> UploadAttemptResult.Success(upload.id, processed) },
                    onFailure = { error ->
                        val uploadError = error.toActionableUploadException()
                        Log.e(
                            TAG,
                            "Upload failed for pending_id=${upload.id} file=${upload.filePath} device_source=${upload.uploadType}",
                            uploadError,
                        )
                        dao.markUploadFailed(upload.id, uploadError.message ?: "Upload failed")
                        UploadAttemptResult.Failure(upload.id, uploadError.message ?: "Upload failed", uploadError.actionable)
                    },
                )
        }
    }

    private suspend fun refreshSessionUploaded(sessionId: String) {
        val uploads = dao.getSessionUploads(sessionId)
        dao.updateSessionUploaded(sessionId, uploads.isNotEmpty())
    }

    suspend fun deletePendingUpload(id: Long): Boolean {
        val upload = dao.getPendingUpload(id) ?: return false
        val deletedFile = runCatching { File(upload.filePath).takeIf { it.exists() }?.delete() ?: true }.getOrDefault(false)
        dao.deletePendingUpload(id)
        return deletedFile
    }

    private fun Throwable.toActionableUploadException(): UploadException {
        if (this is UploadException) return this
        if (this is HttpException) {
            return UploadException(toUserFacingMessage("Upload failed"), actionable = true, cause = this)
        }
        if (this is JsonParseException) return UploadException("Respuesta JSON inválida", actionable = true, cause = this)
        return UploadException(message ?: "Upload failed", actionable = false, cause = this)
    }

    private companion object {
        const val TAG = "UploadRepository"
    }
}

sealed class UploadAttemptResult {
    data class Success(val pendingUploadId: Long, val isProcessed: Boolean) : UploadAttemptResult()
    data class Failure(val pendingUploadId: Long, val message: String, val actionable: Boolean) : UploadAttemptResult()
}

class UploadException(message: String, val actionable: Boolean, cause: Throwable? = null) : Exception(message, cause)

data class SessionUploadState(
    val label: String,
    val canUpload: Boolean,
    val wifiBleLabel: String = "Sin datos",
    val lteLabel: String = "Sin datos",
)

fun SessionUploadState.isOnPlatform(): Boolean =
    label == "Procesando" || label == "Procesado"

fun sessionUploadState(
    uploads: List<PendingUploadEntity>,
    wifiBleType: String = "wifi",
    lteType: String = "lte",
): SessionUploadState {
    val global = when {
        uploads.isEmpty() -> SessionUploadState("No subido", true)
        uploads.any { it.isProcessed == false } -> SessionUploadState("Procesando", false)
        uploads.any { it.lastError != null } -> SessionUploadState("Error", true)
        uploads.all { it.isProcessed == true } -> SessionUploadState("Procesado", false)
        else -> SessionUploadState("No subido", true)
    }
    return global.copy(
        wifiBleLabel = uploads.firstOrNull { it.uploadType == wifiBleType }.uploadLabel(),
        lteLabel = uploads.firstOrNull { it.uploadType == lteType }.uploadLabel(),
    )
}

private fun PendingUploadEntity?.uploadLabel(): String = when {
    this == null -> "Sin datos"
    lastError != null -> "Error"
    isProcessed == true -> "Procesado"
    isProcessed == false -> "Procesando"
    else -> "Pendiente"
}

internal fun matchRemoteFile(localSha256: String, files: List<RemoteFileDto>): RemoteFileDto? =
    files.firstOrNull { it.hashSha256?.equals(localSha256, ignoreCase = true) == true }

internal fun validateUploadResponse(
    response: Response<List<RemoteFileDto>>,
    localSha256: String,
): RemoteFileDto {
    if (response.code() != 201) {
        val serverMessage = response.errorBody()?.let { body ->
            runCatching { extractServerMessage(body.string()) }.getOrNull()
        }
        val fallback = if (response.isSuccessful) "se esperaba HTTP 201" else response.message()
        throw UploadException(httpMessage(response.code(), serverMessage, fallback), actionable = true)
    }
    val files = response.body()
        ?: throw UploadException("HTTP 201: respuesta sin body", actionable = true)
    if (files.any { it.hashSha256.isNullOrBlank() }) {
        throw UploadException("HTTP 201: hash_sha256 ausente en la respuesta", actionable = true)
    }
    if (files.any { it.isProcessed == null }) {
        throw UploadException("HTTP 201: is_procesed ausente en la respuesta", actionable = true)
    }
    return matchRemoteFile(localSha256, files)
        ?: throw UploadException("HTTP 201: ningún hash_sha256 coincide con el archivo enviado", actionable = true)
}

internal fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
