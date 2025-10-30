package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegistered: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Crear cuenta") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                error = null
                scope.launch {
                    repo.register(email.trim(), password, name).onSuccess { onRegistered() }.onFailure { error = it.message ?: "Error" }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Registrarse") }
        }
    }
}
