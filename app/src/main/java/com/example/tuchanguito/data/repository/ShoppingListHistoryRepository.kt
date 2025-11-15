package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.db.ShoppingListDao
import com.example.tuchanguito.data.db.ListItemDao
import com.example.tuchanguito.data.db.ProductDao
import com.example.tuchanguito.data.db.CategoryDao
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.model.ListItemDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShoppingListHistoryRepository(
    private val shoppingListDao: ShoppingListDao,
    private val listItemDao: ListItemDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) {

    fun observeHistory(): Flow<List<ShoppingList>> = shoppingListDao.observeArchived()

    suspend fun save(id: Long, title: String, items: List<ListItemDto>) {
        val entity = ShoppingList(
            id = id,
            title = title,
            archived = true
        )
        // Save the shopping list header
        shoppingListDao.upsert(entity)

        // Save products locally (best-effort) and items
        items.forEach { dto ->
            val p = dto.product
            val prodEntity = Product(
                id = p.id,
                name = p.name,
                price = 0.0,
                categoryId = p.category?.id,
                unit = ""
            )
            try {
                productDao.upsert(prodEntity)
            } catch (_: Throwable) {
                // best effort: ignore failures to persist product
            }

            val itemEntity = ListItem(
                id = dto.id,
                listId = id,
                productId = dto.product.id,
                quantity = dto.quantity.toInt(),
                acquired = dto.purchased
            )
            try {
                listItemDao.upsert(itemEntity)
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    suspend fun getArchivedList(listId: Long): ShoppingList? {
        return shoppingListDao.observeById(listId).first()
    }

    suspend fun getItemsForList(listId: Long): List<ListItem> {
        return listItemDao.observeForList(listId).first()
    }

    suspend fun getProductById(productId: Long): Product? = productDao.getById(productId)

    suspend fun getCategoryName(categoryId: Long?): String? = categoryId?.let { id ->
        try { categoryDao.getById(id)?.name } catch (_: Throwable) { null }
    }
}
