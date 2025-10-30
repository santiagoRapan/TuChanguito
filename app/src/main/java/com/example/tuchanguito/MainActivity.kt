package com.example.tuchanguito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = PreferencesManager(this)
            val theme by prefs.theme.collectAsState(initial = "system")
            TuChanguitoTheme(darkTheme = when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}) {
                TuChanguitoApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun TuChanguitoApp() {
    val navController = rememberNavController()
    var currentDestination by rememberSaveable { mutableStateOf<TopLevelDest?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            listOf(TopLevelDest.Home, TopLevelDest.Lists, TopLevelDest.Pantry, TopLevelDest.Profile).forEach { dest ->
                item(
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    selected = currentDestination?.route == dest.route,
                    onClick = {
                        currentDestination = dest
                        navController.navigate(dest.route) { launchSingleTop = true }
                    }
                )
            }
        }
    ) { innerPadding ->
        val start = Routes.AUTH
        AppNavGraph(
            navController = navController,
            startDestination = start,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TuChanguitoTheme {
        TuChanguitoApp()
    }
}