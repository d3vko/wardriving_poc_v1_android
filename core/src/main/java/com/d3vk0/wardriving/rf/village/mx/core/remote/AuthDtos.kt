package com.d3vk0.wardriving.rf.village.mx.core.remote

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

data class UploadResponse(
    val id: String? = null,
    val status: String? = null,
    val message: String? = null,
)
