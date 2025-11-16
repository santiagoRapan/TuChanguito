package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.db.ShoppingListDao
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.data.network.model.ShoppingListDto
import kotlinx.coroutines.flow.Flow

class ShoppingListsLocalRepository(
    private val shoppingListDao: ShoppingListDao,
    private val remote: ShoppingListsRepository
) {

    fun observeLists(): Flow<List<ShoppingList>> = shoppingListDao.observeActive()

    suspend fun refreshLists(): Result<Unit> = runCatching {
        // Traemos todas las listas accesibles para el usuario (propias y compartidas)
        val page = remote.getLists(owner = null, perPage = 200)
        val entities = page.data.map { it.toEntity() }
        shoppingListDao.clearActive()
        entities.forEach { shoppingListDao.upsert(it) }
    }

    suspend fun createList(title: String): Long {
        if (existsListWithName(title)) {
            throw IllegalStateException("Ya existe una lista con ese nombre")
        }
        val dto = remote.createList(name = title, description = "", recurring = false, metadata = null)
        shoppingListDao.upsert(dto.toEntity())
        return dto.id
    }

    suspend fun renameList(id: Long, newTitle: String) {
        if (existsListWithName(newTitle)) {
            throw IllegalStateException("Ya existe una lista con ese nombre")
        }
        val updated = remote.updateList(id, name = newTitle)
        shoppingListDao.upsert(updated.toEntity())
    }

    suspend fun deleteList(id: Long) {
        remote.deleteList(id)
        shoppingListDao.deleteById(id)
    }

    suspend fun setRecurring(id: Long, recurring: Boolean) {
        val updated = remote.updateList(id, recurring = recurring)
        shoppingListDao.upsert(updated.toEntity())
    }

    private suspend fun existsListWithName(name: String): Boolean = runCatching {
        // Sólo validamos nombres contra listas propias del usuario
        remote.getLists(name = name, owner = true, page = 1, perPage = 1).data.any {
            it.name.equals(name, ignoreCase = true)
        }
    }.getOrDefault(false)

    private fun ShoppingListDto.toEntity(): ShoppingList =
        ShoppingList(
            id = id,
            title = name,
            ownerUserId = owner?.id,
            archived = false,
            recurring = recurring
        )
}
