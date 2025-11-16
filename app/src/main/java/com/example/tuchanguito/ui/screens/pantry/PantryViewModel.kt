package com.example.tuchanguito.ui.screens.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.model.PantryItemDto
import com.example.tuchanguito.data.repository.CategoryRepository
import com.example.tuchanguito.data.repository.PantryRepository
import com.example.tuchanguito.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PantryUiState(
    val isLoading: Boolean = true,
    val items: List<PantryItemDto> = emptyList(),
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val chipCategories: List<CategoryChip> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null
)

data class CategoryChip(val id: Long, val name: String)

class PantryViewModel(
    private val pantryRepository: PantryRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadReferenceData()
            refreshItems()
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        refreshItems()
    }

    fun onCategorySelected(id: Long?) {
        _uiState.update { it.copy(selectedCategoryId = id) }
        refreshItems()
    }

    fun refreshItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val query = _uiState.value.query.takeIf { it.isNotBlank() }
            val categoryId = _uiState.value.selectedCategoryId
            val result = runCatching {
                pantryRepository.getItems(query, categoryId)
            }
            result.fold(
                onSuccess = { items ->
                    val chips = runCatching { pantryRepository.getCategoriesForQuery(query) }
                        .getOrDefault(emptyList())
                        .distinctBy { it.id }
                        .map { category -> CategoryChip(category.id, category.name) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            chipCategories = chips,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun addItem(
        productId: Long?,
        name: String,
        price: Double?,
        unit: String?,
        categoryName: String
    ) {
        val trimmedName = name.trim()
        val trimmedCategory = categoryName.trim()
        if (trimmedName.isEmpty() || trimmedCategory.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Todos los campos son obligatorios") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            val resolvedProductId = runCatching {
                productId ?: createProduct(
                    trimmedName,
                    trimmedCategory,
                    price,
                    unit,
                    DEFAULT_LOW_STOCK_THRESHOLD
                )
            }
            resolvedProductId.fold(
                onSuccess = { id ->
                    val result = runCatching {
                        pantryRepository.addOrIncrementItem(id, 1.0, unit?.takeIf { it.isNotBlank() } ?: "u")
                    }
                    result.fold(
                        onSuccess = {
                            refreshItems()
                            _uiState.update { state -> state.copy(errorMessage = null) }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(errorMessage = error.message) }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun incrementItem(itemId: Long, currentQuantity: Double, unit: String?) {
        viewModelScope.launch {
            val result = runCatching {
                pantryRepository.updateItemQuantity(itemId, currentQuantity + 1, unit ?: "u")
            }
            result.fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(items = state.items.map { if (it.id == updated.id) updated else it })
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun decrementItem(itemId: Long, currentQuantity: Double, unit: String?) {
        if (currentQuantity <= 1) return
        viewModelScope.launch {
            val result = runCatching {
                pantryRepository.updateItemQuantity(itemId, currentQuantity - 1, unit ?: "u")
            }
            result.fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(items = state.items.map { if (it.id == updated.id) updated else it })
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            val result = runCatching { pantryRepository.deleteItem(itemId) }
            result.fold(
                onSuccess = {
                    _uiState.update { state -> state.copy(items = state.items.filterNot { it.id == itemId }) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    private suspend fun createProduct(
        name: String,
        categoryName: String,
        price: Double?,
        unit: String?,
        lowStockThreshold: Int
    ): Long {
        val existingCategory = _uiState.value.categories.firstOrNull {
            it.name.equals(categoryName, ignoreCase = true)
        }
        val categoryId = existingCategory?.id ?: categoryRepository.createCategory(categoryName).id
        if (existingCategory == null) {
            _uiState.update { state ->
                state.copy(categories = state.categories + Category(id = categoryId, name = categoryName))
            }
        }
        val product = productRepository.createProduct(name, categoryId, price, unit, lowStockThreshold)
        _uiState.update { state ->
            state.copy(products = (state.products + product).distinctBy { it.id })
        }
        return product.id
    }

    private suspend fun loadReferenceData() {
        val productsResult = runCatching { productRepository.getProducts(perPage = 200) }
        val categoriesResult = runCatching { categoryRepository.getCategories(perPage = 200) }
        _uiState.update { state ->
            state.copy(
                products = productsResult.getOrDefault(emptyList()),
                categories = categoriesResult.getOrDefault(emptyList())
            )
        }
    }

    companion object {
        private const val DEFAULT_LOW_STOCK_THRESHOLD = 2
    }
}

class PantryViewModelFactory(
    private val pantryRepository: PantryRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PantryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PantryViewModel(pantryRepository, productRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
