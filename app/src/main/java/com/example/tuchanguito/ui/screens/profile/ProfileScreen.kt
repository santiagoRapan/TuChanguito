package com.example.tuchanguito.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import kotlinx.coroutines.launch
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onChangePassword: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val profileLabel = stringResource(id = R.string.profile)
    val userDataLabel = stringResource(id = R.string.user_data)
    val nameLabel = stringResource(id = R.string.name)
    val surnameLabel = stringResource(id = R.string.surname)
    val editProfileLabel = stringResource(id = R.string.edit_profile)
    val changePasswordLabel = stringResource(id = R.string.change_password)
    val saveChangesLabel = stringResource(id = R.string.save_changes)
    val cancelLabel = stringResource(id = R.string.cancel)
    val personalizationLabel = stringResource(id = R.string.personalization)
    val loadingUserErrorLabel = stringResource(id = R.string.loading_user_error)
    val logoutLabel = stringResource(id = R.string.no)
    val profileUpdatedLabel = stringResource(id = R.string.profile_updated)
    val errorSavingLabel = stringResource(id = R.string.error_saving)
    val lightThemeLabel = stringResource(id = R.string.theme_light)
    val logoutConfirmLabel = stringResource(id = R.string.logout_confirm)
    val sessionClosedLabel = stringResource(id = R.string.session_closed)

    val theme by prefs.theme.collectAsState(initial = "system")
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var editMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        repo.getProfile().onSuccess { user ->
            name = user.name
            surname = user.surname
        }.onFailure { snackbarHostState.showSnackbar(it.message ?: loadingUserErrorLabel) }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text(profileLabel) }) }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val scrollMod = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .then(scrollMod)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile info
            Text(userDataLabel, style = MaterialTheme.typography.titleMedium)
            if (loading) { CircularProgressIndicator() }
            else {
                if (!editMode) {
                    Text("${nameLabel}: ${name}")
                    Text("${surnameLabel}: ${surname ?: ""}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { editMode = true }) { Text(editProfileLabel) }
                        Button(onClick = { onChangePassword() }) { Text(changePasswordLabel) }
                    }
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(nameLabel) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = surname ?: "", onValueChange = { surname = it }, label = { Text(surnameLabel) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            // Save
                            scope.launch {
                                loading = true
                                repo.updateProfile(name.takeIf { it.isNotBlank() }, surname?.takeIf { it.isNotBlank() }).onSuccess {
                                    snackbarHostState.showSnackbar(profileUpdatedLabel)
                                    editMode = false
                                }.onFailure { snackbarHostState.showSnackbar(it.message ?: errorSavingLabel) }
                                loading = false
                            }
                        }) { Text(saveChangesLabel) }
                        TextButton(onClick = { editMode = false }) { Text(cancelLabel) }
                    }
                }
            }

            HorizontalDivider()

            Text(personalizationLabel)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(lightThemeLabel)
                // Observe the stored theme directly; toggle between light/dark
                Switch(checked = theme == "light", onCheckedChange = { checked ->
                    scope.launch { prefs.setTheme(if (checked) "light" else "dark") }
                })
            }

            // Moneda field removed per request
            Spacer(Modifier.height(8.dp))
            val contextText = stringResource(id = R.string.logout_title)
            Button(onClick = { showLogoutDialog = true }) { Text(contextText) }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(id = R.string.app_name)) },
            text = { Text(logoutConfirmLabel) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        prefs.setAuthToken(null)
                        prefs.setCurrentUserId(null)
                        snackbarHostState.showSnackbar(sessionClosedLabel)
                     }
                    showLogoutDialog = false
                }) { Text(stringResource(id = R.string.yes)) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(id = R.string.no)) } }
        )
    }
}
