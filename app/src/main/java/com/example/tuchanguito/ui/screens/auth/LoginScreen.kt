package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
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
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val prefs = remember { PreferencesManager(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        prefs.rememberMe.collect { remember = it }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("TuCarrito") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Iniciar sesión")
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = remember, onCheckedChange = { remember = it })
                Text("Recordarme")
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = error!!, color = androidx.compose.ui.graphics.Color.Red)
            }
            Spacer(Modifier.height(12.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                error = null
                scope.launch {
                    val result = repo.login(email.trim(), password)
                    result.onSuccess {
                        prefs.setRememberMe(remember)
                        onLoginSuccess()
                    }.onFailure { error = it.message ?: "Error" }
                }
            }) { Text("Ingresar") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onForgotPassword) { Text("¿Olvidaste tu contraseña?") }
            TextButton(onClick = onRegister) { Text("¿No tenes cuenta? Crear cuenta.") }
        }
    }
}
