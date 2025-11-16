package com.example.tuchanguito.data.network

import com.example.tuchanguito.data.network.api.PurchasesApiService
import com.example.tuchanguito.data.network.core.RemoteDataSource
import com.example.tuchanguito.data.network.model.PaginatedResponseDto
import com.example.tuchanguito.data.network.model.PurchaseDto
import com.example.tuchanguito.data.network.model.ShoppingListDto

class PurchasesRemoteDataSource(
    private val api: PurchasesApiService
) : RemoteDataSource() {

    suspend fun getPurchases(
        listId: Long? = null,
        page: Int? = null,
        perPage: Int? = null,
        sortBy: String? = null,
        order: String? = null
    ): PaginatedResponseDto<PurchaseDto> = safeApiCall {
        api.getPurchases(listId, page, perPage, sortBy, order)
    }

    suspend fun getPurchase(id: Long): PurchaseDto = safeApiCall { api.getPurchase(id) }

    suspend fun restorePurchase(id: Long): ShoppingListDto = safeApiCall { api.restorePurchase(id) }
}
