package com.d3vk0.wardriving.rf.village.mx.core.remote

import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class UploadPartsFactoryTest {
    @Test
    fun createsFilesUploadPayloadWithExpectedFileNameAndDeviceSource() {
        val file = File.createTempFile("wifi_ble_session", ".csv").apply { writeText("a,b\n1,2\n") }
        val parts = UploadPartsFactory().create(
            file = file,
            deviceSource = "wifi_ble_android",
        )

        assertEquals(1, parts.files.size)
        assertEquals("form-data; name=\"files\"; filename=\"${file.name}\"", parts.files.first().headers?.get("Content-Disposition"))
        assertEquals("wifi_ble_android", parts.deviceSource.readUtf8())
        assertEquals(MultipartBody.FORM, MultipartBody.Builder().setType(MultipartBody.FORM).addPart(parts.files.first()).build().type)
    }

    private fun okhttp3.RequestBody.readUtf8(): String {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readUtf8()
    }
}
