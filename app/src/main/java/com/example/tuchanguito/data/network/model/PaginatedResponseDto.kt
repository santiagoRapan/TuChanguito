package com.example.tuchanguito.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponseDto<T>(
    val data: List<T>,
    val pagination: PaginationMetaDto
)
