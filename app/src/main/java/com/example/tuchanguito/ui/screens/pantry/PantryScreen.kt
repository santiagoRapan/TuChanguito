package com.example.tuchanguito.ui.screens.pantry

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.ui.theme.ColorAccent
import com.example.tuchanguito.ui.theme.ColorPrimary
import com.example.tuchanguito.ui.theme.ColorSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val viewModel: PantryViewModel = viewModel(
        factory = PantryViewModelFactory(app.pantryRepository, app.productRepository, app.categoryRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var itemToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHost.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.pantry), color = Color.White) },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            // Circular button consistent with ListsScreen style
            Button(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_product_to_list))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(id = R.string.search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val filterChipColors = FilterChipDefaults.filterChipColors(
                containerColor = ColorAccent.copy(alpha = 0.25f),
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = ColorAccent,
                selectedLabelColor = Color.White
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text(stringResource(id = R.string.all)) },
                        colors = filterChipColors
                    )
                }
                items(uiState.chipCategories, key = { it.id }) { chip ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == chip.id,
                        onClick = { viewModel.onCategorySelected(chip.id) },
                        label = { Text(chip.name) },
                        colors = filterChipColors
                    )
                }
            }

            if (uiState.isLoading) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (uiState.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .border(BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(8.dp))
                                .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.pantry_empty_message),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            PantryItemRow(
                                name = item.product.name,
                                quantity = item.quantity,
                                onInc = { viewModel.incrementItem(item.id, item.quantity, item.unit) },
                                onDec = { viewModel.decrementItem(item.id, item.quantity, item.unit) },
                                onRequestDelete = { itemToDeleteId = item.id }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPantryItemDialog(
            products = uiState.products,
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onAdd = { productId, name, price, unit, categoryName ->
                viewModel.addItem(productId, name.orEmpty(), price, unit, categoryName.orEmpty())
                showAddDialog = false
            }
        )
    }

    if (itemToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { itemToDeleteId = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_pantry_item_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = itemToDeleteId ?: return@TextButton
                    itemToDeleteId = null
                    viewModel.deleteItem(id)
                }) { Text(stringResource(id = R.string.delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteId = null }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPantryItemDialog(
    products: List<Product>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (productId: Long?, name: String?, price: Double?, unit: String?, categoryName: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var priceText by rememberSaveable { mutableStateOf("") }
    // Default unit should be empty instead of "u"
    var unit by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun prefill(product: Product) {
        name = product.name
        priceText = if (product.price != 0.0) product.price.toString() else ""
        // If product has no unit, keep it empty (do not default to "u")
        unit = product.unit.ifBlank { "" }
        category = categories.firstOrNull { it.id == product.categoryId }?.name ?: ""
    }

    val suggestions = remember(name, products) {
        val q = name.trim()
        if (q.isBlank()) emptyList() else products.filter { it.name.contains(q, ignoreCase = true) }.take(5)
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    busy = true
                    val price = priceText.toDoubleOrNull()
                    onAdd(selectedId, if (selectedId == null) name else name.takeIf { it.isNotBlank() }, price, unit, category.ifBlank { null })
                    busy = false
                },
                enabled = !busy && (selectedId != null || name.isNotBlank())
            ) { Text(stringResource(id = R.string.add_label)) }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onDismiss() }) { Text(stringResource(id = R.string.cancel)) }
        },
        title = { Text(stringResource(id = R.string.add_to_pantry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        suggestions.firstOrNull { s -> s.name.equals(it, ignoreCase = true) }?.let { product ->
                            selectedId = product.id
                            prefill(product)
                        }
                    },
                    label = { Text(stringResource(id = R.string.product)) },
                    singleLine = true
                )
                suggestions.forEach { suggestion ->
                    TextButton(onClick = { selectedId = suggestion.id; prefill(suggestion); name = suggestion.name }) {
                        Text(suggestion.name)
                    }
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(stringResource(id = R.string.price_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(id = R.string.unit_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(id = R.string.category_label)) },
                    singleLine = true
                )
            }
        }
    )
}

@Composable
private fun PantryItemRow(
    name: String,
    quantity: Double,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val formattedQuantity = remember(quantity) {
        if (quantity % 1.0 == 0.0) quantity.toInt().toString() else "%.2f".format(quantity)
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete()
                false
            } else false
        }
    )
    val cardColor = ColorSurface

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val fgColor = MaterialTheme.colorScheme.onErrorContainer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = fgColor
                        )
                    }
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDec, enabled = quantity > 1.0) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrementar")
                    }
                    // Show only numeric quantity (no unit)
                    Text(formattedQuantity, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onInc) {
                        Icon(Icons.Default.Add, contentDescription = "Incrementar")
                    }
                    IconButton(onClick = onRequestDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
