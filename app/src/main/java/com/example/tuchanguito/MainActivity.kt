package com.example.tuchanguito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.rememberNavController
import com.example.tuchanguito.ui.navigation.AppNavGraph
import com.example.tuchanguito.ui.navigation.Routes
import com.example.tuchanguito.ui.navigation.TopLevelDest
import com.example.tuchanguito.ui.theme.TuChanguitoTheme
import com.example.tuchanguito.data.PreferencesManager
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.tuchanguito.ui.theme.ButtonBlue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val prefs = PreferencesManager(this)
            val theme by prefs.theme.collectAsState(initial = "system")
            val isDark = when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
            TuChanguitoTheme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
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
    val prefs = remember { PreferencesManager(context) }
    val authToken by prefs.authToken.collectAsState(initial = null)
    val navController = rememberNavController()
    var currentDestination by rememberSaveable { mutableStateOf<TopLevelDest?>(null) }

    if (authToken.isNullOrEmpty()) {
        AppNavGraph(
            navController = navController,
            startDestination = Routes.AUTH,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                listOf(TopLevelDest.Home, TopLevelDest.Products, TopLevelDest.Lists, TopLevelDest.Pantry, TopLevelDest.Profile).forEach { dest ->
                    item(
                        icon = { Icon(dest.icon, contentDescription = dest.label, tint = if (currentDestination?.route == dest.route) ButtonBlue else Color.Unspecified) },
                        label = { Text(dest.label, color = if (currentDestination?.route == dest.route) ButtonBlue else Color.Unspecified) },
                        selected = currentDestination?.route == dest.route,
                        onClick = {
                            currentDestination = dest
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TuChanguitoTheme {
        TuChanguitoApp()
    }
}