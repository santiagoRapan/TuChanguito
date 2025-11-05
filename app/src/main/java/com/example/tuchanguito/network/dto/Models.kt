package com.example.tuchanguito.network.dto

// DTOs aligned to swagger definitions (simplified)

data class CredentialsDTO(val email: String, val password: String)

data class RegistrationDataDTO(val email: String, val name: String, val surname: String, val password: String, val metadata: Map<String, Any>? = null)

data class AuthenticationTokenDTO(val token: String)

data class VerificationCodeDTO(val code: String)

data class PasswordChangeDTO(val currentPassword: String, val newPassword: String)

data class PasswordRecoveryDTO(val email: String)

data class PasswordResetDTO(val code: String, val password: String)

data class CategoryDTO(val id: Long? = null, val name: String, val metadata: Map<String, Any>? = null)

data class ProductDTO(
    val id: Long? = null,
    val name: String,
    val metadata: Map<String, Any>? = null,
    val category: CategoryDTO? = null
)

// Used for create/update according to swagger ProductRegistrationData
data class ProductRegistrationDTO(
    val name: String,
    val category: IdRef? = null,
    val metadata: Map<String, Any>? = null
)

data class ShoppingListCreateDTO(val name: String, val description: String? = null, val recurring: Boolean? = null, val metadata: Map<String, Any>? = null)

data class ShoppingListDTO(
    val id: Long,
    val name: String,
    val description: String? = null,
    val recurring: Boolean? = null,
    val metadata: Map<String, Any>? = null
)

data class PageDTO<T>(
    val data: List<T>,
    val pagination: PaginationDTO? = null
)

data class PaginationDTO(
    val total: Int? = null,
    val page: Int? = null,
    val per_page: Int? = null,
    val total_pages: Int? = null,
    val has_next: Boolean? = null,
    val has_prev: Boolean? = null
)

data class ListItemDTO(
    val id: Long,
    val quantity: Double,
    val unit: String,
    val purchased: Boolean,
    val product: ProductDTO
)

data class ListItemCreateDTO(
    val product: IdRef,
    val quantity: Double = 1.0,
    val unit: String = "kg",
    val metadata: Map<String, Any>? = null
)

data class IdRef(val id: Long)

// User-related DTOs
data class UserDTO(val id: Long, val name: String, val surname: String?, val email: String?)

data class UserUpdateDTO(val name: String?, val surname: String?)

// Pantry-related DTOs
data class PantryDTO(
    val id: Long,
    val name: String,
    val metadata: Map<String, Any>? = null
)

data class PantryItemDTO(
    val id: Long,
    val quantity: Double,
    val unit: String,
    val metadata: Map<String, Any>? = null,
    val product: ProductDTO
)

data class PantryCreateDTO(
    val name: String,
    val metadata: Map<String, Any>? = null
)

data class PantryItemCreateDTO(
    val product: IdRef,
    val quantity: Double,
    val unit: String,
    val metadata: Map<String, Any>? = null
)

data class PantryItemUpdateDTO(
    val quantity: Double,
    val unit: String,
    val metadata: Map<String, Any>? = null
)

// --- New: shopping list share ---
data class ShareRequestDTO(val email: String)
