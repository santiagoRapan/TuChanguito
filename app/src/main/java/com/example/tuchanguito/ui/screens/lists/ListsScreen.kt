package com.example.tuchanguito.ui.screens.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.R
import com.example.tuchanguito.ui.theme.ColorPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    onOpenList: (Long) -> Unit,
    onViewHistory: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val catalogRepository = remember { app.catalogRepository }
    val listsRepository = remember { app.shoppingListsLocalRepository }
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // State
    val lists by listsRepository.observeLists().collectAsState(initial = emptyList())
    val regularLists = remember(lists) { lists.filterNot { it.recurring } }
    val recurringLists = remember(lists) { lists.filter { it.recurring } }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    val editIdState = rememberSaveable { mutableStateOf<Long?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var togglingId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteList by remember { mutableStateOf<ShoppingList?>(null) }
    var showSharedDeleteError by remember { mutableStateOf(false) }

    // Strings from resources
    val loadingUserErrorLabel = stringResource(id = R.string.loading_user_error)
    val deleteErrorLabel = stringResource(id = R.string.delete_error)
    val createErrorLabel = stringResource(id = R.string.create_error)
    val renameErrorLabel = stringResource(id = R.string.rename_error)
    val genericErrorLabel = stringResource(id = R.string.generic_error)
    val sharedDeleteErrorLabel = stringResource(id = R.string.shared_list_delete_not_allowed)

    LaunchedEffect(Unit) {
        busy = true
        // Ensure default categories exist and refresh lists from remote
        scope.launch {
            catalogRepository.ensureDefaultCategories()
        }
        val res = listsRepository.refreshLists()
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
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            // Circular button styled like the add button in ListDetailScreen
            Button(
                onClick = { showCreate = true },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_list))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.view_list_history))
                }
            }
            Spacer(Modifier.height(16.dp))

            if (busy) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (lists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Transparent box with thin black border and rounded corners
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .border(BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(8.dp))
                            .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_lists_found),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (regularLists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.active_lists_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(regularLists, key = { it.id }) { list ->
                            ShoppingListCard(
                                list = list,
                                onOpen = { onOpenList(list.id) },
                                onRename = { editIdState.value = list.id; editName = list.title },
                                onDelete = { pendingDeleteList = list },
                                onToggleRecurring = { checked ->
                                    scope.launch {
                                        togglingId = list.id
                                        runCatching { listsRepository.setRecurring(list.id, checked) }
                                            .onFailure { snackbarHost.showSnackbar(it.message ?: genericErrorLabel) }
                                        togglingId = null
                                    }
                                },
                                toggleEnabled = togglingId != list.id && !busy,
                                busy = busy
                            )
                        }
                    }
                    if (recurringLists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.recurring_lists_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(recurringLists, key = { it.id }) { list ->
                            ShoppingListCard(
                                list = list,
                                onOpen = { onOpenList(list.id) },
                                onRename = { editIdState.value = list.id; editName = list.title },
                                onDelete = { pendingDeleteList = list },
                                onToggleRecurring = { checked ->
                                    scope.launch {
                                        togglingId = list.id
                                        runCatching { listsRepository.setRecurring(list.id, checked) }
                                            .onFailure { snackbarHost.showSnackbar(it.message ?: genericErrorLabel) }
                                        togglingId = null
                                    }
                                },
                                toggleEnabled = togglingId != list.id && !busy,
                                busy = busy
                            )
                        }
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
                        val id = listsRepository.createList(newName.trim())
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

    if (editIdState.value != null) {
        val id = editIdState.value!!
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
                        listsRepository.renameList(id, editName.trim())
                    } catch (t: Throwable) {
                        snackbarHost.showSnackbar(t.message ?: renameErrorLabel)
                    } finally {
                        busy = false
                        editIdState.value = null
                        editName = ""
                    }
                }
            },
            onDismiss = { editIdState.value = null; editName = "" },
            busy = busy
        )
    }

    if (pendingDeleteList != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteList = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_list_message, pendingDeleteList!!.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val list = pendingDeleteList ?: return@TextButton
                        pendingDeleteList = null
                        scope.launch {
                            busy = true
                            try {
                                listsRepository.deleteList(list.id)
                            } catch (t: Throwable) {
                                // Si el backend responde con "shopping list not found" asumimos que es una lista compartida
                                // a la que este usuario ya no tiene permisos de borrado.
                                val msg = t.message ?: ""
                                if (msg.contains("shopping list not found", ignoreCase = true)) {
                                    showSharedDeleteError = true
                                } else {
                                    snackbarHost.showSnackbar(msg.ifBlank { deleteErrorLabel })
                                }
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text(stringResource(id = R.string.delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteList = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showSharedDeleteError) {
        AlertDialog(
            onDismissRequest = { showSharedDeleteError = false },
            title = { Text(stringResource(id = R.string.shared_list_delete_not_allowed_title)) },
            text = { Text(sharedDeleteErrorLabel) },
            confirmButton = {
                TextButton(onClick = { showSharedDeleteError = false }) {
                    Text(stringResource(id = R.string.accept))
                }
            }
        )
    }
}

@Composable
fun ShoppingListCard(
    list: ShoppingList,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleRecurring: (Boolean) -> Unit,
    toggleEnabled: Boolean,
    busy: Boolean
) {
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
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_list_content_description), tint = MaterialTheme.colorScheme.primary)
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
                        checked = list.recurring,
                        onCheckedChange = { onToggleRecurring(it) },
                        enabled = toggleEnabled
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
