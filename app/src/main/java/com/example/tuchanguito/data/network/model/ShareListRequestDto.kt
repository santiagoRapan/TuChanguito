package com.example.tuchanguito.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ShareListRequestDto(
    val email: String
)

@Serializable
data class PurchaseListRequestDto(
    val metadata: JsonObject? = null
)

@Serializable
data class ToggleItemRequestDto(
    val purchased: Boolean
)
