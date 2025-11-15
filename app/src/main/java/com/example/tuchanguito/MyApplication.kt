package com.example.tuchanguito

import android.app.Application
import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.data.db.AppDatabase
import com.example.tuchanguito.data.network.CategoryRemoteDataSource
import com.example.tuchanguito.data.network.AuthRemoteDataSource
import com.example.tuchanguito.data.network.PantryRemoteDataSource
import com.example.tuchanguito.data.network.ProductRemoteDataSource
import com.example.tuchanguito.data.network.ShoppingListsRemoteDataSource
import com.example.tuchanguito.data.network.api.RetrofitClient
import com.example.tuchanguito.data.repository.CategoryRepository
import com.example.tuchanguito.data.repository.AuthRepository
import com.example.tuchanguito.data.repository.PantryRepository
import com.example.tuchanguito.data.repository.ProductRepository
import com.example.tuchanguito.data.repository.ShoppingListHistoryRepository
import com.example.tuchanguito.data.repository.ShoppingListsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class MyApplication : Application() {

    lateinit var preferences: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesManager(this)
        RetrofitClient.configureTokenProvider {
            runCatching { runBlocking { preferences.authToken.firstOrNull() } }.getOrNull()
        }
    }

    private val database: AppDatabase by lazy { AppDatabase.get(this) }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(
            CategoryRemoteDataSource(RetrofitClient.getCategoryApiService())
        )
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(
            ProductRemoteDataSource(RetrofitClient.getProductApiService())
        )
    }

    val shoppingListsRepository: ShoppingListsRepository by lazy {
        ShoppingListsRepository(
            ShoppingListsRemoteDataSource(RetrofitClient.getShoppingListsApiService())
        )
    }

    val pantryRepository: PantryRepository by lazy {
        PantryRepository(
            PantryRemoteDataSource(RetrofitClient.getPantryApiService()),
            preferences
        )
    }

    val shoppingListHistoryRepository: ShoppingListHistoryRepository by lazy {
        ShoppingListHistoryRepository(database.shoppingListDao())
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            AuthRemoteDataSource(RetrofitClient.getAuthApiService()),
            preferences
        )
    }
}
