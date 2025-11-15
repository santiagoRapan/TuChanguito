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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.ui.screens.auth.AuthViewModel
import com.example.tuchanguito.ui.screens.auth.AuthViewModelFactory
import com.example.tuchanguito.ui.theme.ColorSecondary
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import com.example.tuchanguito.ui.theme.ScreenBackgroundGrey
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onVerifyAccount: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MyApplication
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val rememberMe by authViewModel.rememberMe.collectAsState()
    val scope = rememberCoroutineScope()

    val appName = stringResource(id = R.string.app_name)
    val signInLabel = stringResource(id = R.string.login)
    val emailLabel = stringResource(id = R.string.email)
    val passwordLabel = stringResource(id = R.string.password)
    val rememberMeLabel = stringResource(id = R.string.remember_me)
    val forgotPasswordLabel = stringResource(id = R.string.forgot_password)
    val noAccountCreate = stringResource(id = R.string.no_account_create)
    val verifyAccountLabel = stringResource(id = R.string.verify)
    val loginErrorDefault = stringResource(id = R.string.loading_user_error)
    // Hoisted non-composable labels used inside coroutines
    val loginErrorLabel = stringResource(id = R.string.login_error)
    val showPasswordLabel = stringResource(id = R.string.show_password)
    val hidePasswordLabel = stringResource(id = R.string.hide_password)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }

    Scaffold(
        containerColor = ScreenBackgroundGrey
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
                text = appName,
                color = PrimaryTextBlue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Card surface uses theme surface color so it adapts to dark/light
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = signInLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(emailLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(passwordLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = !isLoading
                            ) {
                                val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                                val iconDesc = if (passwordVisible) hidePasswordLabel else showPasswordLabel
                                Icon(
                                    imageVector = icon,
                                    contentDescription = iconDesc,
                                    tint = ColorSecondary
                                )
                            }
                        },
                        enabled = !isLoading
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { checked ->
                                scope.launch { authViewModel.setRememberMe(checked) }
                            },
                            enabled = !isLoading
                        )
                        Text(rememberMeLabel)
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = error!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        onClick = {
                            error = null
                            isLoading = true
                            scope.launch {
                                try {
                                    val result = authViewModel.login(email.trim(), password, rememberMe)
                                    if (result.isSuccess) {
                                        onLoginSuccess()
                                    } else {
                                        val msg = result.exceptionOrNull()?.message ?: loginErrorDefault
                                        error = msg
                                    }
                                } catch (t: Throwable) {
                                    val msg = t.message ?: loginErrorLabel
                                    error = msg
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(signInLabel, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onForgotPassword,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = forgotPasswordLabel,
                            color = ColorSecondary,
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    TextButton(
                        onClick = onRegister,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(noAccountCreate)
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(" " + stringResource(id = R.string.register))
                                }
                            },
                            color = ColorSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val e = email.trim()
                                if (e.isNotEmpty()) {
                                    authViewModel.storePendingCredentials(e, password)
                                }
                                onVerifyAccount()
                            }
                        },
                        enabled = !isLoading && isEmailValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            verifyAccountLabel,
                             color = ColorSecondary,
                             modifier = Modifier.fillMaxWidth(),
                             textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text =  stringResource(id= R.string.verify_account_info),
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
