package com.d3vk0.wardriving.rf.village.mx.core.repository

import android.util.Log
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.domain.resolvedUploadUrl
import com.d3vk0.wardriving.rf.village.mx.core.local.PendingUploadEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDao
import com.d3vk0.wardriving.rf.village.mx.core.remote.UploadPartsFactory
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
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
        api.upload(
            path = config.uploadPath,
            files = parts.files,
            deviceSource = parts.deviceSource,
        )
        dao.markUploadSucceeded(upload.id, System.currentTimeMillis())
    }

    suspend fun uploadAllPending() {
        val pendingUploads = dao.getPendingUploads()
        if (pendingUploads.isEmpty()) {
            Log.i(TAG, "No pending uploads for ${config.resolvedUploadUrl()}")
            return
        }

        pendingUploads.forEach { upload ->
            runCatching { uploadPending(upload) }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "Upload failed for pending_id=${upload.id} file=${upload.filePath} device_source=${upload.uploadType}",
                        error,
                    )
                    dao.markUploadFailed(upload.id, error.message ?: "Upload failed")
                }
        }
    }

    private companion object {
        const val TAG = "UploadRepository"
    }
}
