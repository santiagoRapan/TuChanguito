package com.example.tuchanguito.data.network

import com.example.tuchanguito.data.network.api.CategoryApiService
import com.example.tuchanguito.data.network.core.RemoteDataSource
import com.example.tuchanguito.data.network.model.CategoryDto
import com.example.tuchanguito.data.network.model.CreateCategoryRequestDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.UpdateCategoryRequestDto
import kotlinx.serialization.json.JsonElement

class CategoryRemoteDataSource(
    private val api: CategoryApiService
): RemoteDataSource() {

    suspend fun getCategories(
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null,
        name: String? = null
    ): PaginatedResponseDto<CategoryDto> = safeApiCall {
        api.getCategories(page, perPage, sortBy, order, name)
    }

    suspend fun getCategory(id: Long): CategoryDto = safeApiCall { api.getCategoryById(id) }

    suspend fun createCategory(name: String, metadata: JsonElement? = null): CategoryDto =
        safeApiCall { api.createCategory(CreateCategoryRequestDto(name = name, metadata = metadata)) }

    suspend fun updateCategory(id: Long, name: String, metadata: JsonElement? = null): CategoryDto =
        safeApiCall { api.updateCategory(id, UpdateCategoryRequestDto(name = name, metadata = metadata)) }

    suspend fun deleteCategory(id: Long) = safeApiCall { api.deleteCategory(id) }
}
