package com.example.tuchanguito.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PantryDto(
    val id: Long,
    val name: String,
    val metadata: JsonObject? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class PantryItemDto(
    val id: Long,
    val quantity: Double,
    val unit: String? = null,
    val metadata: JsonObject? = null,
    val product: ProductDto,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class CreatePantryRequestDto(
    val name: String,
    val metadata: JsonObject? = null
)

@Serializable
data class UpdatePantryRequestDto(
    val name: String,
    val metadata: JsonObject? = null
)

@Serializable
data class CreatePantryItemRequestDto(
    val product: ProductReferenceDto,
    val quantity: Double,
    val unit: String,
    val metadata: JsonObject? = null
)

@Serializable
data class UpdatePantryItemRequestDto(
    val quantity: Double,
    val unit: String,
    val metadata: JsonObject? = null
)
