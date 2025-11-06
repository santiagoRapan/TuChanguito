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
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import com.example.tuchanguito.data.AppRepository
import kotlinx.coroutines.launch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val snack = remember { SnackbarHostState() }

    // Filters controlled by UI
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Dialog/edit states must be declared before Scaffold usages
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var editingProductId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Server-driven state
    var remoteCategories by remember { mutableStateOf(listOf<com.example.tuchanguito.network.dto.CategoryDTO>()) }
    var remoteProducts by remember { mutableStateOf(listOf<com.example.tuchanguito.network.dto.ProductDTO>()) }

    // Initial catalog sync clears local ghosts but UI below uses server lists directly
    LaunchedEffect(Unit) {
        val res = repo.syncCatalog()
        if (res.isFailure) snack.showSnackbar(res.exceptionOrNull()?.message ?: "No se pudo sincronizar catálogo")
    }

    // Reload server categories whenever query changes (chips should only show categories with products)
    LaunchedEffect(query) {
        runCatching { repo.categoriesForQuery(query) }
            .onSuccess { cats ->
                remoteCategories = cats
                // If selected category disappeared for this query, reset selection
                if (selectedCategoryId != null && cats.none { it.id == selectedCategoryId }) selectedCategoryId = null
            }
            .onFailure { snack.showSnackbar(it.message ?: "Error cargando categorías") }
    }

    // Reload products whenever query/category changes
    LaunchedEffect(query, selectedCategoryId) {
        runCatching { repo.searchProductsDTO(name = query, categoryId = selectedCategoryId) }
            .onSuccess { list -> remoteProducts = list }
            .onFailure { snack.showSnackbar(it.message ?: "Error cargando productos") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos") },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
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
                label = { Text("Buscar") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Chips solo para categorías con productos (según backend)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("Todas") }
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
                    item { Text("Sin productos", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(remoteProducts.size, key = { idx -> remoteProducts[idx].id ?: idx.toLong() }) { i ->
                        val p = remoteProducts[i]
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { target ->
                                if (target == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        val id = p.id ?: return@launch
                                        val res = runCatching { repo.deleteProductRemote(id) }
                                        if (res.isFailure) {
                                            snack.showSnackbar(res.exceptionOrNull()?.message ?: "No se pudo eliminar")
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
                                    val raw = p.metadata?.get("price")
                                    val priceVal = when (raw) {
                                        is Number -> raw.toDouble()
                                        is String -> raw.toDoubleOrNull() ?: 0.0
                                        else -> 0.0
                                    }
                                    val priceStr = "%.2f".format(priceVal)
                                    Text("Precio: $" + priceStr)
                                },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { editingProductId = p.id }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Editar",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                val id = p.id ?: return@launch
                                                val res = runCatching { repo.deleteProductRemote(id) }
                                                if (res.isFailure) {
                                                    snack.showSnackbar(res.exceptionOrNull()?.message ?: "No se pudo eliminar")
                                                } else {
                                                    remoteProducts = remoteProducts.filterNot { it.id == id }
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Eliminar",
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
                            val finalCategoryId = categoryId ?: repo.createOrFindCategoryByName(categoryInput)
                            repo.createProductRemote(name.trim(), priceText.toDouble(), unit.trim(), finalCategoryId)
                            showCreate = false
                            // Refresh lists to reflect new product
                            remoteProducts = repo.searchProductsDTO(query, selectedCategoryId)
                            remoteCategories = repo.categoriesForQuery(query)
                        } catch (t: Throwable) {
                            // Surface the message instead of crashing the app
                            snack.showSnackbar(t.message ?: "No se pudo crear el producto")
                        } finally { busy = false }
                    }
                }, enabled = valid && !busy) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { if (!busy) showCreate = false }) { Text("Cancelar") } },
            title = { Text("Nuevo producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Precio") }, singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unidad") }, singleLine = true)
                    // Selector/entrada de categoría: elegí existente o escribí una nueva
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = if (categoryId != null) remoteCategories.firstOrNull { it.id == categoryId }?.name ?: categoryInput else categoryInput,
                            onValueChange = { input -> categoryInput = input; categoryId = null },
                            label = { Text("Categoría") },
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
            val price = (prod.metadata?.get("price") as? Double) ?: (prod.metadata?.get("price") as? Number)?.toDouble() ?: 0.0
            mutableStateOf(price.toString())
        }
        var unit by rememberSaveable { mutableStateOf((prod.metadata?.get("unit") as? String).orEmpty()) }
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
                            val finalCategoryId = categoryId ?: repo.createOrFindCategoryByName(categoryInput)
                            repo.updateProductRemote(prod.id ?: return@launch, name.trim(), priceText.toDouble(), unit.trim(), finalCategoryId)
                            editingProductId = null
                            // Refresh products after edit
                            remoteProducts = repo.searchProductsDTO(query, selectedCategoryId)
                            remoteCategories = repo.categoriesForQuery(query)
                        } catch (t: Throwable) {
                            snack.showSnackbar(t.message ?: "No se pudo guardar")
                        } finally { busy = false }
                    }
                }, enabled = valid && !busy) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { if (!busy) editingProductId = null }) { Text("Cancelar") } },
            title = { Text("Editar producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Precio") }, singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unidad") }, singleLine = true)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = if (categoryId != null) remoteCategories.firstOrNull { it.id == categoryId }?.name ?: categoryInput else categoryInput,
                            onValueChange = { input -> categoryInput = input; categoryId = null },
                            label = { Text("Categoría") },
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
