package com.example.tuchanguito.data.network

import com.example.tuchanguito.data.network.api.ShoppingListsApiService
import com.example.tuchanguito.data.network.model.CreateListItemRequestDto
import com.example.tuchanguito.data.network.model.CreateShoppingListRequestDto
import com.example.tuchanguito.data.network.model.ListItemDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.PurchaseListRequestDto
import com.example.tuchanguito.data.network.model.ShareListRequestDto
import com.example.tuchanguito.data.network.model.SharedUserDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.data.network.model.ToggleItemRequestDto
import com.example.tuchanguito.data.network.model.UpdateShoppingListRequestDto
import kotlinx.serialization.json.JsonObject

class ShoppingListsRemoteDataSource(
    private val api: ShoppingListsApiService
) {
    suspend fun listShoppingLists(
        name: String? = null,
        owner: Boolean? = null,
        recurring: Boolean? = null,
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null
    ): PaginatedResponseDto<ShoppingListDto> =
        api.getLists(name, owner, recurring, page, perPage, sortBy, order)

    suspend fun getShoppingList(id: Long): ShoppingListDto = api.getList(id)

    suspend fun createShoppingList(
        name: String,
        description: String,
        recurring: Boolean = false,
        metadata: JsonObject? = null
    ): ShoppingListDto =
        api.createList(CreateShoppingListRequestDto(name, description, recurring, metadata))

    suspend fun updateShoppingList(
        id: Long,
        name: String? = null,
        description: String? = null,
        recurring: Boolean? = null,
        metadata: JsonObject? = null
    ): ShoppingListDto =
        api.updateList(id, UpdateShoppingListRequestDto(name, description, recurring, metadata))

    suspend fun deleteShoppingList(id: Long) = api.deleteList(id)

    suspend fun purchaseShoppingList(id: Long, metadata: JsonObject? = null): ShoppingListDto =
        api.purchaseList(id, PurchaseListRequestDto(metadata))

    suspend fun resetShoppingList(id: Long): ShoppingListDto = api.resetList(id)

    suspend fun moveShoppingListToPantry(id: Long): ShoppingListDto = api.moveListToPantry(id)

    suspend fun shareShoppingList(id: Long, email: String): ShoppingListDto =
        api.shareList(id, ShareListRequestDto(email))

    suspend fun getSharedUsers(id: Long): List<SharedUserDto> = api.getSharedUsers(id)

    suspend fun revokeShare(id: Long, userId: Long) = api.revokeShare(id, userId)

    suspend fun getItems(listId: Long): List<ListItemDto> = api.getItems(listId)

    suspend fun addItem(
        listId: Long,
        productId: Long,
        quantity: Double = 1.0,
        unit: String = "u",
        metadata: JsonObject? = null
    ): ListItemDto = api.addItem(listId, CreateListItemRequestDto(product = com.example.tuchanguito.data.network.model.ProductReferenceDto(productId), quantity = quantity, unit = unit, metadata = metadata))

    suspend fun updateItem(
        listId: Long,
        itemId: Long,
        productId: Long,
        quantity: Double,
        unit: String,
        metadata: JsonObject? = null
    ): ListItemDto = api.updateItem(
        listId,
        itemId,
        CreateListItemRequestDto(product = com.example.tuchanguito.data.network.model.ProductReferenceDto(productId), quantity = quantity, unit = unit, metadata = metadata)
    )

    suspend fun toggleItem(listId: Long, itemId: Long, purchased: Boolean): ListItemDto =
        api.toggleItem(listId, itemId, ToggleItemRequestDto(purchased))

    suspend fun deleteItem(listId: Long, itemId: Long) = api.deleteItem(listId, itemId)
}
