package com.example.tuchanguito.data

import android.content.Context
import com.example.tuchanguito.data.db.AppDatabase
import com.example.tuchanguito.data.model.*
import com.example.tuchanguito.network.ApiModule
import com.example.tuchanguito.network.dto.CredentialsDTO
import com.example.tuchanguito.network.dto.RegistrationDataDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Simple repository coordinating Room DAOs for the app features.
 */
class AppRepository private constructor(context: Context){
    private val db = AppDatabase.get(context)
    private val userDao = db.userDao()
    private val productDao = db.productDao()
    private val categoryDao = db.categoryDao()
    private val listDao = db.shoppingListDao()
    private val itemDao = db.listItemDao()
    private val pantryDao = db.pantryDao()
    private val api = ApiModule(context)
    private val prefs = PreferencesManager(context)

    // Auth (local)
    suspend fun register(email: String, password: String, displayName: String): Result<Unit> = try {
        // Remote first (adjust names/surnames as needed)
        val names = displayName.split(" ")
        val name = names.firstOrNull() ?: displayName
        val surname = names.drop(1).joinToString(" ")
        api.auth.register(RegistrationDataDTO(email = email, name = name, surname = surname, password = password))
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    suspend fun verifyAccount(email: String, code: String): Result<Unit> = try {
        api.auth.verify(com.example.tuchanguito.network.dto.VerificationCodeDTO(code))
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    suspend fun login(email: String, password: String): Result<Unit> = try {
        val token = api.auth.login(CredentialsDTO(email, password)).token
        prefs.setAuthToken(token)
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    suspend fun changePassword(email: String, old: String, new: String): Result<Unit> = try {
        api.auth.changePassword(com.example.tuchanguito.network.dto.PasswordChangeDTO(currentPassword = old, newPassword = new))
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    // Products
    fun categories(): Flow<List<Category>> = categoryDao.observeAll()
    fun products(): Flow<List<Product>> = productDao.observeAll()
    suspend fun upsertCategory(name: String) = categoryDao.upsert(Category(name = name))
    suspend fun upsertProduct(product: Product) = productDao.upsert(product)
    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // Lists
    fun activeLists(): Flow<List<ShoppingList>> = listDao.observeActive()
    fun listById(id: Long): Flow<ShoppingList?> = listDao.observeById(id)
    suspend fun createList(title: String): Long = listDao.upsert(ShoppingList(title = title))
    fun itemsForList(listId: Long): Flow<List<ListItem>> = itemDao.observeForList(listId)
    suspend fun addItem(listId: Long, productId: Long, quantity: Int = 1) = itemDao.upsert(ListItem(listId = listId, productId = productId, quantity = quantity))
    suspend fun updateItem(item: ListItem) = itemDao.update(item)
    suspend fun removeItem(item: ListItem) = itemDao.delete(item)

    // Pantry
    fun pantry(): Flow<List<PantryItem>> = pantryDao.observeAll()
    suspend fun upsertPantry(item: PantryItem) = pantryDao.upsert(item)

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun get(context: Context): AppRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
