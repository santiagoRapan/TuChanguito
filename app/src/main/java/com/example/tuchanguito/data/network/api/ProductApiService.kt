package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.data.network.model.CreateProductRequestDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.ProductDto
import com.example.tuchanguito.data.network.model.UpdateProductRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {

    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
        @Query("name") name: String? = null,
        @Query("category_id") categoryId: Long? = null
    ): PaginatedResponseDto<ProductDto>

    @GET("api/products/{id}")
    suspend fun getProduct(@Path("id") id: Long): ProductDto

    @POST("api/products")
    suspend fun createProduct(@Body body: CreateProductRequestDto): ProductDto

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body body: UpdateProductRequestDto
    ): ProductDto

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long)
}
