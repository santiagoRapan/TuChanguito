package com.example.tuchanguito.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.repository.CategoryRepository
import com.example.tuchanguito.data.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val items: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CategoriesViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState(isLoading = true))
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { repository.getCategories() }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(items = it, isLoading = false) },
                    onFailure = { state.copy(isLoading = false, errorMessage = it.message ?: "Error al cargar categorías") }
                )
            }
        }
    }

    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching {
                repository.createCategory(trimmed)
                repository.getCategories()
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(items = it, isLoading = false) },
                    onFailure = { state.copy(isLoading = false, errorMessage = it.message ?: "Error al crear la categoría") }
                )
            }
        }
    }
}

class CategoriesViewModelFactory(
    private val repository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
