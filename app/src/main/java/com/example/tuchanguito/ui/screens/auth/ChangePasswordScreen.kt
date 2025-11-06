package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = verify current, 2 = set new password
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Modificar contraseña") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } }
        )
    }, contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (step == 1) {
                Text("Verificá tu identidad")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Contraseña actual") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        // get email from profile
                        val profileRes = repo.getProfile()
                        if (profileRes.isFailure) {
                            error = profileRes.exceptionOrNull()?.message ?: "Error"
                            loading = false
                            return@launch
                        }
                        val email = profileRes.getOrNull()?.email
                        if (email.isNullOrEmpty()) {
                            error = "No se pudo obtener email"
                            loading = false
                            return@launch
                        }
                        // Validate credentials without persisting token
                        val valid = repo.validateCredentials(email, current)
                        if (valid.isSuccess) {
                            step = 2
                        } else {
                            error = valid.exceptionOrNull()?.message ?: "Contraseña inválida"
                        }
                        loading = false
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("Verificar") }
            } else {
                Text("Elegí una nueva contraseña")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = new1, onValueChange = { new1 = it }, label = { Text("Nueva contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = new2, onValueChange = { new2 = it }, label = { Text("Confirmar nueva contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    scope.launch {
                        error = null
                        if (new1.isBlank() || new2.isBlank()) { error = "Completá ambos campos"; return@launch }
                        if (new1 != new2) { error = "Las contraseñas no coinciden"; return@launch }
                        loading = true
                        val changeRes = repo.changePassword(current, new1)
                        if (changeRes.isSuccess) {
                            onDone()
                        } else {
                            error = changeRes.exceptionOrNull()?.message ?: "Error al cambiar contraseña"
                        }
                        loading = false
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("Guardar nueva contraseña") }
            }
        }
    }
}
