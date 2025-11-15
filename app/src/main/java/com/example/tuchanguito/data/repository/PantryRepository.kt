package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.data.network.PantryRemoteDataSource
import com.example.tuchanguito.data.network.core.DataSourceException
import com.example.tuchanguito.data.network.model.PantryItemDto
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.JsonObject

class PantryRepository(
    private val remote: PantryRemoteDataSource,
    private val preferences: PreferencesManager
) {

    private var cachedPantry: Pair<Long?, Long?>? = null

    private suspend fun resolvePantryId(): Long {
        val userId = preferences.currentUserId.firstOrNull()
        val cached = cachedPantry
        if (cached != null && cached.first == userId && cached.second != null) {
            return cached.second!!
        }
        val saved = preferences.getCurrentPantryIdForUser(userId)
        if (saved != null && runCatching { remote.getPantry(saved) }.isSuccess) {
            cachedPantry = userId to saved
            return saved
        }
        val fallbackName = "Alacena ${System.currentTimeMillis() % 10_000}"
        val created = remote.createPantry(fallbackName, metadata = null)
        preferences.setCurrentPantryIdForUser(userId, created.id)
        cachedPantry = userId to created.id
        return created.id
    }

    private suspend fun resetPantry(): Long {
        val userId = preferences.currentUserId.firstOrNull()
        preferences.setCurrentPantryIdForUser(userId, null)
        cachedPantry = null
        return resolvePantryId()
    }

    private suspend fun <T> withPantry(block: suspend (Long) -> T): T {
        var pantryId = resolvePantryId()
        return try {
            block(pantryId)
        } catch (error: Throwable) {
            if (error is DataSourceException && (error.statusCode == 404 || error.statusCode == 500)) {
                pantryId = resetPantry()
                block(pantryId)
            } else {
                throw error
            }
        }
    }

    suspend fun getItems(
        search: String? = null,
        categoryId: Long? = null
    ): List<PantryItemDto> = withPantry { pantryId ->
        remote.getItems(
            pantryId = pantryId,
            search = search,
            categoryId = categoryId,
            page = 1,
            perPage = 100
        ).data
    }

    suspend fun getCategoriesForQuery(search: String?): List<com.example.tuchanguito.data.network.model.CategoryDto> =
        withPantry { pantryId ->
            remote.getItems(
                pantryId = pantryId,
                search = search,
                page = 1,
                perPage = 100
            ).data
        }.mapNotNull { it.product.category }
            .distinctBy { it.id }

    suspend fun addOrIncrementItem(
        productId: Long,
        quantity: Double,
        unit: String,
        metadata: JsonObject? = null
    ): PantryItemDto = withPantry { pantryId ->
        try {
            remote.addItem(pantryId, productId, quantity, unit, metadata)
        } catch (error: Throwable) {
            if (error is DataSourceException && error.statusCode == 409) {
                val existing = remote.getItems(
                    pantryId = pantryId,
                    page = 1,
                    perPage = 100
                ).data.firstOrNull { it.product.id == productId }
                    ?: throw error
                remote.updateItem(
                    pantryId = pantryId,
                    itemId = existing.id,
                    quantity = existing.quantity + quantity,
                    unit = unit.ifBlank { existing.unit ?: unit },
                    metadata = metadata
                )
            } else {
                throw error
            }
        }
    }

    suspend fun updateItemQuantity(
        itemId: Long,
        quantity: Double,
        unit: String
    ): PantryItemDto = withPantry { pantryId ->
        remote.updateItem(pantryId, itemId, quantity, unit)
    }

    suspend fun deleteItem(itemId: Long) {
        withPantry { pantryId -> remote.deleteItem(pantryId, itemId) }
    }
}
