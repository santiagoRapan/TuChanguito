package com.example.tuchanguito.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ListItemDto(
    val id: Long,
    val quantity: Double,
    val unit: String? = null,
    val metadata: JsonObject? = null,
    val purchased: Boolean,
    @SerialName("lastPurchasedAt") val lastPurchasedAt: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    val product: ProductDto
)

@Serializable
data class CreateListItemRequestDto(
    @SerialName("product") val product: ProductReferenceDto,
    val quantity: Double = 1.0,
    val unit: String = "u",
    val metadata: JsonObject? = null
)

@Serializable
data class ProductReferenceDto(
    val id: Long
)
