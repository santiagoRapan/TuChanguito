package com.example.tuchanguito.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CredentialsDTO(val email: String, val password: String)

@Serializable
data class RegistrationDataDTO(
    val email: String,
    val name: String,
    val surname: String,
    val password: String,
    val metadata: JsonObject? = null
)

@Serializable
data class AuthenticationTokenDTO(val token: String)

@Serializable
data class VerificationCodeDTO(val code: String)

@Serializable
data class PasswordChangeDTO(val currentPassword: String, val newPassword: String)

@Serializable
data class PasswordRecoveryDTO(val email: String)

@Serializable
data class PasswordResetDTO(val code: String, val password: String)

@Serializable
data class CategoryDTO(val id: Long? = null, val name: String, val metadata: JsonObject? = null)

@Serializable
data class ProductDTO(
    val id: Long? = null,
    val name: String,
    val metadata: JsonObject? = null,
    val category: CategoryDTO? = null
)

@Serializable
data class ProductRegistrationDTO(
    val name: String,
    val category: IdRef? = null,
    val metadata: JsonObject? = null
)

@Serializable
data class ShoppingListCreateDTO(
    val name: String,
    val description: String? = null,
    val recurring: Boolean? = null,
    val metadata: JsonObject? = null
)

@Serializable
data class ShoppingListDTO(
    val id: Long,
    val name: String,
    val description: String? = null,
    val recurring: Boolean? = null,
    val metadata: JsonObject? = null
)

@Serializable
data class PageDTO<T>(
    val data: List<T>,
    val pagination: PaginationDTO? = null
)

@Serializable
data class PaginationDTO(
    val total: Int? = null,
    val page: Int? = null,
    val per_page: Int? = null,
    val total_pages: Int? = null,
    val has_next: Boolean? = null,
    val has_prev: Boolean? = null
)

@Serializable
data class ListItemDTO(
    val id: Long,
    val quantity: Double,
    val unit: String,
    val purchased: Boolean,
    val product: ProductDTO
)

@Serializable
data class ListItemCreateDTO(
    val product: IdRef,
    val quantity: Double = 1.0,
    val unit: String = "kg",
    val metadata: JsonObject? = null
)

@Serializable
data class IdRef(val id: Long)

@Serializable
data class UserDTO(val id: Long, val name: String, val surname: String?, val email: String?)

@Serializable
data class UserUpdateDTO(val name: String?, val surname: String?)

@Serializable
data class PantryDTO(
    val id: Long,
    val name: String,
    val metadata: JsonObject? = null
)

@Serializable
data class PantryItemDTO(
    val id: Long,
    val quantity: Double,
    val unit: String,
    val metadata: JsonObject? = null,
    val product: ProductDTO
)

@Serializable
data class PantryCreateDTO(
    val name: String,
    val metadata: JsonObject? = null
)

@Serializable
data class PantryItemCreateDTO(
    val product: IdRef,
    val quantity: Double,
    val unit: String,
    val metadata: JsonObject? = null
)

@Serializable
data class PantryItemUpdateDTO(
    val quantity: Double,
    val unit: String,
    val metadata: JsonObject? = null
)

@Serializable
data class ShareRequestDTO(val email: String)
