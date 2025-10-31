package com.example.tuchanguito.ui.screens.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Alacenas") }) }) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Text("Gestionar productos en la despensa (RF15 opcional)", modifier = Modifier.padding(16.dp))
        }
    }
}
