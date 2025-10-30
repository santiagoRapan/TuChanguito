package com.example.tuchanguito.ui.screens.lists

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(listId: Long) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val list by repo.listById(listId).collectAsState(initial = null)
    val items by repo.itemsForList(listId).collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val total = items.sumOf { item ->
        val price = products.firstOrNull { it.id == item.productId }?.price ?: 0.0
        (price * item.quantity).toInt()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(list?.title ?: "Lista") }, actions = {
                IconButton(onClick = {
                    // Share list
                    val body = buildString {
                        appendLine(list?.title ?: "Lista")
                        appendLine()
                        items.forEach { li ->
                            val prod = products.firstOrNull { it.id == li.productId }
                            appendLine("- ${prod?.name ?: "Producto"} x${li.quantity}")
                        }
                        appendLine()
                        append("Total: $${total}")
                    }
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }
                    context.startActivity(Intent.createChooser(send, "Compartir lista"))
                }) { Text("↗") }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Add the first product to simplify demo
                scope.launch { products.firstOrNull()?.let { repo.addItem(listId, it.id, 1) } }
            }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f)) {
                items(items, key = { it.id }) { item ->
                    val product = products.firstOrNull { it.id == item.productId }
                    ListRow(productName = product?.name ?: "Producto", price = product?.price ?: 0.0, item = item,
                        onInc = { scope.launch { repo.updateItem(item.copy(quantity = item.quantity + 1)) } },
                        onDec = { scope.launch { if (item.quantity > 1) repo.updateItem(item.copy(quantity = item.quantity - 1)) } },
                        onDelete = { scope.launch { repo.removeItem(item) } },
                        onToggleAcquired = { scope.launch { repo.updateItem(item.copy(acquired = !item.acquired)) } }
                    )
                }
            }
            Text("Costo total: $${total}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            Button(onClick = { /* finalize */ }, modifier = Modifier.padding(16.dp)) { Text("Finalizar") }
        }
    }
}

@Composable
private fun ListRow(
    productName: String,
    price: Double,
    item: ListItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onDelete: () -> Unit,
    onToggleAcquired: () -> Unit
) {
    androidx.compose.material3.ListItem(
        leadingContent = {
            Checkbox(checked = item.acquired, onCheckedChange = { onToggleAcquired() })
        },
        headlineContent = { Text(productName) },
        supportingContent = { Text("$${price}") },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDec) { Text("-") }
                Text("${item.quantity}")
                TextButton(onClick = onInc) { Text("+") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    Divider()
}
