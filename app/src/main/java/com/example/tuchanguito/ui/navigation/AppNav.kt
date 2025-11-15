package com.example.tuchanguito.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tuchanguito.R

sealed class TopLevelDest(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Home: TopLevelDest("home", R.string.home, Icons.Filled.Home)
    data object Products: TopLevelDest("products", R.string.products_title, Icons.Filled.Fastfood)
    data object Lists: TopLevelDest("lists", R.string.lists, Icons.Filled.List)
    data object Pantry: TopLevelDest("pantry", R.string.pantry, Icons.Filled.Kitchen)
    data object Profile: TopLevelDest("profile", R.string.profile, Icons.Filled.AccountCircle)
}

object Routes {
    const val AUTH = "auth"
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val VERIFY = "auth/verify"
    const val CHANGE_PASSWORD = "auth/changePassword"
    const val RECOVER_PASSWORD = "auth/recoverPassword"

    const val LIST_DETAIL = "lists/detail/{listId}"
    const val LIST_HISTORY = "lists/history"
    const val PRODUCTS = "products"
    const val CATEGORIES = "categories"
}
