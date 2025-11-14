package com.example.tuchanguito.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.data.repository.ShoppingListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeList: ShoppingListDto? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: ShoppingListsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching {
                repository.getLists(
                    owner = true,
                    sortBy = "updatedAt",
                    order = "DESC",
                    perPage = 1
                )
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { response ->
                        current.copy(
                            isLoading = false,
                            activeList = response.data.firstOrNull()
                        )
                    },
                    onFailure = { throwable ->
                        current.copy(
                            isLoading = false,
                            errorMessage = throwable.message
                        )
                    }
                )
            }
        }
    }
}

class HomeViewModelFactory(
    private val repository: ShoppingListsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
