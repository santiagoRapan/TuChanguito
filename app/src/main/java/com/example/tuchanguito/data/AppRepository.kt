package com.example.tuchanguito.data

import android.content.Context
import com.example.tuchanguito.data.db.AppDatabase
import com.example.tuchanguito.data.model.*
import com.example.tuchanguito.network.ApiModule
import com.example.tuchanguito.network.dto.CredentialsDTO
import com.example.tuchanguito.network.dto.RegistrationDataDTO
import com.example.tuchanguito.network.dto.CategoryDTO
import com.example.tuchanguito.network.dto.ProductDTO
import com.example.tuchanguito.network.dto.ProductRegistrationDTO
import com.example.tuchanguito.network.dto.IdRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    // In-memory source of truth for lists fetched from API
    private val _remoteLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val remoteLists = _remoteLists.asStateFlow()

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

    suspend fun changePassword(old: String, new: String): Result<Unit> = try {
        api.auth.changePassword(com.example.tuchanguito.network.dto.PasswordChangeDTO(currentPassword = old, newPassword = new))
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    // Profile
    suspend fun getProfile(): Result<com.example.tuchanguito.network.dto.UserDTO> = try {
        val user = api.auth.getProfile()
        // store current user id
        prefs.setCurrentUserId(user.id)
        Result.success(user)
    } catch (t: Throwable) { Result.failure(t) }

    suspend fun updateProfile(name: String?, surname: String?): Result<com.example.tuchanguito.network.dto.UserDTO> = try {
        val updated = api.auth.updateProfile(com.example.tuchanguito.network.dto.UserUpdateDTO(name = name, surname = surname))
        prefs.setCurrentUserId(updated.id)
        Result.success(updated)
    } catch (t: Throwable) { Result.failure(t) }

    // Products
    fun categories(): Flow<List<Category>> = categoryDao.observeAll()
    fun products(): Flow<List<Product>> = productDao.observeAll()
    suspend fun upsertCategory(name: String) = categoryDao.upsert(Category(name = name))
    suspend fun upsertProduct(product: Product) = productDao.upsert(product)
    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // Catalog sync with API
    suspend fun syncCatalog(): Result<Unit> = try {
        // Categories
        val catPage = api.catalog.getCategories(page = 1, perPage = 1000)
        catPage.data.forEach { c ->
            categoryDao.upsert(Category(id = c.id ?: 0L, name = c.name))
        }
        // Products
        val prodPage = api.catalog.getProducts(page = 1, perPage = 1000)
        prodPage.data.forEach { p ->
            val price = (p.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
            val unit = (p.metadata?.get("unit") as? String).orEmpty()
            val catId = p.category?.id
            productDao.upsert(Product(id = p.id ?: 0L, name = p.name, price = price, categoryId = catId, unit = unit))
        }
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    suspend fun createOrFindCategoryByName(name: String): Long {
        val trimmed = name.trim()
        // Try remote search
        val found = runCatching { api.catalog.getCategories(name = trimmed, page = 1, perPage = 1) }.getOrNull()
        val existing = found?.data?.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            categoryDao.upsert(Category(id = existing.id ?: 0L, name = existing.name))
            return existing.id ?: 0L
        }
        // Create
        val created = api.catalog.createCategory(CategoryDTO(name = trimmed))
        categoryDao.upsert(Category(id = created.id ?: 0L, name = created.name))
        return created.id ?: 0L
    }

    suspend fun createProductRemote(name: String, price: Double, unit: String, categoryId: Long?): Long {
        val dto = api.catalog.createProduct(
            ProductRegistrationDTO(
                name = name,
                category = categoryId?.let { IdRef(it) },
                metadata = mapOf("price" to price, "unit" to unit)
            )
        )
        val pId = dto.id ?: 0L
        val pCatId = dto.category?.id
        val pPrice = (dto.metadata?.get("price") as? Number)?.toDouble() ?: price
        val pUnit = (dto.metadata?.get("unit") as? String).orEmpty()
        productDao.upsert(Product(id = pId, name = dto.name, price = pPrice, categoryId = pCatId, unit = pUnit))
        // Ensure list shows all current remote products
        syncProducts()
        return pId
    }

    suspend fun updateProductRemote(id: Long, name: String, price: Double, unit: String, categoryId: Long?) {
        val resp = api.catalog.updateProduct(
            id,
            ProductRegistrationDTO(
                name = name,
                category = categoryId?.let { IdRef(it) },
                metadata = mapOf("price" to price, "unit" to unit)
            )
        )
        // Regardless of body, fetch latest from server to ensure final state matches backend rules
        val server = runCatching { api.catalog.getProduct(id) }.getOrNull()
        val pCatId = server?.category?.id
        val pPrice = (server?.metadata?.get("price") as? Number)?.toDouble() ?: price
        val pUnit = (server?.metadata?.get("unit") as? String).orEmpty()
        val pName = server?.name ?: name
        productDao.upsert(Product(id = id, name = pName, price = pPrice, categoryId = pCatId, unit = pUnit))
        // Refresh full list to avoid partial state
        syncProducts()
    }

    suspend fun deleteProductRemote(id: Long) {
        api.catalog.deleteProduct(id)
        productDao.getById(id)?.let { productDao.delete(it) }
        // Refresh remote to ensure local matches server after deletion
        syncProducts()
    }

    private suspend fun syncProducts() {
        runCatching { api.catalog.getProducts(page = 1, perPage = 1000) }
            .onSuccess { page ->
                page.data.forEach { p ->
                    val price = (p.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
                    val unit = (p.metadata?.get("unit") as? String).orEmpty()
                    val catId = p.category?.id
                    productDao.upsert(Product(id = p.id ?: 0L, name = p.name, price = price, categoryId = catId, unit = unit))
                }
            }
    }

    // Lists (API-backed)
    fun activeLists(): Flow<List<ShoppingList>> = remoteLists
    fun listById(id: Long): Flow<ShoppingList?> = listDao.observeById(id)
    suspend fun loadListIntoLocal(id: Long) {
        // Fetch remote list and persist into Room so UI detail observes correct title
        runCatching { api.shopping.getList(id) }
            .onSuccess { dto -> listDao.upsert(ShoppingList(id = dto.id, title = dto.name)) }
    }

    suspend fun refreshLists(): Result<Unit> = try {
        val page = api.shopping.getLists()
        val lists = page.data.map { ShoppingList(id = it.id, title = it.name) }
        _remoteLists.value = lists
        Result.success(Unit)
    } catch (t: Throwable) {
        // Keep previous cache and report failure without throwing
        Result.failure(t)
    }

    private suspend fun existsListWithName(name: String): Boolean {
        return try {
            // Ask API filtering by name and owner=true to check uniqueness on server
            val page = api.shopping.getLists(name = name, owner = true, page = 1, perPage = 1)
            page.data.any { it.name.equals(name, ignoreCase = true) }
        } catch (_: Throwable) { false }
    }

    suspend fun createList(title: String): Long {
        // API requires name, description (string) and recurring (boolean)
        // Prevent 409 UNIQUE constraint by checking server-side duplicates first
        if (existsListWithName(title)) throw IllegalStateException("Ya existe una lista con ese nombre")
        val dto = api.shopping.createList(
            com.example.tuchanguito.network.dto.ShoppingListCreateDTO(
                name = title,
                description = "",
                recurring = false,
                metadata = emptyMap()
            )
        )
        // Fetch server version to ensure fields are in sync, then update cache
        val created = runCatching { api.shopping.getList(dto.id) }.getOrNull()
        val model = if (created != null) ShoppingList(id = created.id, title = created.name) else ShoppingList(id = dto.id, title = dto.name)
        _remoteLists.update { it + model }
        // Also persist in Room so ListDetail observes the correct title immediately
        listDao.upsert(model)
        return dto.id
    }

    suspend fun renameList(id: Long, newTitle: String) {
        if (existsListWithName(newTitle)) throw IllegalStateException("Ya existe una lista con ese nombre")
        api.shopping.updateList(id, com.example.tuchanguito.network.dto.ShoppingListCreateDTO(name = newTitle))
        _remoteLists.update { cur -> cur.map { if (it.id == id) it.copy(title = newTitle) else it } }
        // Keep Room in sync so ListDetail shows the updated title
        listDao.upsert(ShoppingList(id = id, title = newTitle))
    }

    suspend fun deleteListRemote(id: Long) {
        api.shopping.deleteList(id)
        _remoteLists.update { cur -> cur.filterNot { it.id == id } }
    }

    fun itemsForList(listId: Long): Flow<List<ListItem>> = itemDao.observeForList(listId)
    suspend fun addItem(listId: Long, productId: Long, quantity: Int = 1) = itemDao.upsert(ListItem(listId = listId, productId = productId, quantity = quantity))
    suspend fun updateItem(item: ListItem) = itemDao.update(item)
    suspend fun removeItem(item: ListItem) = itemDao.delete(item)

    // Pantry
    fun pantry(): Flow<List<PantryItem>> = pantryDao.observeAll()
    suspend fun upsertPantry(item: PantryItem) = pantryDao.upsert(item)

    // Validate Credentials
    suspend fun validateCredentials(email: String, password: String): Result<Unit> = try {
        api.auth.login(com.example.tuchanguito.network.dto.CredentialsDTO(email, password))
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun get(context: Context): AppRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
