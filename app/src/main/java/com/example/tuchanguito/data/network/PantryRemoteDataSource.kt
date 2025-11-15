package com.example.tuchanguito.data.network

import com.example.tuchanguito.data.network.api.PantryApiService
import com.example.tuchanguito.data.network.model.CreatePantryItemRequestDto
import com.example.tuchanguito.data.network.model.CreatePantryRequestDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.PantryDto
import com.example.tuchanguito.data.network.model.PantryItemDto
import com.example.tuchanguito.data.network.model.ProductReferenceDto
import com.example.tuchanguito.data.network.model.UpdatePantryItemRequestDto
import com.example.tuchanguito.data.network.model.UpdatePantryRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import kotlinx.serialization.json.JsonObject

class PantryRemoteDataSource(
    private val api: PantryApiService
) {

    suspend fun getPantries(
        owner: Boolean? = null,
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null
    ): PaginatedResponseDto<PantryDto> =
        api.getPantries(owner, page, perPage, sortBy, order)

    suspend fun getPantry(id: Long): PantryDto = api.getPantry(id)

    suspend fun createPantry(name: String, metadata: JsonObject? = null): PantryDto =
        api.createPantry(CreatePantryRequestDto(name, metadata))

    suspend fun updatePantry(id: Long, name: String, metadata: JsonObject? = null): PantryDto =
        api.updatePantry(id, UpdatePantryRequestDto(name, metadata))

    suspend fun deletePantry(id: Long) = api.deletePantry(id)

    suspend fun getItems(
        pantryId: Long,
        search: String? = null,
        categoryId: Long? = null,
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null
    ): PaginatedResponseDto<PantryItemDto> =
        api.getItems(pantryId, search, categoryId, page, perPage, sortBy, order)

    suspend fun getItemsRaw(
        pantryId: Long,
        page: Int? = null,
        perPage: Int? = null
    ): Response<ResponseBody> = api.getItemsRaw(pantryId, page, perPage)

    suspend fun addItem(
        pantryId: Long,
        productId: Long,
        quantity: Double,
        unit: String,
        metadata: JsonObject? = null
    ): PantryItemDto =
        api.addItem(
            pantryId,
            CreatePantryItemRequestDto(
                product = ProductReferenceDto(productId),
                quantity = quantity,
                unit = unit,
                metadata = metadata
            )
        )

    suspend fun updateItem(
        pantryId: Long,
        itemId: Long,
        quantity: Double,
        unit: String,
        metadata: JsonObject? = null
    ): PantryItemDto =
        api.updateItem(
            pantryId,
            itemId,
            UpdatePantryItemRequestDto(
                quantity = quantity,
                unit = unit,
                metadata = metadata
            )
        )

    suspend fun deleteItem(pantryId: Long, itemId: Long) = api.deleteItem(pantryId, itemId)
}
