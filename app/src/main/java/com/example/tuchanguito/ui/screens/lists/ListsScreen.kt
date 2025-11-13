package com.example.tuchanguito.ui.screens.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.example.tuchanguito.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(onOpenList: (Long) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val snackbarHost = remember { SnackbarHostState() }

    val listsTitle = stringResource(id = R.string.lists)
    val createListContentDesc = stringResource(id = R.string.create_list)
    val renameDesc = stringResource(id = R.string.create)
    val deleteDesc = stringResource(id = R.string.delete_error) // reuse for contentDescription fallback
    val createListLabel = stringResource(id = R.string.create_list)
    val createLabel = stringResource(id = R.string.create)
    val cancelLabel = stringResource(id = R.string.cancel)
    val newListTitle = stringResource(id = R.string.create_list_dialog_title)
    val listNameLabel = stringResource(id = R.string.list_name_label)
    val listAlreadyExists = stringResource(id = R.string.list_already_exists)
    val createListButtonLabel = stringResource(id = R.string.create_list)
    // Hoisted non-composable labels for coroutine usage
    val loadingUserErrorLabel = stringResource(id = R.string.loading_user_error)
    val deleteErrorLabel = stringResource(id = R.string.delete_error)
    val createErrorLabel = stringResource(id = R.string.create)
    val renameErrorLabel = stringResource(id = R.string.rename_error)

    // Load lists from API on first composition
    LaunchedEffect(Unit) {
        val res = repo.refreshLists()
        if (res.isFailure) snackbarHost.showSnackbar(res.exceptionOrNull()?.message ?: loadingUserErrorLabel)
    }
    val lists by repo.activeLists().collectAsState(initial = emptyList())

    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(listsTitle) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, contentDescription = createListContentDesc) }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val scrollMod = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(Modifier.fillMaxSize().then(scrollMod).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lists.forEach { list ->
                ListItem(
                    headlineContent = { Text(list.title) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { editId = list.id; editName = list.title }) {
                                Icon(Icons.Filled.Create, contentDescription = stringResource(id = R.string.create))
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        repo.deleteListRemote(list.id)
                                    } catch (t: Throwable) {
                                        snackbarHost.showSnackbar(t.message ?: deleteErrorLabel)
                                    } finally { busy = false }
                                }
                            }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete_error), tint = MaterialTheme.colorScheme.error) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onOpenList(list.id) }
                )
                Divider()
            }
        }
    }

    if (showCreate) {
        val nameTrim = newName.trim()
        val duplicate = lists.any { it.title.equals(nameTrim, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { if (!busy) { showCreate = false; newName = "" } },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        if (duplicate) { snackbarHost.showSnackbar(listAlreadyExists); busy = false; return@launch }
                        val id = try { repo.createList(nameTrim) } catch (t: Throwable) { snackbarHost.showSnackbar(t.message ?: createErrorLabel); 0L } finally { busy = false }
                        if (id > 0) { showCreate = false; val go = id; newName = ""; onOpenList(go) }
                     }
                 }, enabled = nameTrim.isNotBlank() && !busy && !duplicate) { Text(createListButtonLabel) }
            },
            dismissButton = { TextButton(onClick = { if (!busy) { showCreate = false; newName = "" } }) { Text(cancelLabel) } },
            title = { Text(newListTitle) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { value -> newName = value },
                    label = { Text(listNameLabel) },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = { if (duplicate) Text(listAlreadyExists) }
                )
            }
        )
    }

    if (editId != null) {
        val id = editId!!
        val renameTrim = editName.trim()
        val renameDuplicate = lists.any { it.id != id && it.title.equals(renameTrim, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { if (!busy) { editId = null; editName = "" } },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        if (renameDuplicate) { snackbarHost.showSnackbar(listAlreadyExists); busy = false; return@launch }
                        try { repo.renameList(id, renameTrim) } catch (t: Throwable) { snackbarHost.showSnackbar(t.message ?: renameErrorLabel) } finally { busy = false }
                          editId = null; editName = ""
                     }
                }, enabled = renameTrim.isNotBlank() && !busy && !renameDuplicate) { Text(stringResource(id = R.string.save_changes)) }
            },
            dismissButton = { TextButton(onClick = { if (!busy) { editId = null; editName = "" } }) { Text(cancelLabel) } },
            title = { Text(stringResource(id = R.string.rename_error)) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { value -> editName = value },
                    label = { Text(stringResource(id = R.string.create)) },
                    singleLine = true,
                    isError = renameDuplicate,
                    supportingText = { if (renameDuplicate) Text(listAlreadyExists) }
                )
            }
        )
    }
}
