package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(onVerified: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = PreferencesManager(context)
    val pendingEmail by prefs.pendingEmail.collectAsState(initial = null)
    val pendingPassword by prefs.pendingPassword.collectAsState(initial = null)

    Scaffold(topBar = { TopAppBar(title = { Text("Verificar cuenta") }) }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Ingresá el código 123456 para verificar (demo)")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
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
                                        onVerified()
                                    } else {
                                        val msg = loginResult.exceptionOrNull()?.message ?: "Error"
                                        snackbarHostState.showSnackbar(msg)
                                        error = msg
                                    }
                                } else {
                                    onVerified()
                                }
                            } else {
                                onVerified()
                            }
                        } else {
                            val msg = verifyResult.exceptionOrNull()?.message ?: "Error"
                            snackbarHostState.showSnackbar(msg)
                            error = msg
                        }
                    } catch (t: Throwable) {
                        val msg = t.message ?: "Error"
                        snackbarHostState.showSnackbar(msg)
                        error = msg
                    } finally { isLoading = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Verificar")
            }
        }
    }
}
