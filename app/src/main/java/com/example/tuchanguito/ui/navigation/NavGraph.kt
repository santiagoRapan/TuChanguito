package com.example.tuchanguito.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.tuchanguito.ui.screens.auth.ChangePasswordScreen
import com.example.tuchanguito.ui.screens.auth.LoginScreen
import com.example.tuchanguito.ui.screens.auth.RegisterScreen
import com.example.tuchanguito.ui.screens.auth.VerifyScreen
import com.example.tuchanguito.ui.screens.home.HomeScreen
import com.example.tuchanguito.ui.screens.lists.ListDetailScreen
import com.example.tuchanguito.ui.screens.lists.ListsScreen
import com.example.tuchanguito.ui.screens.pantry.PantryScreen
import com.example.tuchanguito.ui.screens.profile.ProfileScreen
import com.example.tuchanguito.ui.screens.products.ProductsScreen
import com.example.tuchanguito.ui.screens.categories.CategoriesScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        navigation(startDestination = Routes.LOGIN, route = Routes.AUTH) {
            composable(Routes.LOGIN) { LoginScreen(
                onLoginSuccess = { navController.navigate(TopLevelDest.Home.route) { popUpTo(Routes.AUTH) { inclusive = true } } },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.CHANGE_PASSWORD) }
            ) }
            composable(Routes.REGISTER) { RegisterScreen(onRegistered = { navController.navigate(Routes.VERIFY) }) }
            composable(Routes.VERIFY) { VerifyScreen(onVerified = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.AUTH) { inclusive = false } } }) }
            composable(Routes.CHANGE_PASSWORD) { ChangePasswordScreen(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() }) }
        }

        composable(TopLevelDest.Home.route) { HomeScreen(
            onOpenList = { id -> navController.navigate("lists/detail/$id") },
            onNewProduct = { navController.navigate(Routes.PRODUCTS) },
            onConfigureCategories = { navController.navigate(Routes.CATEGORIES) }
        ) }
        composable(TopLevelDest.Lists.route) { ListsScreen(onOpenList = { id -> navController.navigate("lists/detail/$id") }) }
        composable(TopLevelDest.Pantry.route) { PantryScreen() }
        composable(TopLevelDest.Profile.route) { ProfileScreen(onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) }) }
        composable(Routes.LIST_DETAIL) { backStack ->
            val id = backStack.arguments?.getString("listId")?.toLongOrNull() ?: -1L
            ListDetailScreen(listId = id, onClose = { navController.popBackStack() })
        }
        composable(Routes.PRODUCTS) { ProductsScreen() }
        composable(Routes.CATEGORIES) { CategoriesScreen() }
    }
}
