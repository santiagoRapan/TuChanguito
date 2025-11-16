package com.example.tuchanguito.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val password: String,
    val verified: Boolean = false,
    val displayName: String = ""
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double = 0.0,
    val categoryId: Long? = null,
    val unit: String = "",
    val lowStockThreshold: Int = 2
)

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val ownerUserId: Long? = null,
    val archived: Boolean = false,
    val recurring: Boolean = false
)

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(entity = ShoppingList::class, parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Product::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("listId"), Index("productId")]
)
data class ListItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val productId: Long,
    val quantity: Int = 1,
    val acquired: Boolean = false
)

@Entity(tableName = "pantry_items",
    foreignKeys = [
        ForeignKey(entity = Product::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("productId")]
)
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Int = 0,
    val lowStockThreshold: Int = 1
)
