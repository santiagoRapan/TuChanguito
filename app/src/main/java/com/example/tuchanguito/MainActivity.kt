package com.example.tuchanguito

import android.os.Bundle
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.tuchanguito.ui.theme.ColorPrimary
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import androidx.core.view.WindowCompat
import android.view.WindowManager
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.MyApplication
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.ui.theme.TuChanguitoTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.tuchanguito.ui.navigation.AppNavGraph
import com.example.tuchanguito.ui.navigation.Routes
import com.example.tuchanguito.ui.navigation.TopLevelDest
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.NavigationBarItemDefaults
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure the window will draw system bar backgrounds and is not translucent
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        // Let the system reserve system bar areas by default; we'll handle edge-to-edge only in landscape
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val prefs = PreferencesManager(this)
            val theme by prefs.theme.collectAsState(initial = "system")
            val isDark = when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
            TuChanguitoTheme(darkTheme = isDark) {
                 val windowBg = MaterialTheme.colorScheme.background
                 SideEffect {
                     window.setBackgroundDrawable(ColorDrawable(windowBg.toArgb()))
                 }

                 Box(modifier = Modifier.fillMaxSize().background(windowBg)) {
                     TuChanguitoApp()
                 }
             }
         }
     }

    // System bars are handled centrally in TuChanguitoTheme via accompanist SystemUiController
}

@PreviewScreenSizes
@Composable
fun TuChanguitoApp() {
    val sysUiController = rememberSystemUiController()
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val catalogRepository = remember { app.catalogRepository }
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val authToken by prefs.authToken.collectAsState(initial = null)
    val navController = rememberNavController()
    // Derive the currently selected top-level destination from the NavController's back stack.
    // This ensures navigations triggered from any screen (for example Home quick actions)
    // are reflected in the bottom navigation / navigation rail selection.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination: TopLevelDest? = remember(currentRoute) {
        // Map route strings (including parameterized routes like "lists/detail/{listId}")
        // to their corresponding top-level destination so the bottom nav highlights correctly.
        when {
            currentRoute == null -> null
            currentRoute.startsWith(TopLevelDest.Home.route) -> TopLevelDest.Home
            currentRoute.startsWith(TopLevelDest.Products.route) -> TopLevelDest.Products
            currentRoute.startsWith(TopLevelDest.Lists.route) -> TopLevelDest.Lists
            currentRoute.startsWith(TopLevelDest.Pantry.route) -> TopLevelDest.Pantry
            currentRoute.startsWith(TopLevelDest.Profile.route) -> TopLevelDest.Profile
            else -> null
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Ensure default categories exist when user is logged in
    LaunchedEffect(authToken) {
        if (!authToken.isNullOrEmpty()) {
            scope.launch {
                catalogRepository.ensureDefaultCategories()
            }
        }
    }

    if (authToken.isNullOrEmpty()) {
        AppNavGraph(
            navController = navController,
            startDestination = Routes.AUTH,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // In landscape show an explicit NavigationRail on the left; in portrait keep the adaptive bottom navigation
        if (isLandscape) {
            // If layoutDirection==LTR, Start is left; if RTL, Start is right
            // We'll apply displayCutout padding on the rail side (Start)
            Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))) {
                NavigationRail(containerColor = ColorPrimary, modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))) {
                    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
                        val indicator = Color.White.copy(alpha = 0.18f)
                        listOf(TopLevelDest.Home, TopLevelDest.Products, TopLevelDest.Lists, TopLevelDest.Pantry, TopLevelDest.Profile).forEach { dest ->
                            val selected = currentDestination?.route == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) { launchSingleTop = true }
                                },
                                icon = { Icon(dest.icon, contentDescription = androidx.compose.ui.res.stringResource(id = dest.labelRes), tint = if (selected) Color.White else Color.White.copy(alpha = 0.9f)) },
                                label = { Text(androidx.compose.ui.res.stringResource(id = dest.labelRes), color = if (selected) Color.White else Color.White.copy(alpha = 0.9f)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    unselectedIconColor = Color.White.copy(alpha = 0.9f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.White.copy(alpha = 0.9f),
                                    indicatorColor = indicator
                                )
                            )
                        }
                    }
                }
                // Content area: apply displayCutout padding on the opposite side as well to prevent overlap
                val contentPadding = WindowInsets.displayCutout.only(WindowInsetsSides.End)
                Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(contentPadding)) {
                    AppNavGraph(
                        navController = navController,
                        startDestination = TopLevelDest.Home.route,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Portrait: let Scaffold respect system bars; NavigationBar sits above system navigation area
            Scaffold(
                bottomBar = {
                    val indicator = Color.White.copy(alpha = 0.18f)
                    NavigationBar(containerColor = ColorPrimary, tonalElevation = 12.dp) {
                        listOf(TopLevelDest.Home, TopLevelDest.Products, TopLevelDest.Lists, TopLevelDest.Pantry, TopLevelDest.Profile).forEach { dest ->
                            val selected = currentDestination?.route == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigate(dest.route) { launchSingleTop = true } },
                                icon = { Icon(dest.icon, contentDescription = stringResource(id = dest.labelRes), tint = if (selected) Color.White else Color.White.copy(alpha = 0.9f)) },
                                label = { Text(stringResource(id = dest.labelRes), color = if (selected) Color.White else Color.White.copy(alpha = 0.9f)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    unselectedIconColor = Color.White.copy(alpha = 0.9f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.White.copy(alpha = 0.9f),
                                    indicatorColor = indicator
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    AppNavGraph(
                        navController = navController,
                        startDestination = TopLevelDest.Home.route,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TuChanguitoTheme {
        TuChanguitoApp()
    }
}
