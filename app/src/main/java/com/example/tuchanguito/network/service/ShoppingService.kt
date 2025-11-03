package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.*
import retrofit2.http.*

interface ShoppingService {
    // Lists
    @GET("api/shopping-lists")
    suspend fun getLists(
        @Query("name") name: String? = null,
        @Query("owner") owner: Boolean? = null,
        @Query("recurring") recurring: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null,
    ): PageDTO<ShoppingListDTO>

    @GET("api/shopping-lists/{id}") suspend fun getList(@Path("id") id: Long): ShoppingListDTO
    @POST("api/shopping-lists") suspend fun createList(@Body body: ShoppingListCreateDTO): ShoppingListDTO
    @PUT("api/shopping-lists/{id}") suspend fun updateList(@Path("id") id: Long, @Body body: ShoppingListCreateDTO): ShoppingListDTO
    @DELETE("api/shopping-lists/{id}") suspend fun deleteList(@Path("id") id: Long)

    // Items
    @GET("api/shopping-lists/{id}/items") suspend fun getItems(@Path("id") listId: Long): List<ListItemDTO>

    // Fallback: some servers may wrap the array in an object with `data`
    @GET("api/shopping-lists/{id}/items") suspend fun getItemsPage(@Path("id") listId: Long): PageDTO<ListItemDTO>

    @POST("api/shopping-lists/{id}/items") suspend fun addItem(@Path("id") listId: Long, @Body body: ListItemCreateDTO): ListItemDTO
    @PUT("api/shopping-lists/{id}/items/{item_id}") suspend fun updateItem(@Path("id") listId: Long, @Path("item_id") itemId: Long, @Body body: ListItemCreateDTO): ListItemDTO
    @DELETE("api/shopping-lists/{id}/items/{item_id}") suspend fun deleteItem(@Path("id") listId: Long, @Path("item_id") itemId: Long)
}
