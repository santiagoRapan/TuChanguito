package com.example.tuchanguito.network.service

import com.example.tuchanguito.network.dto.ListItemDTO
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path

/** Separate interface to issue PATCH calls for shopping list items. */
interface ShoppingPatchService {
    @PATCH("api/shopping-lists/{id}/items/{item_id}")
    suspend fun togglePurchased(
        @Path("id") listId: Long,
        @Path("item_id") itemId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ListItemDTO
}

