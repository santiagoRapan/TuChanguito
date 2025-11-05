package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.*
import retrofit2.http.*

interface PantryService {
    @GET("api/pantries")
    suspend fun getPantries(
        @Query("owner") owner: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
    ): PageDTO<PantryDTO>

    @GET("api/pantries/{id}")
    suspend fun getPantry(@Path("id") id: Long): PantryDTO

    @POST("api/pantries")
    suspend fun createPantry(@Body body: PantryCreateDTO): PantryDTO

    @PUT("api/pantries/{id}")
    suspend fun updatePantry(@Path("id") id: Long, @Body body: PantryCreateDTO): PantryDTO

    @DELETE("api/pantries/{id}")
    suspend fun deletePantry(@Path("id") id: Long)

    // Items
    @GET("api/pantries/{id}/items")
    suspend fun getItems(
        @Path("id") pantryId: Long,
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
    ): PageDTO<PantryItemDTO>

    @POST("api/pantries/{id}/items")
    suspend fun addItem(@Path("id") pantryId: Long, @Body body: PantryItemCreateDTO): PantryItemDTO

    @PUT("api/pantries/{id}/items/{item_id}")
    suspend fun updateItem(@Path("id") pantryId: Long, @Path("item_id") itemId: Long, @Body body: PantryItemUpdateDTO): PantryItemDTO

    @DELETE("api/pantries/{id}/items/{item_id}")
    suspend fun deleteItem(@Path("id") pantryId: Long, @Path("item_id") itemId: Long)
}
