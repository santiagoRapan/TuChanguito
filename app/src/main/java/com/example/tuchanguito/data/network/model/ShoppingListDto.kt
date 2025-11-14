package com.example.tuchanguito.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ShoppingListDto(
    val id: Long,
    val name: String,
    val description: String,
    val recurring: Boolean,
    val metadata: JsonObject? = null,
    val owner: UserSummaryDto? = null,
    @SerialName("sharedWith") val sharedWith: List<UserSummaryDto> = emptyList(),
    @SerialName("lastPurchasedAt") val lastPurchasedAt: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class UserSummaryDto(
    val id: Long,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null
)
