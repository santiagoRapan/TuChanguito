package com.example.tuchanguito.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.ui.screens.auth.AuthViewModel
import com.example.tuchanguito.ui.screens.auth.AuthViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(onVerified: () -> Unit, onBack: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MyApplication
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val scope = rememberCoroutineScope()

    val verifyAccountLabel = stringResource(id = R.string.verify_account)
    val backLabel = stringResource(id = R.string.back)
    val emailToVerifyLabel = stringResource(id = R.string.email_to_verify)
    val codeLabel = stringResource(id = R.string.code_label)
    val enterValidEmailLabel = stringResource(id = R.string.enter_valid_email)
    val codeResentFmt = stringResource(id = R.string.code_resent_fmt)
    val resendErrorLabel = stringResource(id = R.string.resend_error)
    val resendButtonLabel = stringResource(id = R.string.resend_button)
    // Hoisted non-composable labels for coroutine usage
    val verifyErrorLabel = stringResource(id = R.string.error_changing_password)

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val pendingEmail by authViewModel.pendingEmail.collectAsState()
    val pendingPassword by authViewModel.pendingPassword.collectAsState()
    val rememberMe by authViewModel.rememberMe.collectAsState()

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
        androidx.compose.material3.CenterAlignedTopAppBar(
            title = { Text(verifyAccountLabel, color = androidx.compose.ui.graphics.Color.White) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = backLabel, tint = androidx.compose.ui.graphics.Color.White) } },
            colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = com.example.tuchanguito.ui.theme.ColorPrimary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }, contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(emailToVerifyLabel) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading && !resendLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text(codeLabel) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
            if (resendMessage != null) { Spacer(Modifier.height(8.dp)); Text(text = resendMessage!!, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                isLoading = true
                scope.launch {
                    try {
                        val verifyResult = authViewModel.verify(code.trim())
                        if (verifyResult.isSuccess) {
                            if (!pendingEmail.isNullOrEmpty() && !pendingPassword.isNullOrEmpty()) {
                                val e = pendingEmail
                                val p = pendingPassword
                                if (!e.isNullOrEmpty() && !p.isNullOrEmpty()) {
                                    val loginResult = authViewModel.login(e, p, rememberMe)
                                    if (loginResult.isSuccess) {
                                        authViewModel.clearPendingCredentials()
                                        onVerified() // Go to Home (token set)
                                    } else {
                                        val msg = loginResult.exceptionOrNull()?.message ?: verifyErrorLabel
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
                            val msg = verifyResult.exceptionOrNull()?.message ?: verifyErrorLabel
                            error = msg // inline only
                        }
                    } catch (t: Throwable) {
                        val msg = t.message ?: verifyErrorLabel
                        error = msg // inline only
                    } finally { isLoading = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(verifyAccountLabel)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                resendMessage = null
                resendLoading = true
                scope.launch {
                    try {
                        val targetEmail = email.trim()
                        if (!isEmailValid) {
                            resendMessage = enterValidEmailLabel
                        } else {
                            runCatching { authViewModel.resendVerification(targetEmail) }
                                .onSuccess { resendMessage = String.format(codeResentFmt, targetEmail) }
                                .onFailure { resendMessage = it.message ?: resendErrorLabel }
                        }
                    } finally { resendLoading = false }
                }
            }, enabled = isEmailValid && !resendLoading, modifier = Modifier.fillMaxWidth()) {
                if (resendLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(resendButtonLabel)
            }
        }
    }
}
