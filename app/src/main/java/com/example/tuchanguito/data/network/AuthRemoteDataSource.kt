package com.example.tuchanguito.data.network

import com.example.tuchanguito.data.network.api.AuthApiService
import com.example.tuchanguito.data.network.model.AuthTokenDto
import com.example.tuchanguito.data.network.model.PasswordChangeDto
import com.example.tuchanguito.data.network.model.PasswordResetDto
import com.example.tuchanguito.data.network.model.RegistrationRequestDto
import com.example.tuchanguito.data.network.model.UserProfileDto
import com.example.tuchanguito.data.network.model.UserUpdateRequestDto
import com.example.tuchanguito.data.network.model.VerificationCodeDto

class AuthRemoteDataSource(
    private val api: AuthApiService
) {

    suspend fun login(email: String, password: String): AuthTokenDto =
        api.login(com.example.tuchanguito.data.network.model.CredentialsDto(email, password))

    suspend fun register(
        email: String,
        name: String,
        surname: String,
        password: String
    ) = api.register(RegistrationRequestDto(email, name, surname, password))

    suspend fun verify(code: String) = api.verify(VerificationCodeDto(code))

    suspend fun changePassword(current: String, new: String) =
        api.changePassword(PasswordChangeDto(currentPassword = current, newPassword = new))

    suspend fun recoverPassword(email: String) = api.recoverPassword(email)

    suspend fun resetPassword(code: String, password: String) =
        api.resetPassword(PasswordResetDto(code = code, password = password))

    suspend fun getProfile(): UserProfileDto = api.getProfile()

    suspend fun updateProfile(name: String?, surname: String?): UserProfileDto =
        api.updateProfile(UserUpdateRequestDto(name, surname))

    suspend fun sendVerification(email: String) = api.sendVerification(email)
}
