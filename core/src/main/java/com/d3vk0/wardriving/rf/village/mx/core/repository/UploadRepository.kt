package com.d3vk0.wardriving.rf.village.mx.core.repository

import android.util.Log
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.domain.resolvedUploadUrl
import com.d3vk0.wardriving.rf.village.mx.core.local.PendingUploadEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDao
import com.d3vk0.wardriving.rf.village.mx.core.remote.UploadPartsFactory
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import retrofit2.HttpException
import java.io.File

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
        return dao.upsertPendingUpload(
            PendingUploadEntity(
                sessionId = sessionId,
                uploadType = uploadType,
                filePath = file.absolutePath,
                createdAt = System.currentTimeMillis(),
                sampleCount = sampleCount,
                retryCount = 0,
                lastError = null,
                alreadyUploaded = false,
                uploadedAt = null,
            ),
        )
    }

    suspend fun uploadPending(upload: PendingUploadEntity) {
        val file = File(upload.filePath)
        require(file.exists()) { "CSV file no longer exists: ${upload.filePath}" }
        val parts = uploadPartsFactory.create(
            file = file,
            deviceSource = upload.uploadType,
        )
        Log.i(
            TAG,
            "Uploading files=[${file.name}] device_source=${upload.uploadType} to ${config.resolvedUploadUrl()}",
        )
        runCatching {
            api.upload(
                path = config.uploadPath,
                files = parts.files,
                deviceSource = parts.deviceSource,
            )
        }.getOrElse { error ->
            throw error.toActionableUploadException()
        }
        dao.markUploadSucceeded(upload.id, System.currentTimeMillis())
    }

    suspend fun uploadAllPending(): List<UploadAttemptResult> {
        val pendingUploads = dao.getPendingUploads()
        if (pendingUploads.isEmpty()) {
            Log.i(TAG, "No pending uploads for ${config.resolvedUploadUrl()}")
            return emptyList()
        }

        return pendingUploads.map { upload ->
            runCatching { uploadPending(upload) }
                .fold(
                    onSuccess = { UploadAttemptResult.Success(upload.id) },
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

    suspend fun deletePendingUpload(id: Long): Boolean {
        val upload = dao.getPendingUpload(id) ?: return false
        val deletedFile = runCatching { File(upload.filePath).takeIf { it.exists() }?.delete() ?: true }.getOrDefault(false)
        dao.deletePendingUpload(id)
        return deletedFile
    }

    private fun Throwable.toActionableUploadException(): UploadException {
        if (this is UploadException) return this
        if (this is HttpException && code() in 400..499) {
            val label = when (code()) {
                400 -> "archivo rechazado"
                401 -> "token inválido"
                403 -> "sin permisos para subir"
                404 -> "endpoint no encontrado"
                else -> "error de solicitud"
            }
            return UploadException("Error ${code()}: $label", actionable = true, cause = this)
        }
        return UploadException(message ?: "Upload failed", actionable = false, cause = this)
    }

    private companion object {
        const val TAG = "UploadRepository"
    }
}

sealed class UploadAttemptResult {
    data class Success(val pendingUploadId: Long) : UploadAttemptResult()
    data class Failure(val pendingUploadId: Long, val message: String, val actionable: Boolean) : UploadAttemptResult()
}

class UploadException(message: String, val actionable: Boolean, cause: Throwable? = null) : Exception(message, cause)
