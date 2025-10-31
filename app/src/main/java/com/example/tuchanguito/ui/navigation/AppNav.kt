package com.example.tuchanguito.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDest(val route: String, val label: String, val icon: ImageVector) {
    data object Home: TopLevelDest("home", "Inicio", Icons.Filled.Home)
    data object Lists: TopLevelDest("lists", "Listas", Icons.Filled.List)
    data object Pantry: TopLevelDest("pantry", "Alacenas", Icons.Filled.ShoppingCart)
    data object Profile: TopLevelDest("profile", "Perfil", Icons.Filled.AccountCircle)
}

object Routes {
    const val AUTH = "auth"
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val VERIFY = "auth/verify"
    const val CHANGE_PASSWORD = "auth/changePassword"

    const val LIST_DETAIL = "lists/detail/{listId}"
    const val PRODUCTS = "products"
    const val CATEGORIES = "categories"
}
