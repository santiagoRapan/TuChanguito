package com.example.tuchanguito.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.network.model.ListItemDto
import com.example.tuchanguito.data.network.model.PurchaseDto
import com.example.tuchanguito.data.repository.ShoppingListHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Lightweight UI model that holds product info together with list item
data class ArchivedListItem(
    val id: Long,
    val productName: String,
    val categoryName: String?,
    val quantity: Double,
    val acquired: Boolean,
    val unit: String,
    val price: Double
)

data class ArchivedListUiState(
    val isLoading: Boolean = true,
    val purchase: PurchaseDto? = null,
    val itemsByCategory: Map<String?, List<ArchivedListItem>> = emptyMap(),
    val errorMessage: String? = null
)

class ArchivedListDetailViewModel(
    private val purchaseId: Long,
    private val historyRepository: ShoppingListHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivedListUiState())
    val uiState: StateFlow<ArchivedListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { historyRepository.getPurchase(purchaseId) }
            result.fold(
                onSuccess = { purchase ->
                    val items = purchase.items.map { it.toArchivedListItem() }
                    val grouped = items.groupBy { it.categoryName }
                    _uiState.value = ArchivedListUiState(
                        isLoading = false,
                        purchase = purchase,
                        itemsByCategory = grouped
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al cargar el historial"
                        )
                    }
                }
            )
        }
    }
}

class ArchivedListDetailViewModelFactory(
    private val purchaseId: Long,
    private val historyRepository: ShoppingListHistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchivedListDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchivedListDetailViewModel(purchaseId, historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun ListItemDto.toArchivedListItem(): ArchivedListItem {
    val productMetadata = product.metadata?.jsonObject
    val price = productMetadata?.get("price")?.jsonPrimitive?.doubleOrNull ?: 0.0
    val defaultUnit = productMetadata?.get("unit")?.jsonPrimitive?.contentOrNull ?: ""
    return ArchivedListItem(
        id = id,
        productName = product.name,
        categoryName = product.category?.name,
        quantity = quantity,
        acquired = purchased,
        unit = unit ?: defaultUnit,
        price = price
    )
}
