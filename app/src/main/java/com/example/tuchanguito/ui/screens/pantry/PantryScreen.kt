package com.example.tuchanguito.ui.screens.pantry

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Alacenas") }) }) { innerPadding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val scrollMod = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(Modifier.then(scrollMod).padding(innerPadding)) {
            Text("Gestionar productos en la despensa (RF15 opcional)", modifier = Modifier.padding(16.dp))
        }
    }
}
