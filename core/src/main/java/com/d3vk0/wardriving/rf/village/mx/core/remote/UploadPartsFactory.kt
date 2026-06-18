package com.d3vk0.wardriving.rf.village.mx.core.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class UploadParts(
    val files: List<MultipartBody.Part>,
    val deviceSource: RequestBody,
)

class UploadPartsFactory {
    fun create(
        file: File,
        deviceSource: String,
    ): UploadParts {
        val csv = "text/csv".toMediaType()
        val text = "text/plain".toMediaType()
        return UploadParts(
            files = listOf(MultipartBody.Part.createFormData("files", file.name, file.asRequestBody(csv))),
            deviceSource = deviceSource.toRequestBody(text),
        )
    }
}
