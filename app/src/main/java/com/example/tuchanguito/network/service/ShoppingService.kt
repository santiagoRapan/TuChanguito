package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.*
import retrofit2.http.*

interface ShoppingService {
    // Lists
    @GET("api/shopping-lists") suspend fun getLists(): List<ShoppingListDTO>
    @POST("api/shopping-lists") suspend fun createList(@Body body: ShoppingListCreateDTO): ShoppingListDTO
    @PUT("api/shopping-lists/{id}") suspend fun updateList(@Path("id") id: Long, @Body body: ShoppingListCreateDTO): ShoppingListDTO
    @DELETE("api/shopping-lists/{id}") suspend fun deleteList(@Path("id") id: Long)

    // Items
    @GET("api/shopping-lists/{id}/items") suspend fun getItems(@Path("id") listId: Long): List<ListItemDTO>
    @POST("api/shopping-lists/{id}/items") suspend fun addItem(@Path("id") listId: Long, @Body body: ListItemCreateDTO): ListItemDTO
    @PUT("api/shopping-lists/{id}/items/{item_id}") suspend fun updateItem(@Path("id") listId: Long, @Path("item_id") itemId: Long, @Body body: ListItemCreateDTO): ListItemDTO
    @DELETE("api/shopping-lists/{id}/items/{item_id}") suspend fun deleteItem(@Path("id") listId: Long, @Path("item_id") itemId: Long)
}
