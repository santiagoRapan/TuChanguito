package com.example.tuchanguito.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginationMetaDto(
    val total: Int,
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_prev") val hasPrev: Boolean
)
