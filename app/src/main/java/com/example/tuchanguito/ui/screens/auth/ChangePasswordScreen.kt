package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onDone: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Modificar contraseña") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = old, onValueChange = { old = it }, label = { Text("Actual") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = new, onValueChange = { new = it }, label = { Text("Nueva") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    repo.changePassword(email.trim(), old, new).onSuccess { onDone() }.onFailure { error = it.message }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        }
    }
}
