package com.example.tuchanguito.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tuchanguito.data.model.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Category::class, Product::class, ShoppingList::class, ListItem::class, PantryItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun listItemDao(): ListItemDao
    abstract fun pantryDao(): PantryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tuchanguito.db"
            ).addCallback(object: RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed some demo data using coroutines on IO dispatcher
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = get(context)
                            val catId = database.categoryDao().upsert(Category(name = "Bebidas"))
                            val snacksId = database.categoryDao().upsert(Category(name = "Snacks"))
                            database.productDao().upsert(Product(name = "Coca-Cola Zero 2 L", price = 850.0, categoryId = catId))
                            database.productDao().upsert(Product(name = "Papas Lays 330g", price = 3500.0, categoryId = snacksId))
                        } catch (e: Exception) {
                            // swallow exceptions during seeding to avoid crashing on DB create
                        }
                    }
                }
            }).build().also { INSTANCE = it }
        }

        private fun ioThread(block: () -> Unit) = Thread(block).start()
    }
}
