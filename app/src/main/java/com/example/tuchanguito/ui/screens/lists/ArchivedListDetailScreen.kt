package com.example.tuchanguito.ui.screens.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.ui.theme.ColorPrimary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedListDetailScreen(listId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val viewModel: ArchivedListDetailViewModel = viewModel(
        factory = ArchivedListDetailViewModelFactory(listId, app.shoppingListHistoryRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.list_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        if (uiState.isLoading) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text("Cargando...", modifier = Modifier.fillMaxWidth())
            }
        } else if (uiState.errorMessage != null) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(uiState.errorMessage ?: "Error")
            }
        } else {
            val list = uiState.list
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
                item {
                    Text(text = list?.title ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                // Render items grouped by category. itemsByCategory keys can be null (no category)
                val grouped = uiState.itemsByCategory
                grouped.entries.sortedWith(compareBy(nullsLast<String>()) { it.key }).forEach { entry ->
                    val categoryName = entry.key
                    val itemsForCategory = entry.value
                    item {
                        // category header
                        Text(
                            text = categoryName ?: stringResource(id = R.string.no_category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(itemsForCategory) { it ->
                        ArchivedListItemCard(item = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedListItemCard(item: ArchivedListItem) {
    val isDark = MaterialTheme.colorScheme.background != Color.White
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = MaterialTheme.colorScheme.onBackground)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Checkbox(checked = item.acquired, onCheckedChange = {}, enabled = false)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.acquired) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(text = "$%.2f".format(item.price), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val formattedQuantity = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.2f".format(item.quantity)
                Text(formattedQuantity, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = item.unit, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
