package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.*
import retrofit2.http.*

interface CatalogService {
    // Categories
    @GET("api/categories") suspend fun getCategories(): List<CategoryDTO>
    @POST("api/categories") suspend fun createCategory(@Body body: CategoryDTO): CategoryDTO
    @PUT("api/categories/{id}") suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryDTO): CategoryDTO

    // Products
    @GET("api/products") suspend fun getProducts(): List<ProductDTO>
    @POST("api/products") suspend fun createProduct(@Body body: ProductDTO): ProductDTO
    @PUT("api/products/{id}") suspend fun updateProduct(@Path("id") id: Long, @Body body: ProductDTO): ProductDTO
}
