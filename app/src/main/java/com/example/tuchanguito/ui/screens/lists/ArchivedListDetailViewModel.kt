package com.example.tuchanguito.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.repository.ShoppingListHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Lightweight UI model that holds product info together with list item
data class ArchivedListItem(
    val id: Long,
    val listId: Long,
    val productId: Long,
    val productName: String,
    val categoryName: String?,
    val quantity: Double,
    val acquired: Boolean,
    val unit: String,
    val price: Double
)

data class ArchivedListUiState(
    val isLoading: Boolean = true,
    val list: ShoppingList? = null,
    val itemsByCategory: Map<String?, List<ArchivedListItem>> = emptyMap(),
    val errorMessage: String? = null
)

class ArchivedListDetailViewModel(
    private val listId: Long,
    private val historyRepository: ShoppingListHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivedListUiState())
    val uiState: StateFlow<ArchivedListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val list = historyRepository.getArchivedList(listId)
                val itemsEntities = historyRepository.getItemsForList(listId)
                val items = itemsEntities.map { it ->
                    val product: Product? = try { historyRepository.getProductById(it.productId) } catch (_: Throwable) { null }
                    val categoryName = product?.categoryId?.let { cid -> try { historyRepository.getCategoryName(cid) } catch (_: Throwable) { null } }
                    ArchivedListItem(
                        id = it.id,
                        listId = it.listId,
                        productId = it.productId,
                        productName = product?.name ?: "-",
                        categoryName = categoryName,
                        quantity = it.quantity.toDouble(),
                        acquired = it.acquired,
                        unit = product?.unit ?: "",
                        price = product?.price ?: 0.0
                    )
                }
                val grouped = items.groupBy { it.categoryName }
                _uiState.value = ArchivedListUiState(isLoading = false, list = list, itemsByCategory = grouped)
            } catch (t: Throwable) {
                _uiState.value = ArchivedListUiState(isLoading = false, errorMessage = t.message ?: "Error loading archived list")
            }
        }
    }
}

class ArchivedListDetailViewModelFactory(
    private val listId: Long,
    private val historyRepository: ShoppingListHistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchivedListDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchivedListDetailViewModel(listId, historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
