package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.remote.AuthRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.PasswordRecoveryRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.RegisterRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthTokenStorage
import com.d3vk0.wardriving.rf.village.mx.core.security.JwtClaims
import retrofit2.HttpException

class AuthRepository(
    private val api: WardrivingApiService,
    private val config: ApiConfig,
    private val tokenStore: AuthTokenStorage,
) {
    suspend fun login(identifier: String, password: String): String {
        val response = api.login(config.loginPath, authRequest(identifier, password))
        val token = requireNotNull(response.access) { "Login response did not contain an access token" }
        persistSession(token, response.username)
        return token
    }

    suspend fun register(username: String, email: String, password: String, passwordConfirm: String): String {
        val response = api.register(
            config.registerPath,
            RegisterRequest(
                username = username,
                email = email,
                password = password,
                password_confirm = passwordConfirm,
            ),
        )
        val token = requireNotNull(response.access) { "Register response did not contain an access token" }
        persistSession(token, response.username ?: username)
        return token
    }

    suspend fun recoverPassword(identifier: String) {
        val response = api.recoverPassword(
            config.passwordRecoveryPath,
            PasswordRecoveryRequest(username = identifier.takeUnless { it.contains("@") }, email = identifier.takeIf { it.contains("@") }),
        )
        if (!response.isSuccessful) throw HttpException(response)
    }

    fun importJwt(raw: String): String {
        val token = raw.trim()
        require(token.isNotBlank()) { "JWT vacío" }
        persistSession(token, null)
        return token
    }

    fun hasToken(): Boolean = !tokenStore.getToken().isNullOrBlank()

    fun username(): String? = tokenStore.getUsername()

    fun logout() = tokenStore.clear()

    private fun persistSession(token: String, fallbackUsername: String?) {
        tokenStore.saveToken(token)
        tokenStore.saveUsername(JwtClaims.username(token) ?: fallbackUsername?.takeIf { it.isNotBlank() })
    }

    private fun authRequest(identifier: String, password: String): AuthRequest {
        return AuthRequest(
            username = identifier.takeUnless { it.contains("@") },
            email = identifier.takeIf { it.contains("@") },
            password = password,
        )
    }
}
