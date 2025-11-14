package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.data.network.model.CategoryDto
import com.example.tuchanguito.data.network.model.CreateCategoryRequestDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.UpdateCategoryRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CategoryApiService {

    @GET("api/categories")
    suspend fun getCategories(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
        @Query("name") name: String? = null
    ): PaginatedResponseDto<CategoryDto>

    @GET("api/categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): CategoryDto

    @POST("api/categories")
    suspend fun createCategory(@Body body: CreateCategoryRequestDto): CategoryDto

    @PUT("api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body body: UpdateCategoryRequestDto
    ): CategoryDto

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long)
}
