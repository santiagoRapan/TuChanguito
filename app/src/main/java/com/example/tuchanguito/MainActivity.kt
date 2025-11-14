package com.example.tuchanguito

import android.os.Bundle
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.tuchanguito.ui.theme.ButtonBlue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val prefs = PreferencesManager(this)
            val theme by prefs.theme.collectAsState(initial = "system")
            val isDark = when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
            TuChanguitoTheme(darkTheme = isDark) {
                // Read the theme background color from the composition, then
                // apply it to the window background drawable via SideEffect.
                // The SideEffect lambda is non-composable, so we must capture
                // the color in a val first.
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
}

@PreviewScreenSizes
@Composable
fun TuChanguitoApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
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
    val layoutType = if (isLandscape) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar

    // Ensure default categories exist when user is logged in
    LaunchedEffect(authToken) {
        if (!authToken.isNullOrEmpty()) {
            scope.launch {
                repo.ensureDefaultCategories()
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
                NavigationRail(modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))) {
                    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
                        listOf(TopLevelDest.Home, TopLevelDest.Products, TopLevelDest.Lists, TopLevelDest.Pantry, TopLevelDest.Profile).forEach { dest ->
                            val selected = currentDestination?.route == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) { launchSingleTop = true }
                                },
                                icon = { Icon(dest.icon, contentDescription = androidx.compose.ui.res.stringResource(id = dest.labelRes)) },
                                label = { Text(androidx.compose.ui.res.stringResource(id = dest.labelRes)) }
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
            NavigationSuiteScaffold(
                layoutType = layoutType,
                navigationSuiteItems = {
                    listOf(TopLevelDest.Home, TopLevelDest.Products, TopLevelDest.Lists, TopLevelDest.Pantry, TopLevelDest.Profile).forEach { dest ->
                        item(
                            icon = { Icon(dest.icon, contentDescription = androidx.compose.ui.res.stringResource(id = dest.labelRes), tint = if (currentDestination?.route == dest.route) ButtonBlue else Color.Unspecified) },
                            label = { Text(androidx.compose.ui.res.stringResource(id = dest.labelRes), color = if (currentDestination?.route == dest.route) ButtonBlue else Color.Unspecified) },
                            selected = currentDestination?.route == dest.route,
                            onClick = {
                                navController.navigate(dest.route) { launchSingleTop = true }
                            }
                        )
                    }
                }
            ) {
                AppNavGraph(
                    navController = navController,
                    startDestination = TopLevelDest.Home.route,
                    modifier = Modifier.fillMaxSize()
                )
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