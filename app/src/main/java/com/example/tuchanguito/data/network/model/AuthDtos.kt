package com.example.tuchanguito.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CredentialsDto(
    val email: String,
    val password: String
)

@Serializable
data class RegistrationRequestDto(
    val email: String,
    val name: String,
    val surname: String,
    val password: String,
    val metadata: JsonObject? = null
)

@Serializable
data class AuthTokenDto(
    val token: String
)

@Serializable
data class VerificationCodeDto(
    val code: String
)

@Serializable
data class PasswordChangeDto(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class PasswordResetDto(
    val code: String,
    val password: String
)

@Serializable
data class UserProfileDto(
    val id: Long,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    val metadata: JsonObject? = null
)

@Serializable
data class UserUpdateRequestDto(
    val name: String? = null,
    val surname: String? = null
)
