package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.data.network.model.CreatePantryItemRequestDto
import com.example.tuchanguito.data.network.model.CreatePantryRequestDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.PantryDto
import com.example.tuchanguito.data.network.model.PantryItemDto
import com.example.tuchanguito.data.network.model.UpdatePantryItemRequestDto
import com.example.tuchanguito.data.network.model.UpdatePantryRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PantryApiService {

    @GET("api/pantries")
    suspend fun getPantries(
        @Query("owner") owner: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): PaginatedResponseDto<PantryDto>

    @GET("api/pantries/{id}")
    suspend fun getPantry(@Path("id") id: Long): PantryDto

    @POST("api/pantries")
    suspend fun createPantry(@Body body: CreatePantryRequestDto): PantryDto

    @PUT("api/pantries/{id}")
    suspend fun updatePantry(
        @Path("id") id: Long,
        @Body body: UpdatePantryRequestDto
    ): PantryDto

    @DELETE("api/pantries/{id}")
    suspend fun deletePantry(@Path("id") id: Long)

    @GET("api/pantries/{id}/items")
    suspend fun getItems(
        @Path("id") pantryId: Long,
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): PaginatedResponseDto<PantryItemDto>

    @GET("api/pantries/{id}/items")
    suspend fun getItemsRaw(
        @Path("id") pantryId: Long,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<ResponseBody>

    @POST("api/pantries/{id}/items")
    suspend fun addItem(
        @Path("id") pantryId: Long,
        @Body body: CreatePantryItemRequestDto
    ): PantryItemDto

    @PUT("api/pantries/{id}/items/{item_id}")
    suspend fun updateItem(
        @Path("id") pantryId: Long,
        @Path("item_id") itemId: Long,
        @Body body: UpdatePantryItemRequestDto
    ): PantryItemDto

    @DELETE("api/pantries/{id}/items/{item_id}")
    suspend fun deleteItem(
        @Path("id") pantryId: Long,
        @Path("item_id") itemId: Long
    )
}
