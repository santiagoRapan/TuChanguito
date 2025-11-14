package com.example.tuchanguito

import android.app.Application
import com.example.tuchanguito.data.network.CategoryRemoteDataSource
import com.example.tuchanguito.data.network.ProductRemoteDataSource
import com.example.tuchanguito.data.network.ShoppingListsRemoteDataSource
import com.example.tuchanguito.data.network.api.RetrofitClient
import com.example.tuchanguito.data.repository.CategoryRepository
import com.example.tuchanguito.data.repository.ProductRepository
import com.example.tuchanguito.data.repository.ShoppingListsRepository

class MyApplication : Application() {

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
}
