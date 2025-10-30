package com.example.tuchanguito.ui.screens.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(onOpenList: (Long) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val lists by repo.activeLists().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Listas") }) }, floatingActionButton = {
        FloatingActionButton(onClick = { scope.launch { val id = repo.createList("Supermercado - Hoy") ; onOpenList(id) } }) { Text("+") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            lists.forEach { list ->
                ListItem(headlineContent = { Text(list.title) }, modifier = Modifier.fillMaxWidth().clickable { onOpenList(list.id) })
                Divider()
            }
        }
    }
}
