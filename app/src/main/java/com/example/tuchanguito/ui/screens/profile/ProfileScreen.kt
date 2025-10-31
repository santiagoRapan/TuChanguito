package com.example.tuchanguito.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val theme by prefs.theme.collectAsState(initial = "system")
    val currency by prefs.currency.collectAsState(initial = "$")
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Perfil") }) }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Personalización")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = theme == "light", onClick = { scope.launch { prefs.setTheme("light") } }, label = { Text("Claro") })
                FilterChip(selected = theme == "dark", onClick = { scope.launch { prefs.setTheme("dark") } }, label = { Text("Oscuro") })
                FilterChip(selected = theme == "system", onClick = { scope.launch { prefs.setTheme("system") } }, label = { Text("Sistema") })
            }
            OutlinedTextField(value = currency, onValueChange = { scope.launch { prefs.setCurrency(it) } }, label = { Text("Moneda") })
            val contextText = "Cerrar sesión"
            Button(onClick = { showLogoutDialog = true }) { Text(contextText) }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro que querés cerrar la sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        prefs.setAuthToken(null)
                        prefs.setCurrentUserId(null)
                        snackbarHostState.showSnackbar("Sesión cerrada")
                    }
                    showLogoutDialog = false
                }) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("No") } }
        )
    }
}
