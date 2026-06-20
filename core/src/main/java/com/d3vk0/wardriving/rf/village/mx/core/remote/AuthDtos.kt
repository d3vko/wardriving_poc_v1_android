package com.d3vk0.wardriving.rf.village.mx.core.remote

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    val username: String? = null,
    val email: String? = null,
    val password: String,
)

data class PasswordRecoveryRequest(
    val username: String? = null,
    val email: String? = null,
)

data class AuthResponse(
    val refresh: String? = null,
    val access: String? = null,
    val username: String? = null,
)

data class RemoteFileDto(
    val id: String? = null,
    val source: String? = null,
    @SerializedName("is_procesed") val isProcessed: Boolean?,
    @SerializedName("hash_sha256") val hashSha256: String?,
)
