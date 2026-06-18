package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.remote.AuthRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.AuthResponse
import com.d3vk0.wardriving.rf.village.mx.core.remote.PasswordRecoveryRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.UploadResponse
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthTokenStorage
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {
    private val config = ApiConfig(
        baseUrl = "https://api.example.test/",
        loginPath = "auth/login/",
        registerPath = "auth/register/",
        passwordRecoveryPath = "auth/recover/",
        uploadPath = "upload/",
        wifiBleUploadType = "wifi_ble",
        lteUploadType = "lte",
    )

    @Test
    fun loginPersistsAccessToken() = runTest {
        val api = FakeAuthApi(loginResponse = AuthResponse(refresh = "refresh-jwt", access = "access-jwt", username = "operator"))
        val tokenStore = FakeTokenStore()
        val repository = AuthRepository(api, config, tokenStore)

        val token = repository.login("operator", "secret")

        assertEquals("access-jwt", token)
        assertEquals("access-jwt", tokenStore.savedToken)
        assertEquals(AuthRequest(username = "operator", password = "secret"), api.loginRequest)
    }

    @Test
    fun registerPersistsAccessToken() = runTest {
        val api = FakeAuthApi(registerResponse = AuthResponse(refresh = "refresh-jwt", access = "registered-access", username = "operator"))
        val tokenStore = FakeTokenStore()
        val repository = AuthRepository(api, config, tokenStore)

        val token = repository.register("operator@example.test", "secret")

        assertEquals("registered-access", token)
        assertEquals("registered-access", tokenStore.savedToken)
        assertEquals(AuthRequest(email = "operator@example.test", password = "secret"), api.registerRequest)
    }

    @Test
    fun loginWithoutAccessTokenFailsClearlyAndDoesNotPersistToken() = runTest {
        val api = FakeAuthApi(loginResponse = AuthResponse(refresh = "refresh-jwt", access = null, username = "operator"))
        val tokenStore = FakeTokenStore()
        val repository = AuthRepository(api, config, tokenStore)

        try {
            repository.login("operator", "secret")
            fail("Expected login to fail without an access token")
        } catch (error: IllegalArgumentException) {
            assertEquals("Login response did not contain an access token", error.message)
        }
        assertNull(tokenStore.savedToken)
    }

    private class FakeTokenStore : AuthTokenStorage {
        var savedToken: String? = null

        override fun getToken(): String? = savedToken

        override fun saveToken(token: String) {
            savedToken = token
        }

        override fun clear() {
            savedToken = null
        }
    }

    private class FakeAuthApi(
        private val loginResponse: AuthResponse = AuthResponse(access = "login-access"),
        private val registerResponse: AuthResponse = AuthResponse(access = "register-access"),
    ) : WardrivingApiService {
        var loginRequest: AuthRequest? = null
        var registerRequest: AuthRequest? = null

        override suspend fun login(path: String, request: AuthRequest): AuthResponse {
            loginRequest = request
            return loginResponse
        }

        override suspend fun register(path: String, request: AuthRequest): AuthResponse {
            registerRequest = request
            return registerResponse
        }

        override suspend fun recoverPassword(path: String, request: PasswordRecoveryRequest): Response<Unit> {
            return Response.success(Unit)
        }

        override suspend fun upload(
            path: String,
            files: List<MultipartBody.Part>,
            deviceSource: RequestBody,
        ): UploadResponse = UploadResponse()
    }
}
