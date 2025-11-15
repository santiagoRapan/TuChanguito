package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.data.network.model.CreateListItemRequestDto
import com.example.tuchanguito.data.network.model.CreateShoppingListRequestDto
import com.example.tuchanguito.data.network.model.ListItemDto
import com.example.tuchanguito.data.network.model.ListItemEnvelopeDto
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.PurchaseListRequestDto
import com.example.tuchanguito.data.network.model.ShareListRequestDto
import com.example.tuchanguito.data.network.model.SharedUserDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.data.network.model.ToggleItemRequestDto
import com.example.tuchanguito.data.network.model.UpdateShoppingListRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ShoppingListsApiService {

    // Lists
    @GET("api/shopping-lists")
    suspend fun getLists(
        @Query("name") name: String? = null,
        @Query("owner") owner: Boolean? = null,
        @Query("recurring") recurring: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): PaginatedResponseDto<ShoppingListDto>

    @GET("api/shopping-lists/{id}")
    suspend fun getList(@Path("id") id: Long): ShoppingListDto

    @POST("api/shopping-lists")
    suspend fun createList(@Body body: CreateShoppingListRequestDto): ShoppingListDto

    @PUT("api/shopping-lists/{id}")
    suspend fun updateList(
        @Path("id") id: Long,
        @Body body: UpdateShoppingListRequestDto
    ): ShoppingListDto

    @DELETE("api/shopping-lists/{id}")
    suspend fun deleteList(@Path("id") id: Long)

    // Actions
    @POST("api/shopping-lists/{id}/purchase")
    suspend fun purchaseList(
        @Path("id") id: Long,
        @Body body: PurchaseListRequestDto = PurchaseListRequestDto()
    ): ShoppingListDto

    @POST("api/shopping-lists/{id}/reset")
    suspend fun resetList(@Path("id") id: Long): ShoppingListDto

    @POST("api/shopping-lists/{id}/move-to-pantry")
    suspend fun moveListToPantry(@Path("id") id: Long): retrofit2.Response<kotlin.Unit>

    // Share
    @POST("api/shopping-lists/{id}/share")
    suspend fun shareList(
        @Path("id") id: Long,
        @Body body: ShareListRequestDto
    ): ShoppingListDto

    @GET("api/shopping-lists/{id}/shared-users")
    suspend fun getSharedUsers(@Path("id") id: Long): List<SharedUserDto>

    @DELETE("api/shopping-lists/{id}/share/{user_id}")
    suspend fun revokeShare(
        @Path("id") id: Long,
        @Path("user_id") userId: Long
    )

    // Items
    @GET("api/shopping-lists/{id}/items")
    suspend fun getItems(
        @Path("id") id: Long,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): PaginatedResponseDto<ListItemDto>

    @POST("api/shopping-lists/{id}/items")
    suspend fun addItem(
        @Path("id") listId: Long,
        @Body body: CreateListItemRequestDto
    ): ListItemEnvelopeDto

    @PUT("api/shopping-lists/{id}/items/{item_id}")
    suspend fun updateItem(
        @Path("id") listId: Long,
        @Path("item_id") itemId: Long,
        @Body body: CreateListItemRequestDto
    ): ListItemDto

    @PATCH("api/shopping-lists/{id}/items/{item_id}")
    suspend fun toggleItem(
        @Path("id") listId: Long,
        @Path("item_id") itemId: Long,
        @Body body: ToggleItemRequestDto
    ): ListItemDto

    @DELETE("api/shopping-lists/{id}/items/{item_id}")
    suspend fun deleteItem(
        @Path("id") listId: Long,
        @Path("item_id") itemId: Long
    )
}
