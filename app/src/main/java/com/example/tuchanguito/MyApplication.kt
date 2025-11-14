package com.example.tuchanguito

import android.app.Application
import com.example.tuchanguito.data.network.CategoryRemoteDataSource
import com.example.tuchanguito.data.network.ProductRemoteDataSource
import com.example.tuchanguito.data.network.api.RetrofitClient
import com.example.tuchanguito.data.repository.CategoryRepository
import com.example.tuchanguito.data.repository.ProductRepository

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
}
