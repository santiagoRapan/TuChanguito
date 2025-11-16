package com.example.tuchanguito.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PurchaseDto(
    val id: Long,
    val metadata: JsonElement? = null,
    val owner: UserSummaryDto? = null,
    val list: ShoppingListDto,
    val items: List<ListItemDto> = emptyList(),
    @SerialName("createdAt") val createdAt: String
)
