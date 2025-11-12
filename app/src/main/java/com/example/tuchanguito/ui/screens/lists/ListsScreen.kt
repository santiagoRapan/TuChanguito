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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(onOpenList: (Long) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val snackbarHost = remember { SnackbarHostState() }
    // Load lists from API on first composition
    LaunchedEffect(Unit) {
        val res = repo.refreshLists()
        if (res.isFailure) snackbarHost.showSnackbar(res.exceptionOrNull()?.message ?: "No se pudo cargar listas")
    }
    val lists by repo.activeLists().collectAsState(initial = emptyList())

    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Listas") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, contentDescription = "Crear lista") }
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
                                Icon(Icons.Filled.Create, contentDescription = "Renombrar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        repo.deleteListRemote(list.id)
                                    } catch (t: Throwable) {
                                        snackbarHost.showSnackbar(t.message ?: "No se pudo eliminar")
                                    } finally { busy = false }
                                }
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error) }
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
                        if (duplicate) { snackbarHost.showSnackbar("Ya existe una lista con ese nombre"); busy = false; return@launch }
                        val id = try { repo.createList(nameTrim) } catch (t: Throwable) { snackbarHost.showSnackbar(t.message ?: "No se pudo crear"); 0L } finally { busy = false }
                        if (id > 0) { showCreate = false; val go = id; newName = ""; onOpenList(go) }
                     }
                 }, enabled = nameTrim.isNotBlank() && !busy && !duplicate) { Text("Crear lista") }
            },
            dismissButton = { TextButton(onClick = { if (!busy) { showCreate = false; newName = "" } }) { Text("Cancelar") } },
            title = { Text("Nueva lista") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { value -> newName = value },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = { if (duplicate) Text("Ya existe una lista con ese nombre") }
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
                        if (renameDuplicate) { snackbarHost.showSnackbar("Ya existe una lista con ese nombre"); busy = false; return@launch }
                        try { repo.renameList(id, renameTrim) } catch (t: Throwable) { snackbarHost.showSnackbar(t.message ?: "No se pudo renombrar") } finally { busy = false }
                          editId = null; editName = ""
                     }
                }, enabled = renameTrim.isNotBlank() && !busy && !renameDuplicate) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { if (!busy) { editId = null; editName = "" } }) { Text("Cancelar") } },
            title = { Text("Renombrar lista") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { value -> editName = value },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    isError = renameDuplicate,
                    supportingText = { if (renameDuplicate) Text("Ya existe una lista con ese nombre") }
                )
            }
        )
    }
}
