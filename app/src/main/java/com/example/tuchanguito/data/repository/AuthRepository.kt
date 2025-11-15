package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.data.network.AuthRemoteDataSource
import com.example.tuchanguito.data.network.model.UserProfileDto
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val remote: AuthRemoteDataSource,
    private val preferences: PreferencesManager
) {

    val rememberMeFlow: Flow<Boolean> = preferences.rememberMe
    val pendingEmailFlow: Flow<String?> = preferences.pendingEmail
    val pendingPasswordFlow: Flow<String?> = preferences.pendingPassword

    suspend fun register(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        val parts = displayName.trim().split(" ")
        val name = parts.firstOrNull().orEmpty().ifBlank { displayName }
        val surname = parts.drop(1).joinToString(" ")
        remote.register(email.trim(), name, surname, password)
        preferences.setPendingCredentials(email.trim(), password)
    }

    suspend fun verify(code: String): Result<Unit> = runCatching {
        remote.verify(code)
    }

    suspend fun login(email: String, password: String, remember: Boolean): Result<Unit> = runCatching {
        val token = remote.login(email, password).token
        preferences.setAuthToken(token)
        preferences.setRememberMe(remember)
    }

    suspend fun resendVerification(email: String): Result<Unit> = runCatching {
        remote.sendVerification(email.trim())
    }

    suspend fun changePassword(current: String, new: String): Result<Unit> = runCatching {
        remote.changePassword(current, new)
    }

    suspend fun validateCredentials(email: String, password: String): Result<Unit> = runCatching {
        remote.login(email, password)
    }

    suspend fun getProfile(): Result<UserProfileDto> = runCatching {
        remote.getProfile().also { preferences.setCurrentUserId(it.id) }
    }

    suspend fun updateProfile(name: String?, surname: String?): Result<UserProfileDto> = runCatching {
        remote.updateProfile(name, surname).also { preferences.setCurrentUserId(it.id) }
    }

    suspend fun clearPendingCredentials() = preferences.clearPendingCredentials()

    suspend fun setRememberMe(value: Boolean) = preferences.setRememberMe(value)

    suspend fun storePendingCredentials(email: String, password: String) =
        preferences.setPendingCredentials(email, password)
}
