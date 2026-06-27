package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.remote.AuthRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.AuthResponse
import com.d3vk0.wardriving.rf.village.mx.core.remote.PasswordRecoveryRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.RegisterRequest
import com.d3vk0.wardriving.rf.village.mx.core.remote.RemoteFileDto
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthTokenStorage
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthInvalidation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

        val token = repository.register("operator", "operator@example.test", "secret", "secret")

        assertEquals("registered-access", token)
        assertEquals("registered-access", tokenStore.savedToken)
        assertEquals(
            RegisterRequest(
                username = "operator",
                email = "operator@example.test",
                password = "secret",
                password_confirm = "secret",
            ),
            api.registerRequest,
        )
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
        override val invalidations: SharedFlow<AuthInvalidation> = MutableSharedFlow()

        override fun getToken(): String? = savedToken

        override fun saveToken(token: String) {
            savedToken = token
        }

        override fun clear() {
            savedToken = null
        }

        override fun invalidate(httpCode: Int) {
            clear()
        }
    }

    private class FakeAuthApi(
        private val loginResponse: AuthResponse = AuthResponse(access = "login-access"),
        private val registerResponse: AuthResponse = AuthResponse(access = "register-access"),
    ) : WardrivingApiService {
        var loginRequest: AuthRequest? = null
        var registerRequest: RegisterRequest? = null

        override suspend fun login(path: String, request: AuthRequest): AuthResponse {
            loginRequest = request
            return loginResponse
        }

        override suspend fun register(path: String, request: RegisterRequest): AuthResponse {
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
        ): retrofit2.Response<List<RemoteFileDto>> = retrofit2.Response.success(201, emptyList())
    }
}
