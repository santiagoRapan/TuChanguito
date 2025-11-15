package com.example.tuchanguito.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.repository.AuthRepository
import com.example.tuchanguito.data.network.model.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    val rememberMe: StateFlow<Boolean> = repository.rememberMeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val pendingEmail: StateFlow<String?> = repository.pendingEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pendingPassword: StateFlow<String?> = repository.pendingPasswordFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _profile = MutableStateFlow<UserProfileDto?>(null)
    val profile: StateFlow<UserProfileDto?> = _profile

    fun refreshProfile() {
        viewModelScope.launch {
            repository
                .getProfile()
                .onSuccess { _profile.value = it }
        }
    }

    suspend fun login(email: String, password: String, remember: Boolean) =
        repository.login(email, password, remember)

    suspend fun register(email: String, password: String, displayName: String) =
        repository.register(email, password, displayName)

    suspend fun verify(code: String) = repository.verify(code)

    suspend fun resendVerification(email: String) = repository.resendVerification(email)

    suspend fun changePassword(current: String, new: String) =
        repository.changePassword(current, new)

    suspend fun recoverPassword(email: String) =
        repository.recoverPassword(email)

    suspend fun resetPassword(code: String, newPassword: String) =
        repository.resetPassword(code, newPassword)

    suspend fun validateCredentials(email: String, password: String) =
        repository.validateCredentials(email, password)

    suspend fun getProfile() =
        repository
            .getProfile()
            .onSuccess { _profile.value = it }

    suspend fun clearPendingCredentials() = repository.clearPendingCredentials()

    suspend fun setRememberMe(value: Boolean) = repository.setRememberMe(value)

    suspend fun storePendingCredentials(email: String, password: String) =
        repository.storePendingCredentials(email, password)

    suspend fun updateProfile(name: String?, surname: String?) =
        repository
            .updateProfile(name, surname)
            .onSuccess { _profile.value = it }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
