package com.example.tuchanguito.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import kotlinx.coroutines.launch
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onChangePassword: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val theme by prefs.theme.collectAsState(initial = "system")
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var editMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        repo.getProfile().onSuccess { user ->
            name = user.name
            surname = user.surname
        }.onFailure { snackbarHostState.showSnackbar(it.message ?: "Error al cargar usuario") }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Perfil") }) }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val scrollMod = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(Modifier.fillMaxSize().then(scrollMod).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Profile info
            Text("Datos de usuario", style = MaterialTheme.typography.titleMedium)
            if (loading) { CircularProgressIndicator() }
            else {
                if (!editMode) {
                    Text("Nombre: ${name}")
                    Text("Apellido: ${surname ?: ""}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { editMode = true }) { Text("Editar perfil") }
                        Button(onClick = { onChangePassword() }) { Text("Cambiar contraseña") }
                    }
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = surname ?: "", onValueChange = { surname = it }, label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            // Save
                            scope.launch {
                                loading = true
                                repo.updateProfile(name.takeIf { it.isNotBlank() }, surname?.takeIf { it.isNotBlank() }).onSuccess {
                                    snackbarHostState.showSnackbar("Perfil actualizado")
                                    editMode = false
                                }.onFailure { snackbarHostState.showSnackbar(it.message ?: "Error al guardar") }
                                loading = false
                            }
                        }) { Text("Guardar cambios") }
                        TextButton(onClick = { editMode = false }) { Text("Cancelar") }
                    }
                }
            }

            Divider()

            Text("Personalización")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tema claro")
                // Observe the stored theme directly; toggle between light/dark
                Switch(checked = theme == "light", onCheckedChange = { checked ->
                    scope.launch { prefs.setTheme(if (checked) "light" else "dark") }
                })
            }

            // Moneda field removed per request
            Spacer(Modifier.height(8.dp))
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
