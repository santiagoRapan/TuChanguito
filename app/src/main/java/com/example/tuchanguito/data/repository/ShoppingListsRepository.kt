package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.network.ShoppingListsRemoteDataSource
import com.example.tuchanguito.data.network.model.ListItemDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.SharedUserDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import kotlinx.serialization.json.JsonObject

class ShoppingListsRepository(
    private val remote: ShoppingListsRemoteDataSource
) {
    suspend fun getLists(
        name: String? = null,
        owner: Boolean? = null,
        recurring: Boolean? = null,
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null
    ): PaginatedResponseDto<ShoppingListDto> =
        remote.listShoppingLists(name, owner, recurring, page, perPage, sortBy, order)

    suspend fun getList(id: Long): ShoppingListDto = remote.getShoppingList(id)

    suspend fun createList(
        name: String,
        description: String,
        recurring: Boolean = false,
        metadata: JsonObject? = null
    ): ShoppingListDto =
        remote.createShoppingList(name, description, recurring, metadata)

    suspend fun updateList(
        id: Long,
        name: String? = null,
        description: String? = null,
        recurring: Boolean? = null,
        metadata: JsonObject? = null
    ): ShoppingListDto =
        remote.updateShoppingList(id, name, description, recurring, metadata)

    suspend fun deleteList(id: Long) = remote.deleteShoppingList(id)

    suspend fun purchaseList(id: Long, metadata: JsonObject? = null): ShoppingListDto =
        remote.purchaseShoppingList(id, metadata)

    suspend fun resetList(id: Long): ShoppingListDto = remote.resetShoppingList(id)

    suspend fun moveListToPantry(id: Long) = remote.moveShoppingListToPantry(id)

    suspend fun shareList(id: Long, email: String): ShoppingListDto = remote.shareShoppingList(id, email)

    suspend fun sharedUsers(id: Long): List<SharedUserDto> = remote.getSharedUsers(id)

    suspend fun revokeShare(id: Long, userId: Long) = remote.revokeShare(id, userId)

    suspend fun getItems(listId: Long): List<ListItemDto> = remote.getItems(listId).data

    suspend fun addItem(
        listId: Long,
        productId: Long,
        quantity: Double = 1.0,
        unit: String = "u",
        metadata: JsonObject? = null
    ): ListItemDto = remote.addItem(listId, productId, quantity, unit, metadata)

    suspend fun updateItem(
        listId: Long,
        itemId: Long,
        productId: Long,
        quantity: Double,
        unit: String,
        metadata: JsonObject? = null
    ): ListItemDto = remote.updateItem(listId, itemId, productId, quantity, unit, metadata)

    suspend fun toggleItem(listId: Long, itemId: Long, purchased: Boolean): ListItemDto =
        remote.toggleItem(listId, itemId, purchased)

    suspend fun deleteItem(listId: Long, itemId: Long) = remote.deleteItem(listId, itemId)
}
