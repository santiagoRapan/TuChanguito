package com.example.tuchanguito.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(onVerified: () -> Unit, onBack: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val prefs = PreferencesManager(context)
    val pendingEmail by prefs.pendingEmail.collectAsState(initial = null)
    val pendingPassword by prefs.pendingPassword.collectAsState(initial = null)

    // Reactive email state: update when pendingEmail appears
    var email by remember { mutableStateOf(pendingEmail ?: "") }
    LaunchedEffect(pendingEmail) {
        if (!pendingEmail.isNullOrBlank()) {
            email = pendingEmail!!
        }
    }

    var resendMessage by remember { mutableStateOf<String?>(null) }
    var resendLoading by remember { mutableStateOf(false) }

    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Verificar cuenta") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } }
        )
    } /* snackbarHost removed */ ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email a verificar") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading && !resendLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
            if (resendMessage != null) { Spacer(Modifier.height(8.dp)); Text(text = resendMessage!!, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                isLoading = true
                scope.launch {
                    try {
                        val verifyResult = repo.verifyAccount("", code.trim())
                        if (verifyResult.isSuccess) {
                            if (!pendingEmail.isNullOrEmpty() && !pendingPassword.isNullOrEmpty()) {
                                val e = pendingEmail
                                val p = pendingPassword
                                if (!e.isNullOrEmpty() && !p.isNullOrEmpty()) {
                                    val loginResult = repo.login(e, p)
                                    if (loginResult.isSuccess) {
                                        prefs.clearPendingCredentials()
                                        onVerified() // Go to Home (token set)
                                    } else {
                                        val msg = loginResult.exceptionOrNull()?.message ?: "Error"
                                        error = msg // inline only
                                    }
                                } else {
                                    // Verified, but no credentials to log in: go back to Login
                                    onBack()
                                }
                            } else {
                                // Verified, but no credentials to log in: go back to Login
                                onBack()
                            }
                        } else {
                            val msg = verifyResult.exceptionOrNull()?.message ?: "Error"
                            error = msg // inline only
                        }
                    } catch (t: Throwable) {
                        val msg = t.message ?: "Error"
                        error = msg // inline only
                    } finally { isLoading = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Verificar")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                resendMessage = null
                resendLoading = true
                scope.launch {
                    try {
                        val targetEmail = email.trim()
                        if (!isEmailValid) {
                            resendMessage = "Ingresa un email válido"
                        } else {
                            runCatching { repo.resendVerificationCode(targetEmail) }
                                .onSuccess { resendMessage = "Código reenviado a $targetEmail" }
                                .onFailure { resendMessage = it.message ?: "No se pudo reenviar código (verifica que el email esté registrado)" }
                        }
                    } finally { resendLoading = false }
                }
            }, enabled = isEmailValid && !resendLoading, modifier = Modifier.fillMaxWidth()) {
                if (resendLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Reenviar código")
            }
        }
    }
}
