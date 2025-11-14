package com.example.tuchanguito.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateProductRequestDto(
    val name: String,
    val category: ProductCategoryReferenceDto? = null,
    val metadata: JsonElement? = null
)
