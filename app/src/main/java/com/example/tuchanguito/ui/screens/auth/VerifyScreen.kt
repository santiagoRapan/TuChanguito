package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(onVerified: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Verificar cuenta") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Ingresá el código 123456 para verificar (demo)")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth())
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    repo.verifyAccount("", code.trim()).onSuccess { onVerified() }.onFailure { error = it.message }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Verificar") }
        }
    }
}
