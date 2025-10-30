package com.example.tuchanguito.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val products by repo.products().collectAsState(initial = emptyList())
    val categories by repo.categories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Productos") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Precio") }, modifier = Modifier.width(120.dp))
                Button(onClick = {
                    scope.launch { repo.upsertProduct(Product(name = name, price = priceText.toDoubleOrNull() ?: 0.0)) ; name = "" ; priceText = "" }
                }) { Text("Añadir") }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            products.forEach { p ->
                ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("$${p.price}") })
            }
        }
    }
}
