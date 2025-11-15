package com.example.tuchanguito.ui.screens.lists

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

data class ListHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<ShoppingListDto> = emptyList(),
    val errorMessage: String? = null
)

class ListHistoryViewModel(
    private val repository: ShoppingListsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListHistoryUiState())
    val uiState: StateFlow<ListHistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching {
                repository.getLists(owner = true, perPage = 100, sortBy = "updatedAt", order = "DESC").data
            }
            result.fold(
                onSuccess = { lists -> _uiState.update { it.copy(isLoading = false, items = lists) } },
                onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
            )
        }
    }
}

class ListHistoryViewModelFactory(
    private val repository: ShoppingListsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
