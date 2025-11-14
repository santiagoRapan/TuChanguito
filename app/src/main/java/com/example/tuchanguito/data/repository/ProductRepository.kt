package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.network.ProductRemoteDataSource
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.model.ProductDto
import kotlinx.serialization.json.JsonElement

class ProductRepository(
    private val remoteDataSource: ProductRemoteDataSource
) {

    suspend fun getProducts(
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null,
        name: String? = null,
        categoryId: Long? = null
    ): List<Product> =
        remoteDataSource.getProducts(page, perPage, sortBy, order, name, categoryId).data.map { it.asModel() }

    suspend fun getProduct(id: Long): Product = remoteDataSource.getProduct(id).asModel()

    suspend fun createProduct(name: String, categoryId: Long? = null, metadata: JsonElement? = null): Product =
        remoteDataSource.createProduct(name, categoryId, metadata).asModel()

    suspend fun updateProduct(id: Long, name: String, categoryId: Long? = null, metadata: JsonElement? = null): Product =
        remoteDataSource.updateProduct(id, name, categoryId, metadata).asModel()

    suspend fun deleteProduct(id: Long) = remoteDataSource.deleteProduct(id)

    private fun ProductDto.asModel(): Product = Product(
        id = id,
        name = name,
        price = 0.0,
        categoryId = category?.id,
        unit = ""
    )
}
