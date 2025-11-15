package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.db.ShoppingListDao
import com.example.tuchanguito.data.model.ShoppingList
import kotlinx.coroutines.flow.Flow

class ShoppingListHistoryRepository(
    private val shoppingListDao: ShoppingListDao
) {

    fun observeHistory(): Flow<List<ShoppingList>> = shoppingListDao.observeArchived()

    suspend fun save(id: Long, title: String) {
        val entity = ShoppingList(
            id = id,
            title = title,
            archived = true
        )
        shoppingListDao.upsert(entity)
    }
}
