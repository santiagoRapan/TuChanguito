package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.network.CategoryRemoteDataSource
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.network.model.CategoryDto
import kotlinx.serialization.json.JsonElement

class CategoryRepository(
    private val remoteDataSource: CategoryRemoteDataSource
) {

    suspend fun createCategory(name: String, metadata: JsonElement? = null): Category {
        return remoteDataSource.createCategory(name, metadata).asModel()
    }

    suspend fun updateCategory(id: Long, name: String, metadata: JsonElement? = null): Category {
        return remoteDataSource.updateCategory(id, name, metadata).asModel()
    }

    suspend fun getCategories(
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null,
        name: String? = null
    ): List<Category> {
        return remoteDataSource.getCategories(page, perPage, sortBy, order, name).data.map { it.asModel() }
    }

    suspend fun getCategory(id: Long): Category = remoteDataSource.getCategory(id).asModel()

    suspend fun deleteCategory(id: Long) = remoteDataSource.deleteCategory(id)

    private fun CategoryDto.asModel(): Category = Category(
        id = id,
        name = name
    )
}
