package com.example.tuchanguito.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.network.model.PurchaseDto
import com.example.tuchanguito.data.repository.ShoppingListHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListHistoryUiState(
    val isLoading: Boolean = true,
    val purchases: List<PurchaseDto> = emptyList(),
    val errorMessage: String? = null
)

class ListHistoryViewModel(
    private val repository: ShoppingListHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListHistoryUiState())
    val uiState: StateFlow<ListHistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { repository.getHistory(perPage = 100) }
            result.fold(
                onSuccess = { purchases ->
                    _uiState.value = ListHistoryUiState(
                        isLoading = false,
                        purchases = purchases,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No pudimos cargar el historial"
                        )
                    }
                }
            )
        }
    }
}

class ListHistoryViewModelFactory(
    private val repository: ShoppingListHistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
