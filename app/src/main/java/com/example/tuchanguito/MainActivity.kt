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
import androidx.compose.runtime.LaunchedEffect
import com.example.tuchanguito.ui.theme.ButtonBlue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure content does not draw behind system bars by default, avoids flicker on rotation
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val prefs = PreferencesManager(this)
            val theme by prefs.theme.collectAsState(initial = "system")
            val isDark = when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
            TuChanguitoTheme(darkTheme = isDark) {
                 StatusBarForTheme(isDark)
                 val configuration = LocalConfiguration.current
                 val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                 Column {
                    if (!isDark) {
                        // Pintar SIEMPRE la franja del alto de la status bar en azul en tema claro
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1F6FA3))
                                .windowInsetsTopHeight(WindowInsets.statusBars)
                        )
                    }
                    if (isLandscape && !isDark) {
                        // Fallback mínimo si los insets reportan 0 en horizontal
                        Spacer(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color(0xFF1F6FA3)))
                    }
                    TuChanguitoApp()
                 }
             }
        }
    }
}

@Composable
private fun StatusBarForTheme(isDark: Boolean) {
    val activity = (LocalContext.current as? Activity) ?: return
    SideEffect {
        if (activity.isFinishing || activity.isDestroyed) return@SideEffect
        runCatching {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (!isDark) {
                // En modo claro: status bar azul e íconos blancos
                window.statusBarColor = android.graphics.Color.parseColor("#1F6FA3")
                controller.isAppearanceLightStatusBars = false
            } else {
                // En modo oscuro: mantener íconos claros
                controller.isAppearanceLightStatusBars = false // íconos claros sobre fondo oscuro
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun TuChanguitoApp(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val authToken by prefs.authToken.collectAsState(initial = null)

    val navController = rememberNavController()
    var currentDestination by rememberSaveable { mutableStateOf<TopLevelDest?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


    if (authToken.isNullOrEmpty()) {
        // Not signed in: show auth flow without bottom navigation
        AppNavGraph(
            navController = navController,
            startDestination = Routes.AUTH,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Signed in: show bottom navigation and main graph
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