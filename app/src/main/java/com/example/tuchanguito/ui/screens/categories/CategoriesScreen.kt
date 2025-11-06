package com.example.tuchanguito.ui.screens.categories

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val categories by repo.categories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Categorías") }) }, contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.weight(1f))
                Button(onClick = { scope.launch { repo.upsertCategory(name.trim()) ; name = "" } }) { Text("Añadir") }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            categories.forEach { c -> ListItem(headlineContent = { Text(c.name) }) }
        }
    }
}
