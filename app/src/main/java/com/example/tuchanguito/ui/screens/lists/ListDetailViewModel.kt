package com.example.tuchanguito.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.model.ListItemDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.data.repository.CategoryRepository
import com.example.tuchanguito.data.repository.PantryRepository
import com.example.tuchanguito.data.repository.ProductRepository
import com.example.tuchanguito.data.repository.ShoppingListsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ListDetailUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val list: ShoppingListDto? = null,
    val items: List<ListItemDto> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableLists: List<ShoppingListDto> = emptyList(),
    val errorMessage: String? = null
)

data class ShareUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

data class ListFinalizeOptions(
    val includePurchasedToPantry: Boolean,
    val moveNotPurchased: Boolean,
    val targetListId: Long?,
    val newListName: String?
)

sealed interface ListDetailEvent {
    data class ShowSnackbar(val message: String) : ListDetailEvent
    object ItemAdded : ListDetailEvent
    object ListFinalized : ListDetailEvent
}

class ListDetailViewModel(
    private val listId: Long,
    private val shoppingListsRepository: ShoppingListsRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val pantryRepository: PantryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    private val _shareState = MutableStateFlow(ShareUiState())
    val shareState: StateFlow<ShareUiState> = _shareState.asStateFlow()

    private val _events = MutableSharedFlow<ListDetailEvent>()
    val events: SharedFlow<ListDetailEvent> = _events.asSharedFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching {
                coroutineScope {
                    val listDeferred = async { shoppingListsRepository.getList(listId) }
                    val itemsDeferred = async { shoppingListsRepository.getItems(listId) }
                    val productsDeferred = async { productRepository.getProducts(perPage = 200) }
                    val categoriesDeferred = async { categoryRepository.getCategories(perPage = 200) }
                    val list = listDeferred.await()
                    val items = itemsDeferred.await()
                    val products = productsDeferred.await()
                    val categories = categoriesDeferred.await()
                    Quadruple(list, items, products, categories)
                }
            }
            result.fold(
                onSuccess = { (list, items, products, categories) ->
                    _uiState.value = ListDetailUiState(
                        isLoading = false,
                        list = list,
                        items = items,
                        products = products,
                        categories = categories
                    )
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Error al cargar la lista") }
                    _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al cargar la lista"))
                }
            )
        }
    }

    fun updateItemQuantity(itemId: Long, productId: Long, newQuantity: Double, unit: String?) {
        if (newQuantity <= 0) {
            deleteItem(itemId)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                shoppingListsRepository.updateItem(listId, itemId, productId, newQuantity, unit ?: "u")
            }
            result.fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(items = state.items.map { if (it.id == updated.id) updated else it })
                    }
                },
                onFailure = { error ->
                    _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al actualizar cantidad"))
                }
            )
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            val result = runCatching { shoppingListsRepository.deleteItem(listId, itemId) }
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(items = state.items.filterNot { it.id == itemId })
                    }
                },
                onFailure = { error ->
                    _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al eliminar producto"))
                }
            )
        }
    }

    fun toggleItem(itemId: Long, purchased: Boolean) {
        viewModelScope.launch {
            val result = runCatching { shoppingListsRepository.toggleItem(listId, itemId, purchased) }
            result.fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(items = state.items.map { if (it.id == updated.id) updated else it })
                    }
                },
                onFailure = { error ->
                    _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al actualizar estado"))
                }
            )
        }
    }

    fun addItem(productId: Long?, name: String, price: Double?, unit: String?, categoryName: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedCategory = categoryName.trim()
            if (trimmedName.isEmpty() || trimmedCategory.isEmpty()) {
                _events.emit(ListDetailEvent.ShowSnackbar("El nombre y la categoría son obligatorios"))
                return@launch
            }
            val normalizedUnit = unit?.takeIf { it.isNotBlank() } ?: "u"
            val resolvedProductResult = runCatching {
                productId ?: createProduct(trimmedName, trimmedCategory, price, normalizedUnit)
            }
            resolvedProductResult.fold(
                onSuccess = { resolvedId ->
                    val addResult = runCatching {
                        shoppingListsRepository.addItem(listId, resolvedId, 1.0, normalizedUnit)
                    }
                    addResult.fold(
                        onSuccess = { created ->
                            _uiState.update { state -> state.copy(items = state.items + created) }
                            _events.emit(ListDetailEvent.ItemAdded)
                        },
                        onFailure = { error ->
                            if (error is HttpException && error.code() == 409) {
                                val handled = incrementExistingItem(resolvedId, normalizedUnit)
                                if (handled) {
                                    _events.emit(ListDetailEvent.ItemAdded)
                                } else {
                                    _events.emit(ListDetailEvent.ShowSnackbar("Ya existe un ítem con ese producto y no se pudo actualizar."))
                                }
                            } else {
                                _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al añadir producto"))
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al crear el producto"))
                }
            )
        }
    }

    fun shareList(email: String) {
        viewModelScope.launch {
            _shareState.value = ShareUiState(isBusy = true, message = null, isError = false)
            val result = runCatching { shoppingListsRepository.shareList(listId, email) }
            _shareState.value = result.fold(
                onSuccess = { ShareUiState(isBusy = false, message = "Lista compartida con éxito", isError = false) },
                onFailure = { error -> ShareUiState(isBusy = false, message = error.message ?: "Error al compartir", isError = true) }
            )
        }
    }

    fun resetShareState() {
        _shareState.value = ShareUiState()
    }

    fun loadAvailableLists() {
        viewModelScope.launch {
            val result = runCatching { shoppingListsRepository.getLists(owner = true, perPage = 200).data }
            result.onSuccess { lists ->
                _uiState.update { it.copy(availableLists = lists.filterNot { list -> list.id == listId }) }
            }
        }
    }

    fun finalizeList(options: ListFinalizeOptions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = runCatching {
                val items = shoppingListsRepository.getItems(listId)
                val purchased = items.filter { it.purchased }
                val pending = items.filterNot { it.purchased }

                if (options.includePurchasedToPantry && purchased.isNotEmpty()) {
                    purchased.forEach { item ->
                        pantryRepository.addOrIncrementItem(
                            productId = item.product.id,
                            quantity = item.quantity,
                            unit = item.unit ?: "u"
                        )
                    }
                }

                if (options.moveNotPurchased && pending.isNotEmpty()) {
                    val targetId = when {
                        !options.newListName.isNullOrBlank() -> {
                            shoppingListsRepository.createList(
                                name = options.newListName.trim(),
                                description = "",
                                recurring = false
                            ).id
                        }
                        options.targetListId != null -> options.targetListId
                        else -> null
                    }

                    if (targetId != null) {
                        pending.forEach { item ->
                            shoppingListsRepository.addItem(
                                targetId,
                                productId = item.product.id,
                                quantity = item.quantity,
                                unit = item.unit ?: "u"
                            )
                        }
                    }
                }

                shoppingListsRepository.deleteList(listId)
            }
            _uiState.update { it.copy(isProcessing = false) }
            result.fold(
                onSuccess = {
                    _events.emit(ListDetailEvent.ShowSnackbar("Lista finalizada"))
                    _events.emit(ListDetailEvent.ListFinalized)
                },
                onFailure = { error ->
                    _events.emit(ListDetailEvent.ShowSnackbar(error.message ?: "Error al finalizar la lista"))
                }
            )
        }
    }

    private suspend fun createProduct(
        name: String,
        categoryName: String,
        price: Double?,
        unit: String?
    ): Long {
        val categoryId = findOrCreateCategory(categoryName)
        val product = productRepository.createProduct(name, categoryId, price, unit)
        _uiState.update { state -> state.copy(products = (state.products + product).distinctBy { it.id }) }
        return product.id
    }

    private suspend fun findOrCreateCategory(categoryName: String): Long {
        val existing = _uiState.value.categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        if (existing != null) return existing.id
        val created = categoryRepository.createCategory(categoryName)
        _uiState.update { state -> state.copy(categories = state.categories + created) }
        return created.id
    }

    private suspend fun incrementExistingItem(productId: Long, fallbackUnit: String): Boolean {
        val currentItems = _uiState.value.items
        val cached = currentItems.firstOrNull { it.product.id == productId }
        val target = cached ?: run {
            val refreshed = runCatching { shoppingListsRepository.getItems(listId) }.getOrNull() ?: return false
            _uiState.update { it.copy(items = refreshed) }
            refreshed.firstOrNull { it.product.id == productId }
        } ?: return false

        val updated = shoppingListsRepository.updateItem(
            listId,
            target.id,
            productId,
            target.quantity + 1,
            target.unit?.takeIf { it.isNotBlank() } ?: fallbackUnit
        )
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == updated.id) updated else it })
        }
        return true
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}

class ListDetailViewModelFactory(
    private val listId: Long,
    private val shoppingListsRepository: ShoppingListsRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val pantryRepository: PantryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListDetailViewModel(
                listId,
                shoppingListsRepository,
                productRepository,
                categoryRepository,
                pantryRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
