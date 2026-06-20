package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.remote.AuthRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.PasswordRecoveryRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthTokenStorage
import retrofit2.HttpException

class AuthRepository(
    private val api: WardrivingApiService,
    private val config: ApiConfig,
    private val tokenStore: AuthTokenStorage,
) {
    suspend fun login(identifier: String, password: String): String {
        val response = api.login(config.loginPath, authRequest(identifier, password))
        val token = requireNotNull(response.access) { "Login response did not contain an access token" }
        tokenStore.saveToken(token)
        return token
    }

    suspend fun register(identifier: String, password: String): String {
        val response = api.register(config.registerPath, authRequest(identifier, password))
        val token = requireNotNull(response.access) { "Register response did not contain an access token" }
        tokenStore.saveToken(token)
        return token
    }

    suspend fun recoverPassword(identifier: String) {
        val response = api.recoverPassword(
            config.passwordRecoveryPath,
            PasswordRecoveryRequest(username = identifier.takeUnless { it.contains("@") }, email = identifier.takeIf { it.contains("@") }),
        )
        if (!response.isSuccessful) throw HttpException(response)
    }

    fun hasToken(): Boolean = !tokenStore.getToken().isNullOrBlank()

    fun logout() = tokenStore.clear()

    private fun authRequest(identifier: String, password: String): AuthRequest {
        return AuthRequest(
            username = identifier.takeUnless { it.contains("@") },
            email = identifier.takeIf { it.contains("@") },
            password = password,
        )
    }
}
