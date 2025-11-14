package com.example.tuchanguito.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UpdateCategoryRequestDto(
    val name: String,
    val metadata: JsonElement? = null
)
