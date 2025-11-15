package com.example.tuchanguito.data.network

import com.example.tuchanguito.data.network.api.ProductApiService
import com.example.tuchanguito.data.network.core.RemoteDataSource
import com.example.tuchanguito.data.network.model.CreateProductRequestDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.ProductCategoryReferenceDto
import com.example.tuchanguito.data.network.model.ProductDto
import com.example.tuchanguito.data.network.model.UpdateProductRequestDto
import kotlinx.serialization.json.JsonElement

class ProductRemoteDataSource(
    private val api: ProductApiService
): RemoteDataSource() {

    suspend fun getProducts(
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null,
        name: String? = null,
        categoryId: Long? = null
    ): PaginatedResponseDto<ProductDto> = safeApiCall {
        api.getProducts(page, perPage, sortBy, order, name, categoryId)
    }

    suspend fun getProduct(id: Long): ProductDto = safeApiCall { api.getProduct(id) }

    suspend fun createProduct(
        name: String,
        categoryId: Long? = null,
        metadata: JsonElement? = null
    ): ProductDto = safeApiCall {
        api.createProduct(
            CreateProductRequestDto(
                name = name,
                category = categoryId?.let { ProductCategoryReferenceDto(it) },
                metadata = metadata
            )
        )
    }

    suspend fun updateProduct(
        id: Long,
        name: String,
        categoryId: Long? = null,
        metadata: JsonElement? = null
    ): ProductDto = safeApiCall {
        api.updateProduct(
            id,
            UpdateProductRequestDto(
                name = name,
                category = categoryId?.let { ProductCategoryReferenceDto(it) },
                metadata = metadata
            )
        )
    }

    suspend fun deleteProduct(id: Long) = safeApiCall { api.deleteProduct(id) }
}
