package com.example.tuchanguito.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.data.repository.PantryRepository
import com.example.tuchanguito.data.repository.ShoppingListsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.ceil

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeList: ShoppingListDto? = null,
    val lowStockItems: List<LowStockItemUi> = emptyList(),
    val listOptions: List<ShoppingListOption> = emptyList(),
    val isProcessingLowStock: Boolean = false,
    val errorMessage: String? = null,
    // progreso de compra de la lista activa
    val activePurchasedCount: Int = 0,
    val activeTotalCount: Int = 0,
    val activeProgress: Float = 0f
)

data class LowStockItemUi(
    val pantryItemId: Long,
    val productId: Long,
    val name: String,
    val category: String?,
    val currentQuantity: Double,
    val unit: String,
    val lowStockThreshold: Int
) {
    val suggestedQuantity: Int
        get() {
            val deficit = ceil(lowStockThreshold - currentQuantity).toInt()
            return deficit.coerceAtLeast(1)
        }
}

data class ShoppingListOption(val id: Long, val name: String)

sealed interface HomeEvent {
    data class LowStockAdded(val quantity: Int, val listName: String?) : HomeEvent
    data class ShowError(val message: String) : HomeEvent
}

class HomeViewModel(
    private val repository: ShoppingListsRepository,
    private val pantryRepository: PantryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching {
                kotlinx.coroutines.coroutineScope {
                    val listsDeferred = async {
                        repository.getLists(
                            owner = null,
                            sortBy = "updatedAt",
                            order = "DESC",
                            perPage = 50
                        )
                    }
                    val pantryDeferred = async { pantryRepository.getItems() }
                    val lists = listsDeferred.await()
                    val active = lists.data.firstOrNull()
                    val items = if (active != null) repository.getItems(active.id) else emptyList()
                    Triple(lists, pantryDeferred.await(), items)
                }
            }
            val processedResult = result.fold(
                onSuccess = { (response, pantryItems, activeItems) ->
                    val lowStockRaw = pantryItems.toLowStock()
                    val dismissedIds = pantryRepository.dismissedLowStockIds()
                    pantryRepository.retainDismissedLowStockIds(lowStockRaw.map { it.pantryItemId }.toSet())
                    Result.success(Triple(response, lowStockRaw.filterNot { dismissedIds.contains(it.pantryItemId) }, activeItems))
                },
                onFailure = { throwable -> Result.failure(throwable) }
            )
            _uiState.update { current ->
                processedResult.fold(
                    onSuccess = { (response, filteredLowStock, activeItems) ->
                        val active = response.data.firstOrNull()
                        val purchased = activeItems.count { it.purchased }
                        val total = activeItems.size
                        val progress = if (total > 0) purchased.toFloat() / total.toFloat() else 0f
                        current.copy(
                            isLoading = false,
                            activeList = active,
                            lowStockItems = filteredLowStock,
                            listOptions = response.data.map { ShoppingListOption(it.id, it.name) },
                            activePurchasedCount = purchased,
                            activeTotalCount = total,
                            activeProgress = progress
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

    fun addLowStockItemToList(
        pantryItemId: Long,
        listId: Long,
        quantity: Int
    ) {
        val item = _uiState.value.lowStockItems.firstOrNull { it.pantryItemId == pantryItemId } ?: return
        val resolvedQuantity = quantity.coerceAtLeast(1)
        val unit = item.unit.ifBlank { DEFAULT_UNIT }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingLowStock = true) }
            val result = runCatching {
                repository.addItem(
                    listId,
                    productId = item.productId,
                    quantity = resolvedQuantity.toDouble(),
                    unit = unit
                )
            }
            _uiState.update { it.copy(isProcessingLowStock = false) }
            result.fold(
                onSuccess = {
                    pantryRepository.dismissLowStockItem(pantryItemId)
                    _uiState.update { state ->
                        state.copy(
                            lowStockItems = state.lowStockItems.filterNot { low -> low.pantryItemId == pantryItemId }
                        )
                    }
                    val listName = _uiState.value.listOptions.firstOrNull { it.id == listId }?.name
                    _events.emit(HomeEvent.LowStockAdded(resolvedQuantity, listName))
                },
                onFailure = { error ->
                    _events.emit(
                        HomeEvent.ShowError(
                            error.message ?: "Error al agregar el producto"
                        )
                    )
                }
            )
        }
    }

    private fun List<com.example.tuchanguito.data.network.model.PantryItemDto>.toLowStock(): List<LowStockItemUi> {
        return this.mapNotNull { item ->
            val metadata = item.product.metadata?.jsonObject
            val threshold = metadata
                ?.get("lowStockThreshold")
                ?.jsonPrimitive
                ?.intOrNull
                ?.takeIf { it > 0 }
                ?: DEFAULT_LOW_STOCK_THRESHOLD
            val quantity = item.quantity
            if (quantity > threshold) return@mapNotNull null
            val unit = item.unit
                ?: metadata?.get("unit")?.jsonPrimitive?.contentOrNull
                ?: DEFAULT_UNIT
            LowStockItemUi(
                pantryItemId = item.id,
                productId = item.product.id,
                name = item.product.name,
                category = item.product.category?.name,
                currentQuantity = quantity,
                unit = unit,
                lowStockThreshold = threshold
            )
        }.sortedBy { it.currentQuantity }
            .take(5)
    }

    companion object {
        private const val DEFAULT_LOW_STOCK_THRESHOLD = 2
        private const val DEFAULT_UNIT = "u"
    }
}

class HomeViewModelFactory(
    private val repository: ShoppingListsRepository,
    private val pantryRepository: PantryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, pantryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
