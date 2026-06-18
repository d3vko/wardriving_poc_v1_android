package com.d3vk0.wardriving.rf.village.mx.core.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface WardrivingApiService {
    @POST
    suspend fun login(@Url path: String, @Body request: AuthRequest): AuthResponse

    @POST
    suspend fun register(@Url path: String, @Body request: AuthRequest): AuthResponse

    @POST
    suspend fun recoverPassword(
        @Url path: String,
        @Body request: PasswordRecoveryRequest,
    ): Response<Unit>

    @Multipart
    @POST
    suspend fun upload(
        @Url path: String,
        @Part files: List<MultipartBody.Part>,
        @Part("device_source") deviceSource: RequestBody,
    ): UploadResponse
}
