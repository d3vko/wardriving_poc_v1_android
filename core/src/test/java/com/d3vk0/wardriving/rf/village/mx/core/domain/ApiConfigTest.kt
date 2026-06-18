package com.d3vk0.wardriving.rf.village.mx.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {
    @Test
    fun resolvesUploadUrlFromApiBaseUrlAndRelativePath() {
        val config = ApiConfig(
            baseUrl = "https://www.wardriving.lat/wardriving/api/v1/",
            loginPath = "auth/login/",
            registerPath = "auth/register/",
            passwordRecoveryPath = "auth/password/reset/",
            uploadPath = "files-uploaded/",
            wifiBleUploadType = "wifi_ble_android",
            lteUploadType = "lte_android",
        )

        assertEquals(
            "https://www.wardriving.lat/wardriving/api/v1/files-uploaded/",
            config.resolvedUploadUrl(),
        )
    }

    @Test
    fun keepsBasePathWhenUploadPathStartsWithSlash() {
        val config = ApiConfig(
            baseUrl = "https://www.wardriving.lat/wardriving/api/v1",
            loginPath = "auth/login/",
            registerPath = "auth/register/",
            passwordRecoveryPath = "auth/password/reset/",
            uploadPath = "/files-uploaded/",
            wifiBleUploadType = "wifi_ble_android",
            lteUploadType = "lte_android",
        )

        assertEquals(
            "https://www.wardriving.lat/wardriving/api/v1/files-uploaded/",
            config.resolvedUploadUrl(),
        )
    }
}
