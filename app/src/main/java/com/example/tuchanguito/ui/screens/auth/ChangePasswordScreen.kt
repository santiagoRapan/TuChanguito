package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.VisualTransformation
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.ui.theme.ColorSecondary
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MyApplication
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AuthViewModelFactory(app.authRepository)
    )
    val scope = rememberCoroutineScope()

    val modifyPasswordLabel = stringResource(id = R.string.modify_password)
    val verifyIdentityLabel = stringResource(id = R.string.verify_identity)
    val currentPasswordLabel = stringResource(id = R.string.current_password)
    val verifyLabel = stringResource(id = R.string.verify)
    val chooseNewPasswordLabel = stringResource(id = R.string.choose_new_password)
    val newPasswordLabel = stringResource(id = R.string.new_password)
    val confirmNewPasswordLabel = stringResource(id = R.string.confirm_new_password)
    val completeBothFieldsLabel = stringResource(id = R.string.complete_both_fields)
    val passwordsDoNotMatchLabel = stringResource(id = R.string.passwords_do_not_match)
    val errorChangingPasswordLabel = stringResource(id = R.string.error_changing_password)
    val saveNewPasswordLabel = stringResource(id = R.string.save_new_password)
    val genericErrorLabel = stringResource(id = R.string.generic_error)
    val loadingUserErrorLabel = stringResource(id = R.string.loading_user_error)
    val showPasswordLabel = stringResource(id = R.string.show_password)
    val hidePasswordLabel = stringResource(id = R.string.hide_password)

    var current by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentVisible by remember { mutableStateOf(false) }
    var new1Visible by remember { mutableStateOf(false) }
    var new2Visible by remember { mutableStateOf(false) }

    AuthScreenContainer(
        title = modifyPasswordLabel,
        onBack = onBack,
        content = {
        if (step == 1) {
            Text(verifyIdentityLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = current,
                onValueChange = { current = it },
                label = { Text(currentPasswordLabel) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { currentVisible = !currentVisible }, enabled = !loading) {
                        val icon = if (currentVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val desc = if (currentVisible) hidePasswordLabel else showPasswordLabel
                        Icon(imageVector = icon, contentDescription = desc, tint = ColorSecondary)
                    }
                },
                enabled = !loading
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        val profileRes = authViewModel.getProfile()
                        if (profileRes.isFailure) {
                            error = profileRes.exceptionOrNull()?.message ?: genericErrorLabel
                            loading = false
                            return@launch
                        }
                        val email = profileRes.getOrNull()?.email
                        if (email.isNullOrEmpty()) {
                            error = loadingUserErrorLabel
                            loading = false
                            return@launch
                        }
                        val valid = authViewModel.validateCredentials(email, current)
                        if (valid.isSuccess) {
                            step = 2
                        } else {
                            error = valid.exceptionOrNull()?.message ?: passwordsDoNotMatchLabel
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(verifyLabel, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        } else {
            Text(chooseNewPasswordLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = new1,
                onValueChange = { new1 = it },
                label = { Text(newPasswordLabel) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (new1Visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { new1Visible = !new1Visible }, enabled = !loading) {
                        val icon = if (new1Visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val desc = if (new1Visible) hidePasswordLabel else showPasswordLabel
                        Icon(imageVector = icon, contentDescription = desc, tint = ColorSecondary)
                    }
                },
                enabled = !loading
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = new2,
                onValueChange = { new2 = it },
                label = { Text(confirmNewPasswordLabel) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (new2Visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { new2Visible = !new2Visible }, enabled = !loading) {
                        val icon = if (new2Visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val desc = if (new2Visible) hidePasswordLabel else showPasswordLabel
                        Icon(imageVector = icon, contentDescription = desc, tint = ColorSecondary)
                    }
                },
                enabled = !loading
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        error = null
                        if (new1.isBlank() || new2.isBlank()) {
                            error = completeBothFieldsLabel
                            return@launch
                        }
                        if (new1 != new2) {
                            error = passwordsDoNotMatchLabel
                            return@launch
                        }
                        loading = true
                        val changeRes = authViewModel.changePassword(current, new1)
                        if (changeRes.isSuccess) {
                            onDone()
                        } else {
                            error = changeRes.exceptionOrNull()?.message ?: errorChangingPasswordLabel
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(saveNewPasswordLabel, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    })
}
