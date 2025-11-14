package com.example.tuchanguito.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CreateShoppingListRequestDto(
    val name: String,
    val description: String,
    val recurring: Boolean = false,
    val metadata: JsonObject? = null
)

@Serializable
data class UpdateShoppingListRequestDto(
    val name: String? = null,
    val description: String? = null,
    val recurring: Boolean? = null,
    val metadata: JsonObject? = null
)
