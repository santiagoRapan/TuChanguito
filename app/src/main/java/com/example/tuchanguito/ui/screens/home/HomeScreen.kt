package com.example.tuchanguito.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import com.example.tuchanguito.ui.theme.ButtonBlue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }

    val lists by repo.activeLists().collectAsState(initial = emptyList())
    val active = lists.firstOrNull()
    val items by if (active != null) {
        repo.itemsForList(active.id).collectAsState(initial = emptyList())
    } else remember { mutableStateOf(emptyList()) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.app_name), color = PrimaryTextBlue) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ActiveListCard(active, items, onOpenList)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickAction("Nueva lista") { onOpenList(newList()) }
                QuickAction("Nuevo producto", onNewProduct)
                QuickAction("Configurar categorias", onConfigureCategories)
            }
            Spacer(Modifier.height(16.dp))
            Text("Bajo stock", style = MaterialTheme.typography.titleLarge)
            // In a full implementation, list pantry items under stock threshold
        }
    }
}

@Composable
private fun ActiveListCard(active: ShoppingList?, items: List<ListItem>, onOpen: (Long) -> Unit) {
    val acquiredCount = items.count { it.acquired }
    val progress = if (items.isEmpty()) 0f else acquiredCount.toFloat() / items.size.toFloat()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ButtonBlue) {
        Column(Modifier.padding(16.dp)) {
            Text(active?.title ?: "Supermercado - Hoy", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)), color = Color.White, trackColor = Color(0xFF8ECBF5))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${items.size - acquiredCount} pendientes", color = Color.White)
                Text("$${total(items)}", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { if (active != null) onOpen(active.id) }, colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue, contentColor = Color.White)) { Text("Abrir") }
        }
    }
}

private fun total(items: List<ListItem>): Int = items.sumOf { it.quantity * 100 } // demo pricing

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)),
                color = ButtonBlue,
                contentColor = Color.White,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "add", tint = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun newList(): Long {
     // Create a new list synchronously for demo with runBlocking in real use; here we fake an id 0 and trigger navigation afterwards
     // For simplicity in Composables we avoid blocking; let Lists screen handle creation. Here we just return -1 to open Lists page
     return -1
 }
