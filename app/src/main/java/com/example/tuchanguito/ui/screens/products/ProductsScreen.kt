package com.example.tuchanguito.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import com.example.tuchanguito.R
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.data.network.model.CategoryDto
import com.example.tuchanguito.data.network.model.ProductDto
import kotlinx.coroutines.launch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MyApplication
    val catalogRepository = remember { app.catalogRepository }
    val scope = rememberCoroutineScope()

    val snack = remember { SnackbarHostState() }
    val productsTitle = stringResource(id = R.string.products_title)
    val newProductDesc = stringResource(id = R.string.new_product)
    val searchLabel = stringResource(id = R.string.search)
    val allLabel = stringResource(id = R.string.all)
    val noProductsLabel = stringResource(id = R.string.no_products)
    val createErrorLabel = stringResource(id = R.string.create_error)
    val deleteErrorLabel = stringResource(id = R.string.delete_error)
    val editLabel = stringResource(id = R.string.edit_profile) // reuse edit label
    val deleteLabel = stringResource(id = R.string.delete_error)

    // Filters controlled by UI
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Dialog/edit states must be declared before Scaffold usages
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var editingProductId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Server-driven state
    var remoteCategories by remember { mutableStateOf(listOf<CategoryDto>()) }
    var remoteProducts by remember { mutableStateOf(listOf<ProductDto>()) }

    // Initial catalog sync clears local ghosts but UI below uses server lists directly
    LaunchedEffect(Unit) {
        val res = catalogRepository.syncCatalog()
        if (res.isFailure) snack.showSnackbar(res.exceptionOrNull()?.message ?: createErrorLabel)
    }

    // Reload server categories whenever query changes (chips should only show categories with products)
    LaunchedEffect(query) {
        runCatching { catalogRepository.categoriesForQuery(query) }
            .onSuccess { cats ->
                remoteCategories = cats
                // If selected category disappeared for this query, reset selection
                if (selectedCategoryId != null && cats.none { it.id == selectedCategoryId }) selectedCategoryId = null
            }
            .onFailure { snack.showSnackbar(it.message ?: "Error cargando categorías") }
    }

    // Reload products whenever query/category changes
    LaunchedEffect(query, selectedCategoryId) {
        runCatching { catalogRepository.searchProducts(name = query, categoryId = selectedCategoryId) }
            .onSuccess { list -> remoteProducts = list }
            .onFailure { snack.showSnackbar(it.message ?: "Error cargando productos") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(productsTitle) },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = newProductDesc)
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Búsqueda
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(searchLabel) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Chips solo para categorías con productos (según backend)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text(allLabel) }
                    )
                }
                items(remoteCategories.size) { index ->
                    val cat = remoteCategories[index]
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            HorizontalDivider()

            // Lista de productos (desde backend), con acciones
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (remoteProducts.isEmpty()) {
                    item { Text(noProductsLabel, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(remoteProducts.size, key = { idx -> remoteProducts[idx].id ?: idx.toLong() }) { i ->
                        val p = remoteProducts[i]
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { target ->
                                if (target == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        val id = p.id ?: return@launch
                                        val res = runCatching { catalogRepository.deleteProduct(id) }
                                        if (res.isFailure) {
                                            snack.showSnackbar(res.exceptionOrNull()?.message ?: deleteErrorLabel)
                                        } else {
                                            // Actualiza UI localmente para evitar un GET extra
                                            remoteProducts = remoteProducts.filterNot { it.id == id }
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val fgColor = MaterialTheme.colorScheme.onErrorContainer
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(vertical = 0.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = fgColor)
                                    }
                                }
                            }
                        ) {
                            // Contenido del item
                            ListItem(
                                headlineContent = { Text(p.name) },
                                supportingContent = {
                                    val priceVal = p.metadata.doubleValue("price")
                                    val priceStr = "%.2f".format(priceVal)
                                    Text(stringResource(id = R.string.price_label) + ": $" + priceStr)
                                },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { editingProductId = p.id }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = editLabel,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                val id = p.id ?: return@launch
                                                val res = runCatching { catalogRepository.deleteProduct(id) }
                                                if (res.isFailure) {
                                                    snack.showSnackbar(res.exceptionOrNull()?.message ?: deleteErrorLabel)
                                                } else {
                                                    remoteProducts = remoteProducts.filterNot { it.id == id }
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = deleteLabel,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by rememberSaveable { mutableStateOf("") }
        var priceText by rememberSaveable { mutableStateOf("") }
        var unit by rememberSaveable { mutableStateOf("") }
        var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
        var categoryInput by rememberSaveable { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        val valid = name.isNotBlank() && (priceText.toDoubleOrNull() != null) && unit.isNotBlank() && (categoryId != null || categoryInput.isNotBlank())

        AlertDialog(
            onDismissRequest = { if (!busy) showCreate = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val finalCategoryId = categoryId ?: catalogRepository.createOrFindCategoryByName(categoryInput)
                            catalogRepository.createProduct(name.trim(), priceText.toDouble(), unit.trim(), finalCategoryId)
                            showCreate = false
                            // Refresh lists to reflect new product
                            remoteProducts = catalogRepository.searchProducts(query, selectedCategoryId)
                            remoteCategories = catalogRepository.categoriesForQuery(query)
                        } catch (t: Throwable) {
                            // Surface the message instead of crashing the app
                            snack.showSnackbar(t.message ?: createErrorLabel)
                        } finally { busy = false }
                    }
                }, enabled = valid && !busy) { Text(stringResource(id = R.string.create)) }
            },
            dismissButton = { TextButton(onClick = { if (!busy) showCreate = false }) { Text(stringResource(id = R.string.cancel)) } },
            title = { Text(stringResource(id = R.string.new_product)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(id = R.string.name_label)) }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text(stringResource(id = R.string.price_label)) }, singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(stringResource(id = R.string.unit_label)) }, singleLine = true)
                    // Selector/entrada de categoría: elegí existente o escribí una nueva
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = if (categoryId != null) remoteCategories.firstOrNull { it.id == categoryId }?.name ?: categoryInput else categoryInput,
                            onValueChange = { input -> categoryInput = input; categoryId = null },
                            label = { Text(stringResource(id = R.string.category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            remoteCategories.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; categoryInput = ""; expanded = false })
                            }
                        }
                    }
                }
            }
        )
    }

    // Edit dialog usando los datos actuales de backend
    remoteProducts.firstOrNull { it.id == editingProductId }?.let { prod ->
        var name by rememberSaveable { mutableStateOf(prod.name) }
        var priceText by rememberSaveable {
            val price = prod.metadata.doubleValue("price")
            mutableStateOf(price.toString())
        }
        var unit by rememberSaveable { mutableStateOf(prod.metadata.stringValue("unit")) }
        var categoryId by rememberSaveable { mutableStateOf<Long?>(prod.category?.id) }
        var categoryInput by rememberSaveable { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        val valid = name.isNotBlank() && (priceText.toDoubleOrNull() != null) && (categoryId != null || categoryInput.isNotBlank())

        AlertDialog(
            onDismissRequest = { if (!busy) editingProductId = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val finalCategoryId = categoryId ?: catalogRepository.createOrFindCategoryByName(categoryInput)
                            catalogRepository.updateProduct(prod.id, name.trim(), priceText.toDouble(), unit.trim(), finalCategoryId)
                            editingProductId = null
                            // Refresh products after edit
                            remoteProducts = catalogRepository.searchProducts(query, selectedCategoryId)
                            remoteCategories = catalogRepository.categoriesForQuery(query)
                        } catch (t: Throwable) {
                            snack.showSnackbar(t.message ?: createErrorLabel)
                        } finally { busy = false }
                    }
                }, enabled = valid && !busy) { Text(stringResource(id = R.string.save_changes)) }
            },
            dismissButton = { TextButton(onClick = { if (!busy) editingProductId = null }) { Text(stringResource(id = R.string.cancel)) } },
            title = { Text(stringResource(id = R.string.new_product)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(id = R.string.name_label)) }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text(stringResource(id = R.string.price_label)) }, singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(stringResource(id = R.string.unit_label)) }, singleLine = true)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = if (categoryId != null) remoteCategories.firstOrNull { it.id == categoryId }?.name ?: categoryInput else categoryInput,
                            onValueChange = { input -> categoryInput = input; categoryId = null },
                            label = { Text(stringResource(id = R.string.category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            remoteCategories.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; categoryInput = ""; expanded = false })
                            }
                        }
                    }
                }
            }
        )
    }
}

private fun JsonElement?.doubleValue(key: String, default: Double = 0.0): Double =
    this?.jsonObject?.get(key)?.jsonPrimitive?.doubleOrNull ?: default

private fun JsonElement?.stringValue(key: String): String =
    this?.jsonObject?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
