package com.example.tuchanguito.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.ColorPrimary
import com.example.tuchanguito.ui.theme.ColorSecondary
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import com.example.tuchanguito.ui.theme.ScreenBackgroundGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onVerifyAccount: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        prefs.rememberMe.collect { remember = it }
    }

    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }

    Scaffold(
        containerColor = ScreenBackgroundGrey,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título centrado "TuChanguito" usando el color secundario (importante)
            Text(
                text = "TuChanguito",
                color = ColorSecondary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Contenedor blanco con sombra leve
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorSecondary
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isLoading
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = remember, onCheckedChange = { remember = it }, enabled = !isLoading)
                        Text("Recordarme")
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = error!!, color = Color.Red)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary, contentColor = Color.White),
                        onClick = {
                            error = null
                            isLoading = true
                            scope.launch {
                                try {
                                    val result = repo.login(email.trim(), password)
                                    if (result.isSuccess) {
                                        prefs.setRememberMe(remember)
                                        onLoginSuccess()
                                    } else {
                                        val msg = result.exceptionOrNull()?.message ?: "Error de inicio de sesión"
                                        error = msg
                                    }
                                } catch (t: Throwable) {
                                    val msg = t.message ?: "Error de inicio de sesión"
                                    error = msg
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("Ingresar")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onForgotPassword,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = ColorPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    TextButton(onClick = onRegister, enabled = !isLoading) {
                        Text(
                            text = buildAnnotatedString {
                                append("¿No tenes cuenta? ")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Crear cuenta.")
                                }
                            },
                            color = ColorPrimary
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val e = email.trim()
                                if (e.isNotEmpty()) {
                                    prefs.setPendingCredentials(e, password)
                                }
                                onVerifyAccount()
                            }
                        },
                        enabled = !isLoading && isEmailValid
                    ) { Text("Verificar cuenta", color = ColorPrimary) }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Si ya te registraste pero no verificaste tu cuenta, ingresá tu email y presioná Verificar cuenta",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
