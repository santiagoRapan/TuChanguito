package com.example.tuchanguito.ui.screens.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ShoppingList
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.tuchanguito.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(onOpenList: (Long) -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // State
    val lists by repo.activeLists().collectAsState(initial = emptyList())
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // Strings from resources
    val listAlreadyExists = stringResource(id = R.string.list_already_exists)
    val loadingUserErrorLabel = stringResource(id = R.string.loading_user_error)
    val deleteErrorLabel = stringResource(id = R.string.delete_error)
    val createErrorLabel = stringResource(id = R.string.create_error)
    val renameErrorLabel = stringResource(id = R.string.rename_error)

    LaunchedEffect(Unit) {
        busy = true
        // Ensure default categories exist and refresh lists from remote
        scope.launch {
            repo.ensureDefaultCategories()
        }
        val res = repo.refreshLists()
        if (res.isFailure) {
            snackbarHost.showSnackbar(res.exceptionOrNull()?.message ?: loadingUserErrorLabel)
        }
        busy = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.my_lists), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showCreate = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.create_list))
            }
            Spacer(Modifier.height(16.dp))

            if (busy) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (lists.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_lists_found))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(lists, key = { it.id }) { list ->
                        ShoppingListCard(
                            list = list,
                            onOpen = { onOpenList(list.id) },
                            onRename = { editId = list.id; editName = list.title },
                            onDelete = {
                                scope.launch {
                                    busy = true
                                    try {
                                        repo.deleteListRemote(list.id)
                                    } catch (t: Throwable) {
                                        snackbarHost.showSnackbar(t.message ?: deleteErrorLabel)
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            busy = busy
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateOrRenameDialog(
            title = stringResource(R.string.create_list_dialog_title),
            confirmButtonText = stringResource(R.string.create),
            listName = newName,
            isDuplicate = lists.any { it.title.equals(newName.trim(), ignoreCase = true) },
            onNameChange = { newName = it },
            onConfirm = {
                scope.launch {
                    busy = true
                    try {
                        val id = repo.createList(newName.trim())
                        if (id > 0) {
                            showCreate = false
                            newName = ""
                            onOpenList(id)
                        }
                    } catch (t: Throwable) {
                        snackbarHost.showSnackbar(t.message ?: createErrorLabel)
                    } finally {
                        busy = false
                    }
                }
            },
            onDismiss = { showCreate = false; newName = "" },
            busy = busy
        )
    }

    if (editId != null) {
        val id = editId!!
        CreateOrRenameDialog(
            title = stringResource(R.string.rename_list_dialog_title),
            confirmButtonText = stringResource(R.string.save_changes),
            listName = editName,
            isDuplicate = lists.any { it.id != id && it.title.equals(editName.trim(), ignoreCase = true) },
            onNameChange = { editName = it },
            onConfirm = {
                scope.launch {
                    busy = true
                    try {
                        repo.renameList(id, editName.trim())
                    } catch (t: Throwable) {
                        snackbarHost.showSnackbar(t.message ?: renameErrorLabel)
                    } finally {
                        busy = false
                        editId = null
                        editName = ""
                    }
                }
            },
            onDismiss = { editId = null; editName = "" },
            busy = busy
        )
    }
}

@Composable
fun ShoppingListCard(
    list: ShoppingList,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    busy: Boolean
) {
    var isRecurrent by rememberSaveable { mutableStateOf(false) } // This is local state for now

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(list.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onRename, enabled = !busy) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_list_content_description))
                    }
                    IconButton(onClick = onDelete, enabled = !busy) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_list_content_description), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isRecurrent,
                        onCheckedChange = { isRecurrent = it },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_as_recurrent))
                }
                Button(onClick = onOpen, enabled = !busy) {
                    Text(stringResource(R.string.open_list))
                }
            }
        }
    }
}

@Composable
fun CreateOrRenameDialog(
    title: String,
    confirmButtonText: String,
    listName: String,
    isDuplicate: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean
) {
    val listAlreadyExists = stringResource(id = R.string.list_already_exists)
    
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = listName.trim().isNotBlank() && !busy && !isDuplicate
            ) { Text(confirmButtonText) }
        },
        dismissButton = { TextButton(onClick = { if (!busy) onDismiss() }) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = listName,
                onValueChange = onNameChange,
                label = { Text(stringResource(id = R.string.list_name_label)) },
                singleLine = true,
                isError = isDuplicate,
                supportingText = { if (isDuplicate) Text(listAlreadyExists) }
            )
        }
    )
}
