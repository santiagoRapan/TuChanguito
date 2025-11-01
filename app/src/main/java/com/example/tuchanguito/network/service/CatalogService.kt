package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.*
import retrofit2.http.*
import retrofit2.Response

interface CatalogService {
    // Categories (paginated)
    @GET("api/categories")
    suspend fun getCategories(
        @Query("name") name: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
    ): PageDTO<CategoryDTO>
    @POST("api/categories") suspend fun createCategory(@Body body: CategoryDTO): CategoryDTO
    @PUT("api/categories/{id}") suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryDTO): CategoryDTO

    // Products (paginated)
    @GET("api/products")
    suspend fun getProducts(
        @Query("name") name: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
    ): PageDTO<ProductDTO>
    @GET("api/products/{id}") suspend fun getProduct(@Path("id") id: Long): ProductDTO
    @POST("api/products") suspend fun createProduct(@Body body: ProductRegistrationDTO): ProductDTO
    @PUT("api/products/{id}") suspend fun updateProduct(@Path("id") id: Long, @Body body: ProductRegistrationDTO): Response<Unit>
    @DELETE("api/products/{id}") suspend fun deleteProduct(@Path("id") id: Long)
}
