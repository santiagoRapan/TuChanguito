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
import com.example.tuchanguito.network.dto.ListItemDTO
import com.example.tuchanguito.network.dto.PantryItemDTO
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
        // Clear local snapshot before re-hydrating to avoid ghost data after API switch
        categoryDao.clearAll()
        productDao.clearAll()
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
        // Resolve category: if id came from local DB but doesn’t exist remotely, create it first
        val finalCatId = if (categoryId != null) {
            // Try to fetch remote category; if missing, create by local name fallback
            val remoteOk = runCatching { api.catalog.getCategories(page = 1, perPage = 1000) }.getOrNull()?.data?.any { it.id == categoryId } == true
            if (remoteOk) categoryId else {
                // Try to read local name and recreate remotely
                val localName = runCatching { categoryDao.getById(categoryId)?.name }.getOrNull()
                if (!localName.isNullOrBlank()) createOrFindCategoryByName(localName) else categoryId
            }
        } else null

        val resp = api.catalog.createProduct(
            ProductRegistrationDTO(
                name = name,
                category = finalCatId?.let { IdRef(it) },
                metadata = mapOf("price" to price, "unit" to unit)
            )
        )
        if (!resp.isSuccessful) {
            // Attempt to find existing product with same name (409 Conflict case)
            val page = runCatching { api.catalog.getProducts(name = name, page = 1, perPage = 1) }.getOrNull()
            val existing = page?.data?.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                val exId = existing.id ?: 0L
                val pPrice = (existing.metadata?.get("price") as? Number)?.toDouble() ?: price
                val pUnit = (existing.metadata?.get("unit") as? String).orEmpty().ifBlank { unit }
                productDao.upsert(Product(id = exId, name = existing.name, price = pPrice, categoryId = existing.category?.id, unit = pUnit))
                return exId
            }
            throw IllegalStateException("Error creando producto: HTTP ${'$'}{resp.code()}")
        }
        // Some servers return minimal/empty body on create; fetch by name to locate the created product
        val page = runCatching { api.catalog.getProducts(name = name, page = 1, perPage = 1) }.getOrNull()
        val dto = page?.data?.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: throw IllegalStateException("Producto creado pero no encontrado por nombre")
        val pId = dto.id ?: 0L
        val pCatId = dto.category?.id
        val pPrice = (dto.metadata?.get("price") as? Number)?.toDouble() ?: price
        val pUnit = (dto.metadata?.get("unit") as? String).orEmpty().ifBlank { unit }
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
        if (!resp.isSuccessful) {
            throw IllegalStateException("Error actualizando producto: HTTP ${'$'}{resp.code()}")
        }
        // Regardless of body, fetch latest from server to ensure final state matches backend rules
        val server = runCatching { api.catalog.getProduct(id) }.getOrNull()
        val pCatId = server?.category?.id ?: categoryId
        val pPrice = (server?.metadata?.get("price") as? Number)?.toDouble() ?: price
        val pUnit = (server?.metadata?.get("unit") as? String).orEmpty().ifBlank { unit }
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

    private suspend fun fetchListItemsEither(listId: Long): List<ListItemDTO> {
        // Try plain array first
        return runCatching { api.shopping.getItems(listId) }.getOrElse {
            // Fallback to page-wrapped structure
            runCatching { api.shopping.getItemsPage(listId).data }.getOrElse { emptyList() }
        }
    }

    suspend fun fetchListItemsRemote(listId: Long): List<ListItemDTO> = fetchListItemsEither(listId)

    // --- New: API-backed list item operations ---
    suspend fun syncListItems(listId: Long) {
        // Ensure list exists locally (FK for list_items)
        runCatching { api.shopping.getList(listId) }.onSuccess { dto ->
            listDao.upsert(ShoppingList(id = dto.id, title = dto.name))
        }
        val remoteItems = fetchListItemsEither(listId)
        remoteItems.forEach { li ->
            val p = li.product
            val price = (p.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
            val unitFromServer = (p.metadata?.get("unit") as? String).orEmpty()
            val existing = runCatching { productDao.getById(p.id ?: 0L) }.getOrNull()
            var finalUnit = unitFromServer.ifBlank { existing?.unit ?: "" }
            var finalCatId = p.category?.id ?: existing?.categoryId
            // Hydrate missing category/unit from product endpoint when needed
            if ((finalCatId == null || finalUnit.isBlank()) && (p.id != null)) {
                runCatching { api.catalog.getProduct(p.id!!) }.onSuccess { full ->
                    finalCatId = full.category?.id ?: finalCatId
                    val u = (full.metadata?.get("unit") as? String).orEmpty()
                    if (finalUnit.isBlank() && u.isNotBlank()) finalUnit = u
                }
            }
            p.category?.let { categoryDto ->
                categoryDao.upsert(Category(id = categoryDto.id ?: 0L, name = categoryDto.name))
            }
            productDao.upsert(Product(id = p.id ?: 0L, name = p.name, price = price, categoryId = finalCatId, unit = finalUnit))
            itemDao.upsert(
                ListItem(
                    id = li.id,
                    listId = listId,
                    productId = p.id ?: 0L,
                    quantity = li.quantity.toInt(),
                    acquired = li.purchased
                )
            )
        }
    }

    suspend fun addItemRemote(listId: Long, productId: Long, quantity: Int = 1, unit: String = "u"): Long {
        return try {
            val resp = api.shopping.addItem(listId, com.example.tuchanguito.network.dto.ListItemCreateDTO(product = IdRef(productId), quantity = quantity.toDouble(), unit = unit))
            if (!resp.isSuccessful) throw retrofit2.HttpException(resp)
            runCatching { api.shopping.getList(listId) }.onSuccess { dto ->
                listDao.upsert(ShoppingList(id = dto.id, title = dto.name))
            }
            val remoteItems = fetchListItemsEither(listId)
            remoteItems.forEach { li ->
                val p = li.product
                val price = (p.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
                val unitFromServer = (p.metadata?.get("unit") as? String).orEmpty()
                val existing = runCatching { productDao.getById(p.id ?: 0L) }.getOrNull()
                var finalUnit = unitFromServer.ifBlank { existing?.unit ?: "" }
                var finalCatId = p.category?.id ?: existing?.categoryId
                if ((finalCatId == null || finalUnit.isBlank()) && (p.id != null)) {
                    runCatching { api.catalog.getProduct(p.id!!) }.onSuccess { full ->
                        finalCatId = full.category?.id ?: finalCatId
                        val u = (full.metadata?.get("unit") as? String).orEmpty()
                        if (finalUnit.isBlank() && u.isNotBlank()) finalUnit = u
                    }
                }
                p.category?.let { c -> categoryDao.upsert(Category(id = c.id ?: 0L, name = c.name)) }
                productDao.upsert(Product(id = p.id ?: 0L, name = p.name, price = price, categoryId = finalCatId, unit = finalUnit))
                itemDao.upsert(ListItem(id = li.id, listId = listId, productId = p.id ?: 0L, quantity = li.quantity.toInt(), acquired = li.purchased))
            }
            remoteItems.firstOrNull { it.product.id == productId }?.id
                ?: remoteItems.maxByOrNull { it.id }?.id
                ?: 0L
        } catch (t: Throwable) {
            val code = (t as? retrofit2.HttpException)?.code()
            if (code == 409) {
                val remoteItems = runCatching { fetchListItemsEither(listId) }.getOrDefault(emptyList())
                val existing = remoteItems.firstOrNull { it.product.id == productId }
                if (existing != null) {
                    val newQty = existing.quantity.toInt() + quantity
                    val chosenUnit = existing.unit.ifBlank { unit }
                    updateItemQuantityRemote(listId, existing.id, productId, newQty, chosenUnit)
                    existing.id
                } else {
                    throw t
                }
            } else {
                throw t
            }
        }
    }

    suspend fun updateItemQuantityRemote(listId: Long, itemId: Long, productId: Long, newQuantity: Int, unit: String = "u") {
        val updated = api.shopping.updateItem(listId, itemId, com.example.tuchanguito.network.dto.ListItemCreateDTO(product = IdRef(productId), quantity = newQuantity.toDouble(), unit = unit))
        val p = updated.product
        val existing = runCatching { productDao.getById(p.id ?: productId) }.getOrNull()
        var price = (p.metadata?.get("price") as? Number)?.toDouble() ?: (existing?.price ?: 0.0)
        var unitFromServer = (p.metadata?.get("unit") as? String).orEmpty()
        var finalUnit = unitFromServer.ifBlank { existing?.unit ?: unit }
        var finalCatId = p.category?.id ?: existing?.categoryId
        if ((finalCatId == null || finalUnit.isBlank()) && (p.id != null)) {
            runCatching { api.catalog.getProduct(p.id!!) }.onSuccess { full ->
                finalCatId = full.category?.id ?: finalCatId
                val u = (full.metadata?.get("unit") as? String).orEmpty()
                if (finalUnit.isBlank() && u.isNotBlank()) finalUnit = u
                val pr = (full.metadata?.get("price") as? Number)?.toDouble()
                if (pr != null) price = pr
            }
        }
        p.category?.let { c -> categoryDao.upsert(Category(id = c.id ?: 0L, name = c.name)) }
        productDao.upsert(Product(id = p.id ?: productId, name = p.name, price = price, categoryId = finalCatId, unit = finalUnit))
        itemDao.upsert(
            ListItem(
                id = updated.id,
                listId = listId,
                productId = updated.product.id ?: productId,
                quantity = updated.quantity.toInt(),
                acquired = updated.purchased
            )
        )
    }

    suspend fun toggleItemPurchasedRemote(listId: Long, itemId: Long, purchased: Boolean) {
        val updated = api.shoppingPatch.togglePurchased(listId, itemId, mapOf("purchased" to purchased))
        val p = updated.product
        val pId = p.id ?: 0L
        val existing = runCatching { productDao.getById(pId) }.getOrNull()
        var price = (p.metadata?.get("price") as? Number)?.toDouble() ?: (existing?.price ?: 0.0)
        var unitFromServer = (p.metadata?.get("unit") as? String).orEmpty()
        var finalUnit = unitFromServer.ifBlank { existing?.unit ?: "" }
        var finalCatId = p.category?.id ?: existing?.categoryId
        if ((finalCatId == null || finalUnit.isBlank()) && (p.id != null)) {
            runCatching { api.catalog.getProduct(p.id!!) }.onSuccess { full ->
                finalCatId = full.category?.id ?: finalCatId
                val u = (full.metadata?.get("unit") as? String).orEmpty()
                if (finalUnit.isBlank() && u.isNotBlank()) finalUnit = u
                val pr = (full.metadata?.get("price") as? Number)?.toDouble()
                if (pr != null) price = pr
            }
        }
        p.category?.let { c -> categoryDao.upsert(Category(id = c.id ?: 0L, name = c.name)) }
        productDao.upsert(Product(id = pId, name = p.name, price = price, categoryId = finalCatId, unit = finalUnit))
        itemDao.upsert(ListItem(id = updated.id, listId = listId, productId = pId, quantity = updated.quantity.toInt(), acquired = updated.purchased))
    }

    suspend fun deleteItemRemote(listId: Long, itemId: Long, local: ListItem? = null) {
        // Delete from server; ignore body
        runCatching { api.shopping.deleteItem(listId, itemId) }
            .onFailure { throw it }
        // Remove local row by id (or provided entity) to keep Room in sync immediately
        if (local != null) {
            runCatching { itemDao.delete(local) }
        }
        runCatching { itemDao.deleteById(itemId) }
        // Optionally refresh items from server to ensure correct state (quantities of other items may change)
        runCatching { syncListItems(listId) }
    }

    suspend fun deleteItemByIdLocal(id: Long) = itemDao.deleteById(id)

    // Local-only fallbacks preserved
    suspend fun addItem(listId: Long, productId: Long, quantity: Int = 1) = itemDao.upsert(ListItem(listId = listId, productId = productId, quantity = quantity))
    suspend fun updateItem(item: ListItem) = itemDao.update(item)
    suspend fun removeItem(item: ListItem) = itemDao.delete(item)

    // Pantry
    fun pantry(): Flow<List<PantryItem>> = pantryDao.observeAll()

    suspend fun ensureDefaultPantryId(): Long {
        // Try to get pantries; if none, create one named "Alacena"
        val page = runCatching { api.pantry.getPantries(owner = true, page = 1, perPage = 1) }.getOrNull()
        val existing = page?.data?.firstOrNull()
        if (existing != null) return existing.id
        val created = api.pantry.createPantry(com.example.tuchanguito.network.dto.PantryCreateDTO(name = "Alacena"))
        return created.id
    }

    suspend fun syncPantry(search: String? = null, categoryId: Long? = null) {
        val pantryId = ensureDefaultPantryId()
        val page = runCatching { api.pantry.getItems(pantryId, search = search, categoryId = categoryId, page = 1, perPage = 1000) }.getOrNull()
        val items = page?.data.orEmpty()
        // Upsert products/categories and mirror pantry items
        items.forEach { it ->
            val p = it.product
            val price = (p.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
            val unitMeta = (p.metadata?.get("unit") as? String).orEmpty()
            val catId = p.category?.id
            p.category?.let { c -> categoryDao.upsert(Category(id = c.id ?: 0L, name = c.name)) }
            productDao.upsert(Product(id = p.id ?: 0L, name = p.name, price = price, categoryId = catId, unit = unitMeta))
            pantryDao.upsert(PantryItem(id = it.id, productId = p.id ?: 0L, quantity = it.quantity.toInt()))
        }
    }

    suspend fun addPantryItem(productId: Long, quantity: Int, unit: String = "u") {
        val pantryId = ensureDefaultPantryId()
        val created = api.pantry.addItem(pantryId, com.example.tuchanguito.network.dto.PantryItemCreateDTO(product = com.example.tuchanguito.network.dto.IdRef(productId), quantity = quantity.toDouble(), unit = unit))
        // Upsert related product/category then mirror item
        created.product.category?.let { c -> categoryDao.upsert(Category(id = c.id ?: 0L, name = c.name)) }
        val pPrice = (created.product.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
        val pUnit = (created.product.metadata?.get("unit") as? String).orEmpty()
        productDao.upsert(Product(id = created.product.id ?: productId, name = created.product.name, price = pPrice, categoryId = created.product.category?.id, unit = pUnit))
        pantryDao.upsert(PantryItem(id = created.id, productId = created.product.id ?: productId, quantity = created.quantity.toInt()))
    }

    // Add-or-increment pantry item if a row for the same product already exists
    suspend fun addOrIncrementPantryItem(productId: Long, addQuantity: Int, unit: String = "u") {
        // First ensure we have current pantry snapshot locally
        runCatching { syncPantry() }
        val existing = pantryDao.findByProduct(productId)
        if (existing != null) {
            // Update quantity via API using existing item id
            updatePantryItem(existing.id, existing.quantity + addQuantity, unit)
        } else {
            addPantryItem(productId, addQuantity, unit)
        }
    }

    suspend fun updatePantryItem(itemId: Long, quantity: Int, unit: String = "u") {
        val pantryId = ensureDefaultPantryId()
        val updated = api.pantry.updateItem(pantryId, itemId, com.example.tuchanguito.network.dto.PantryItemUpdateDTO(quantity = quantity.toDouble(), unit = unit))
        pantryDao.upsert(PantryItem(id = updated.id, productId = updated.product.id ?: 0L, quantity = updated.quantity.toInt()))
    }

    suspend fun deletePantryItem(itemId: Long) {
        val pantryId = ensureDefaultPantryId()
        runCatching { api.pantry.deleteItem(pantryId, itemId) }
            .onFailure { throw it }
        // Clean local row safely by id
        runCatching { pantryDao.deleteById(itemId) }
        // Refresh to ensure UI consistent
        runCatching { syncPantry() }
    }

    // Validate Credentials
    suspend fun validateCredentials(email: String, password: String): Result<Unit> = try {
        api.auth.login(com.example.tuchanguito.network.dto.CredentialsDTO(email, password))
        Result.success(Unit)
    } catch (t: Throwable) { Result.failure(t) }

    // --- Backend-driven product search helpers for ProductsScreen ---
    suspend fun searchProductsDTO(name: String?, categoryId: Long?): List<ProductDTO> {
        val page = api.catalog.getProducts(name = name?.takeIf { it.isNotBlank() }, categoryId = categoryId, page = 1, perPage = 1000)
        return page.data
    }

    suspend fun categoriesForQuery(name: String?): List<CategoryDTO> {
        val page = api.catalog.getProducts(name = name?.takeIf { it.isNotBlank() }, categoryId = null, page = 1, perPage = 1000)
        val distinct = LinkedHashMap<Long, CategoryDTO>()
        page.data.forEach { p ->
            val c = p.category
            if (c?.id != null && !distinct.containsKey(c.id!!)) distinct[c.id!!] = CategoryDTO(id = c.id, name = c.name, metadata = c.metadata)
        }
        return distinct.values.toList().sortedBy { it.name.lowercase() }
    }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun get(context: Context): AppRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
