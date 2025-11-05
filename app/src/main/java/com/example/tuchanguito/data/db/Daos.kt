package com.example.tuchanguito.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tuchanguito.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: Long): Flow<User?>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: Category): Long

    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products ORDER BY name")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: ShoppingList): Long

    @Query("SELECT * FROM shopping_lists WHERE archived = 0 ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    fun observeById(id: Long): Flow<ShoppingList?>
}

@Dao
interface ListItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ListItem): Long

    @Update
    suspend fun update(item: ListItem)

    @Delete
    suspend fun delete(item: ListItem)

    @Query("DELETE FROM list_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM list_items WHERE listId = :listId")
    fun observeForList(listId: Long): Flow<List<ListItem>>
}

@Dao
interface PantryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PantryItem): Long

    @Query("SELECT * FROM pantry_items")
    fun observeAll(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE productId = :productId LIMIT 1")
    suspend fun findByProduct(productId: Long): PantryItem?

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
