package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.data.network.PantryRemoteDataSource
import com.example.tuchanguito.data.network.model.PantryItemDto
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException

class PantryRepository(
    private val remote: PantryRemoteDataSource,
    private val preferences: PreferencesManager
) {

    private var cachedPantryId: Long? = null

    private suspend fun resolvePantryId(): Long {
        cachedPantryId?.let { return it }
        val saved = preferences.currentPantryId.firstOrNull()
        if (saved != null && runCatching { remote.getPantry(saved) }.isSuccess) {
            cachedPantryId = saved
            return saved
        }
        val fallbackName = "Alacena ${System.currentTimeMillis() % 10_000}"
        val created = remote.createPantry(fallbackName, metadata = null)
        preferences.setCurrentPantryId(created.id)
        cachedPantryId = created.id
        return created.id
    }

    private suspend fun resetPantry(): Long {
        preferences.setCurrentPantryId(null)
        cachedPantryId = null
        return resolvePantryId()
    }

    suspend fun getItems(
        search: String? = null,
        categoryId: Long? = null
    ): List<PantryItemDto> {
        var pantryId = resolvePantryId()
        val fetch: suspend () -> List<PantryItemDto> = {
            remote.getItems(
                pantryId = pantryId,
                search = search,
                categoryId = categoryId,
                page = 1,
                perPage = 100
            ).data
        }
        val result = runCatching { fetch() }
        return result.getOrElse { error ->
            if (error is HttpException && error.code() == 500) {
                pantryId = resetPantry()
                fetch()
            } else {
                throw error
            }
        }
    }

    suspend fun getCategoriesForQuery(search: String?): List<com.example.tuchanguito.data.network.model.CategoryDto> {
        var pantryId = resolvePantryId()
        val fetch: suspend () -> List<PantryItemDto> = {
            remote.getItems(
                pantryId = pantryId,
                search = search,
                page = 1,
                perPage = 100
            ).data
        }
        val result = runCatching { fetch() }
        val items = result.getOrElse { error ->
            if (error is HttpException && error.code() == 500) {
                pantryId = resetPantry()
                fetch()
            } else {
                throw error
            }
        }
        return items.mapNotNull { it.product.category }
            .distinctBy { it.id }
    }

    suspend fun addOrIncrementItem(
        productId: Long,
        quantity: Double,
        unit: String,
        metadata: JsonObject? = null
    ): PantryItemDto {
        val pantryId = resolvePantryId()
        return try {
            remote.addItem(pantryId, productId, quantity, unit, metadata)
        } catch (error: Throwable) {
            if (error is HttpException && error.code() == 409) {
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
    ): PantryItemDto {
        val pantryId = resolvePantryId()
        return remote.updateItem(pantryId, itemId, quantity, unit)
    }

    suspend fun deleteItem(itemId: Long) {
        val pantryId = resolvePantryId()
        remote.deleteItem(pantryId, itemId)
    }

}
