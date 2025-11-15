package com.example.tuchanguito.ui.screens.auth

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.tuchanguito.R
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.ui.screens.auth.AuthViewModel
import com.example.tuchanguito.ui.screens.auth.AuthViewModelFactory
import com.example.tuchanguito.ui.theme.ColorSecondary
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import com.example.tuchanguito.ui.theme.ScreenBackgroundGrey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegistered: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MyApplication
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val titleLabel = stringResource(id = R.string.register)
    val backLabel = stringResource(id = R.string.back)
    val nameLabel = stringResource(id = R.string.name)
    val emailLabel = stringResource(id = R.string.email)
    val passwordLabel = stringResource(id = R.string.password)
    val confirmPasswordLabel = stringResource(id = R.string.confirm_new_password)
    val passwordsNotMatchLabel = stringResource(id = R.string.passwords_do_not_match)
    val registeringLabel = stringResource(id = R.string.register)
    val genericErrorLabel = stringResource(id = R.string.generic_error)
    val showPasswordLabel = stringResource(id = R.string.show_password)
    val hidePasswordLabel = stringResource(id = R.string.hide_password)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") } // New: confirm password
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val passwordsMatch = remember(password, confirm) { password.isNotBlank() && confirm.isNotBlank() && password == confirm }

    Scaffold(
        containerColor = ScreenBackgroundGrey,
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { Text(titleLabel, color = PrimaryTextBlue) },
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = backLabel, tint = ColorSecondary)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ScreenBackgroundGrey,
                    titleContentColor = PrimaryTextBlue,
                    navigationIconContentColor = ColorSecondary,
                    actionIconContentColor = ColorSecondary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(nameLabel) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(emailLabel) }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(passwordLabel) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val iconDesc = if (passwordVisible) hidePasswordLabel else showPasswordLabel
                        Icon(imageVector = icon, contentDescription = iconDesc, tint = ColorSecondary)
                    }
                },
                enabled = !isLoading
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text(confirmPasswordLabel) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }, enabled = !isLoading) {
                        val icon = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val iconDesc = if (confirmVisible) hidePasswordLabel else showPasswordLabel
                        Icon(imageVector = icon, contentDescription = iconDesc, tint = ColorSecondary)
                    }
                },
                enabled = !isLoading
            )
            if (!passwordsMatch && (password.isNotEmpty() || confirm.isNotEmpty())) {
                Spacer(Modifier.height(6.dp))
                Text(text = passwordsNotMatchLabel, color = MaterialTheme.colorScheme.error)
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
                        authViewModel.register(email.trim(), password, name).onSuccess {
                            onRegistered()
                        }.onFailure {
                            val msg = it.message ?: genericErrorLabel
                            snackbarHostState.showSnackbar(msg)
                            error = msg
                        }
                    } catch (t: Throwable) {
                        val msg = t.message ?: genericErrorLabel
                        snackbarHostState.showSnackbar(msg)
                        error = msg
                    } finally { isLoading = false }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading && passwordsMatch, colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary, contentColor = Color.White)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White) else Text(registeringLabel, color = Color.White)
            }
        }
    }
}
