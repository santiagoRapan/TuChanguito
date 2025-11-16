package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.PurchaseDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PurchasesApiService {

    @GET("api/purchases")
    suspend fun getPurchases(
        @Query("list_id") listId: Long? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): PaginatedResponseDto<PurchaseDto>

    @GET("api/purchases/{id}")
    suspend fun getPurchase(@Path("id") id: Long): PurchaseDto

    @POST("api/purchases/{id}/restore")
    suspend fun restorePurchase(@Path("id") id: Long): ShoppingListDto
}
