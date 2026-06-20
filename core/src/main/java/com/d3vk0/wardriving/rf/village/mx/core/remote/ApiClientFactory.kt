package com.d3vk0.wardriving.rf.village.mx.core.remote

import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClientFactory(
    private val tokenProvider: () -> String?,
    private val onAuthenticationRejected: (Int) -> Unit = {},
) {
    fun create(config: ApiConfig): WardrivingApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                val request = requestBuilder.build()
                val response = chain.proceed(request)
                if (isAuthenticatedRejection(request.header("Authorization"), response.code)) {
                    onAuthenticationRejected(response.code)
                }
                response
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(config.baseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WardrivingApiService::class.java)
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}

internal fun isAuthenticatedRejection(authorization: String?, httpCode: Int): Boolean =
    authorization?.startsWith("Bearer ", ignoreCase = true) == true && httpCode in setOf(401, 403)
