package com.example.tuchanguito.ui.screens.auth

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegistered: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") } // New: confirm password
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val passwordsMatch = remember(password, confirm) { password.isNotBlank() && confirm.isNotBlank() && password == confirm }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), enabled = !isLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirmar contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), enabled = !isLoading)
            if (!passwordsMatch && (password.isNotEmpty() || confirm.isNotEmpty())) {
                Spacer(Modifier.height(6.dp))
                Text(text = "Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error)
            }
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                // Don’t call API when passwords don’t match; no generic error either to avoid duplication
                if (!passwordsMatch) {
                    return@Button
                }
                error = null
                isLoading = true
                scope.launch {
                    try {
                        repo.register(email.trim(), password, name).onSuccess {
                            val prefs = PreferencesManager(context)
                            prefs.setPendingCredentials(email.trim(), password)
                            onRegistered()
                        }.onFailure {
                            val msg = it.message ?: "Error"
                            snackbarHostState.showSnackbar(msg)
                            error = msg
                        }
                    } catch (t: Throwable) {
                        val msg = t.message ?: "Error"
                        snackbarHostState.showSnackbar(msg)
                        error = msg
                    } finally { isLoading = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading && passwordsMatch) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Registrarse")
            }
        }
    }
}
