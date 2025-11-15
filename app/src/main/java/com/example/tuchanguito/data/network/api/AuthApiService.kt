package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.data.network.model.AuthTokenDto
import com.example.tuchanguito.data.network.model.CredentialsDto
import com.example.tuchanguito.data.network.model.PasswordChangeDto
import com.example.tuchanguito.data.network.model.PasswordResetDto
import com.example.tuchanguito.data.network.model.RegistrationRequestDto
import com.example.tuchanguito.data.network.model.UserProfileDto
import com.example.tuchanguito.data.network.model.UserUpdateRequestDto
import com.example.tuchanguito.data.network.model.VerificationCodeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface AuthApiService {

    @POST("api/users/login")
    suspend fun login(@Body body: CredentialsDto): AuthTokenDto

    @POST("api/users/register")
    suspend fun register(@Body body: RegistrationRequestDto)

    @POST("api/users/verify-account")
    suspend fun verify(@Body body: VerificationCodeDto)

    @POST("api/users/change-password")
    suspend fun changePassword(@Body body: PasswordChangeDto)

    @POST("api/users/forgot-password")
    suspend fun recoverPassword(@Query("email") email: String)

    @POST("api/users/reset-password")
    suspend fun resetPassword(@Body body: PasswordResetDto)

    @GET("api/users/profile")
    suspend fun getProfile(): UserProfileDto

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body body: UserUpdateRequestDto): UserProfileDto

    @POST("api/users/send-verification")
    suspend fun sendVerification(@Query("email") email: String)
}
