package com.example.tuchanguito.data.repository

import com.example.tuchanguito.data.network.PurchasesRemoteDataSource
import com.example.tuchanguito.data.network.model.PurchaseDto
import com.example.tuchanguito.data.network.model.ShoppingListDto

class ShoppingListHistoryRepository(
    private val remote: PurchasesRemoteDataSource
) {

    suspend fun getHistory(
        page: Int = 1,
        perPage: Int = 20,
        sortBy: String? = "createdAt",
        order: String? = "DESC"
    ): List<PurchaseDto> = remote.getPurchases(
        page = page,
        perPage = perPage,
        sortBy = sortBy,
        order = order
    ).data

    suspend fun getPurchase(purchaseId: Long): PurchaseDto = remote.getPurchase(purchaseId)

    suspend fun restorePurchase(purchaseId: Long): ShoppingListDto = remote.restorePurchase(purchaseId)
}
