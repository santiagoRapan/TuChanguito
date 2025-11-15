package com.example.tuchanguito.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverPasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MyApplication
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var step by remember { mutableStateOf(RecoverStep.REQUEST) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val title = stringResource(id = R.string.recover_password_title)
    val instructions = stringResource(id = R.string.recover_password_instructions)
    val sendEmailLabel = stringResource(id = R.string.recover_password_send)
    val successEmailLabel = stringResource(id = R.string.recover_password_email_sent)
    val codeHint = stringResource(id = R.string.code_label)
    val newPasswordLabel = stringResource(id = R.string.new_password)
    val confirmPasswordLabel = stringResource(id = R.string.confirm_new_password)
    val resetLabel = stringResource(id = R.string.recover_password_reset)
    val passwordsMismatch = stringResource(id = R.string.passwords_do_not_match)
    val genericError = stringResource(id = R.string.generic_error)
    val passwordResetSuccess = stringResource(id = R.string.recover_password_success)
    val emailLabel = stringResource(id = R.string.email)

    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }

    LaunchedEffect(step) {
        if (step == RecoverStep.REQUEST) {
            code = ""
            password = ""
            confirmPassword = ""
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { Text(title, color = androidx.compose.ui.graphics.Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = com.example.tuchanguito.ui.theme.ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            if (step == RecoverStep.REQUEST) {
                Text(text = instructions, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(emailLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !loading
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                } else if (info != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = info!!, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            info = null
                            authViewModel.recoverPassword(email.trim())
                                .onSuccess {
                                    step = RecoverStep.CODE
                                    info = successEmailLabel
                                }
                                .onFailure {
                                    error = it.message ?: genericError
                                }
                            loading = false
                        }
                    },
                    enabled = isEmailValid && !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(sendEmailLabel)
                    }
                }
            } else {
                Text(text = successEmailLabel, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(codeHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !loading
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(newPasswordLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(confirmPasswordLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                } else if (info != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = info!!, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            error = passwordsMismatch
                            return@Button
                        }
                        scope.launch {
                            loading = true
                            error = null
                            info = null
                            authViewModel.resetPassword(code.trim(), password)
                                .onSuccess {
                                    info = passwordResetSuccess
                                    onDone()
                                }
                                .onFailure {
                                    error = it.message ?: genericError
                                }
                            loading = false
                        }
                    },
                    enabled = code.isNotBlank() && password.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(resetLabel)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { step = RecoverStep.REQUEST }, enabled = !loading) {
                    Text(text = stringResource(id = R.string.recover_password_start_over))
                }
            }
        }
    }
}

private enum class RecoverStep { REQUEST, CODE }
