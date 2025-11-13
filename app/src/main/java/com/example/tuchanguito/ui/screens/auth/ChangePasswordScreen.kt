package com.example.tuchanguito.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val modifyPasswordLabel = stringResource(id = R.string.modify_password)
    val backLabel = stringResource(id = R.string.back)
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
    // Hoist non-composable usages: use these inside coroutines/handlers
    val genericErrorLabel = stringResource(id = R.string.generic_error)
    val loadingUserErrorLabel = stringResource(id = R.string.loading_user_error)

    var current by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = verify current, 2 = set new password
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(modifyPasswordLabel) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = backLabel) } }
        )
    }, contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (step == 1) {
                Text(verifyIdentityLabel)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text(currentPasswordLabel) }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        // get email from profile
                        val profileRes = repo.getProfile()
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
                        // Validate credentials without persisting token
                        val valid = repo.validateCredentials(email, current)
                        if (valid.isSuccess) {
                            step = 2
                        } else {
                            error = valid.exceptionOrNull()?.message ?: passwordsDoNotMatchLabel
                        }
                        loading = false
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text(verifyLabel) }
            } else {
                Text(chooseNewPasswordLabel)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = new1, onValueChange = { new1 = it }, label = { Text(newPasswordLabel) }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = new2, onValueChange = { new2 = it }, label = { Text(confirmNewPasswordLabel) }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                if (error != null) { Spacer(Modifier.height(8.dp)); Text(text = error!!, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    scope.launch {
                        error = null
                        if (new1.isBlank() || new2.isBlank()) { error = completeBothFieldsLabel; return@launch }
                        if (new1 != new2) { error = passwordsDoNotMatchLabel; return@launch }
                        loading = true
                        val changeRes = repo.changePassword(current, new1)
                        if (changeRes.isSuccess) {
                            onDone()
                        } else {
                            error = changeRes.exceptionOrNull()?.message ?: errorChangingPasswordLabel
                        }
                        loading = false
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text(saveNewPasswordLabel) }
            }
        }
    }
}
