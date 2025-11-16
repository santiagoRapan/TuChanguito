package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.db.CategoryDao
import com.example.tuchanguito.data.db.ProductDao
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.CategoryRemoteDataSource
import com.example.tuchanguito.data.network.ProductRemoteDataSource
import com.example.tuchanguito.data.network.model.CategoryDto
import com.example.tuchanguito.data.network.model.ProductDto
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class CatalogRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val categoryRemote: CategoryRemoteDataSource,
    private val productRemote: ProductRemoteDataSource
) {

    suspend fun ensureDefaultCategories(defaults: List<String> = DEFAULT_CATEGORIES) {
        defaults.forEach { createOrFindCategoryByName(it) }
    }

    suspend fun syncCatalog(): Result<Unit> = runCatching {
        categoryDao.clearAll()
        productDao.clearAll()

        val categories = categoryRemote.getCategories(page = 1, perPage = 1000).data
        categories.forEach { dto ->
            categoryDao.upsert(Category(id = dto.id, name = dto.name))
        }

        val products = productRemote.getProducts(page = 1, perPage = 1000).data
        products.forEach { dto ->
            productDao.upsert(dto.toEntity())
        }
    }

    suspend fun searchProducts(name: String?, categoryId: Long?): List<ProductDto> =
        productRemote.getProducts(name = name?.takeIf { it.isNotBlank() }, categoryId = categoryId, page = 1, perPage = 1000).data

    suspend fun categoriesForQuery(name: String?): List<CategoryDto> {
        val page = productRemote.getProducts(name = name?.takeIf { it.isNotBlank() }, page = 1, perPage = 1000)
        val distinct = LinkedHashMap<Long, CategoryDto>()
        page.data.forEach { product ->
            val category = product.category ?: return@forEach
            if (!distinct.containsKey(category.id)) {
                distinct[category.id] = category
            }
        }
        return distinct.values.sortedBy { it.name.lowercase() }
    }

    suspend fun createOrFindCategoryByName(name: String): Long {
        val trimmed = name.trim()
        val found = runCatching {
            categoryRemote.getCategories(name = trimmed, page = 1, perPage = 1)
        }.getOrNull()
        val existing = found?.data?.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            categoryDao.upsert(Category(id = existing.id, name = existing.name))
            return existing.id
        }

        val created = categoryRemote.createCategory(trimmed)
        categoryDao.upsert(Category(id = created.id, name = created.name))
        return created.id
    }

    suspend fun createProduct(
        name: String,
        price: Double,
        unit: String,
        categoryId: Long?,
        lowStockThreshold: Int
    ): Long {
        val metadata = metadataJson(price, unit, lowStockThreshold)
        val dto = productRemote.createProduct(name, categoryId, metadata)
        productDao.upsert(dto.toEntity(price, unit, lowStockThreshold))
        return dto.id
    }

    suspend fun updateProduct(
        id: Long,
        name: String,
        price: Double,
        unit: String,
        categoryId: Long?,
        lowStockThreshold: Int
    ) {
        val metadata = metadataJson(price, unit, lowStockThreshold)
        val dto = productRemote.updateProduct(id, name, categoryId, metadata)
        productDao.upsert(dto.toEntity(price, unit, lowStockThreshold))
    }

    suspend fun deleteProduct(id: Long) {
        productRemote.deleteProduct(id)
        productDao.getById(id)?.let { productDao.delete(it) }
    }

    private fun metadataJson(price: Double, unit: String, lowStockThreshold: Int): JsonObject =
        buildJsonObject {
            put("price", price)
            put("unit", unit)
            if (lowStockThreshold > 0) {
                put("lowStockThreshold", lowStockThreshold)
            }
        }

    private fun ProductDto.toEntity(
        fallbackPrice: Double? = null,
        fallbackUnit: String? = null,
        fallbackThreshold: Int? = null
    ): Product {
        val obj = metadata?.jsonObject
        val price = obj?.get("price")?.jsonPrimitive?.doubleOrNull ?: fallbackPrice ?: 0.0
        val unit = obj?.get("unit")?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { fallbackUnit.orEmpty() }
        val threshold = obj?.get("lowStockThreshold")?.jsonPrimitive?.intOrNull
            ?: fallbackThreshold
            ?: 2
        return Product(
            id = id,
            name = name,
            price = price,
            categoryId = category?.id,
            unit = unit,
            lowStockThreshold = threshold
        )
    }

    companion object {
        private val DEFAULT_CATEGORIES = listOf("Bebidas", "Snacks", "Lacteos")
    }
}
