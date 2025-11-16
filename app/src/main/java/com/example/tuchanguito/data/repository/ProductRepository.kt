package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.ProductRemoteDataSource
import com.example.tuchanguito.data.network.model.ProductDto
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import retrofit2.HttpException

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

    suspend fun createProduct(
        name: String,
        categoryId: Long? = null,
        price: Double? = null,
        unit: String? = null,
        lowStockThreshold: Int? = null
    ): Product {
        return try {
            remoteDataSource.createProduct(
                name,
                categoryId,
                buildMetadata(price, unit, lowStockThreshold)
            ).asModel()
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 409) {
                findProductByName(name)
                    ?: throw IllegalStateException("El producto ya existe pero no se pudo recuperar", t)
            } else {
                throw t
            }
        }
    }

    suspend fun updateProduct(
        id: Long,
        name: String,
        categoryId: Long? = null,
        price: Double? = null,
        unit: String? = null,
        lowStockThreshold: Int? = null
    ): Product = remoteDataSource.updateProduct(
        id,
        name,
        categoryId,
        buildMetadata(price, unit, lowStockThreshold)
    ).asModel()

    suspend fun deleteProduct(id: Long) = remoteDataSource.deleteProduct(id)

    suspend fun findProductByName(name: String): Product? =
        remoteDataSource.getProducts(name = name, perPage = 1).data.firstOrNull()?.asModel()

    private fun ProductDto.asModel(): Product {
        val metadataObject = metadata?.jsonObject
        val priceValue = metadataObject?.get("price")?.jsonPrimitive?.doubleOrNull ?: 0.0
        val unitValue = metadataObject?.get("unit")?.jsonPrimitive?.contentOrNull ?: ""
        val thresholdValue = metadataObject
            ?.get("lowStockThreshold")
            ?.jsonPrimitive
            ?.intOrNull
            ?.takeIf { it > 0 }
            ?: 2
        return Product(
            id = id,
            name = name,
            price = priceValue,
            categoryId = category?.id,
            unit = unitValue,
            lowStockThreshold = thresholdValue
        )
    }

    private fun buildMetadata(price: Double?, unit: String?, lowStockThreshold: Int?): JsonElement? {
        val hasPrice = price != null
        val hasUnit = !unit.isNullOrBlank()
        val hasThreshold = lowStockThreshold != null && lowStockThreshold > 0
        if (!hasPrice && !hasUnit && !hasThreshold) return null
        return buildJsonObject {
            price?.let { put("price", it) }
            unit?.takeIf { it.isNotBlank() }?.let { put("unit", it) }
            lowStockThreshold?.takeIf { it > 0 }?.let { put("lowStockThreshold", it) }
        }
    }
}
