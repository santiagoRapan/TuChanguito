package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/** NOTE: Paths are based on common conventions. If your backend differs, adjust here. */
interface AuthService {
    // User endpoints live under /api/users/* according to the provided API
    @POST("api/users/login")
    suspend fun login(@Body body: CredentialsDTO): AuthenticationTokenDTO

    @POST("api/users/register")
    suspend fun register(@Body body: RegistrationDataDTO): Unit

    @POST("api/users/verify-account")
    suspend fun verify(@Body body: VerificationCodeDTO): Unit

    @POST("api/users/change-password")
    suspend fun changePassword(@Body body: PasswordChangeDTO): Unit

    @POST("api/users/forgot-password")
    suspend fun recover(@Query("email") email: String): Unit

    @POST("api/users/reset-password")
    suspend fun reset(@Body body: PasswordResetDTO): Unit

    @GET("api/users/profile")
    suspend fun getProfile(): com.example.tuchanguito.network.dto.UserDTO

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body body: com.example.tuchanguito.network.dto.UserUpdateDTO): com.example.tuchanguito.network.dto.UserDTO

    @POST("api/users/send-verification")
    suspend fun sendVerification(@Query("email") email: String): Unit
}
