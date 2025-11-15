package com.example.tuchanguito.ui.screens.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.ColorPrimary
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color

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
                modifier = Modifier.size(48.dp)
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text(stringResource(id = R.string.all)) }
                    )
                }
                items(uiState.chipCategories, key = { it.id }) { chip ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == chip.id,
                        onClick = { viewModel.onCategorySelected(chip.id) },
                        label = { Text(chip.name) }
                    )
                }
            }

            if (uiState.isLoading) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (uiState.items.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(id = R.string.no_products_in_pantry))
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
                                unit = item.unit ?: "",
                                onInc = { viewModel.incrementItem(item.id, item.quantity, item.unit) },
                                onDec = { viewModel.decrementItem(item.id, item.quantity, item.unit) },
                                onDelete = { viewModel.deleteItem(item.id) }
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
    var unit by rememberSaveable { mutableStateOf("u") }
    var category by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun prefill(product: Product) {
        name = product.name
        priceText = if (product.price != 0.0) product.price.toString() else ""
        unit = product.unit.ifBlank { "u" }
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
    unit: String,
    quantity: Double,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text("${quantity.roundToInt()} $unit") },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDec, enabled = quantity > 1) { Text("-") }
                Text(quantity.roundToInt().toString())
                TextButton(onClick = onInc) { Text("+") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        }
    )
}
