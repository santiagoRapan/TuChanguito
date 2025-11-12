package com.example.tuchanguito.ui.screens.pantry

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()

    val pantryItems by repo.pantry().collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())
    val categories by repo.categories().collectAsState(initial = emptyList())

    val snack = remember { SnackbarHostState() }

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    // Backend-driven categories for chips (stable for current search)
    var chipCategories by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }

    // Load categories for current search text
    LaunchedEffect(query) {
        val list = runCatching { repo.pantryCategoriesForQuery(query.ifBlank { null }) }.getOrDefault(emptyList())
        chipCategories = list.mapNotNull { it.id?.let { id -> id to it.name } }
        if (selectedCategoryId != null && chipCategories.none { it.first == selectedCategoryId }) {
            selectedCategoryId = null
        }
    }

    // Sync pantry items when search or selected category changes
    LaunchedEffect(query, selectedCategoryId) {
        runCatching { repo.syncPantry(search = query.ifBlank { null }, categoryId = selectedCategoryId) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alacena") }) },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = null) } },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar") },
                singleLine = true
            )

            val productById = remember(products) { products.associateBy { it.id } }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    FilterChip(selected = selectedCategoryId == null, onClick = { selectedCategoryId = null }, label = { Text("Todas") })
                }
                items(chipCategories.size) { i ->
                    val (id, name) = chipCategories[i]
                    FilterChip(selected = selectedCategoryId == id, onClick = { selectedCategoryId = id }, label = { Text(name) })
                }
            }

            HorizontalDivider()

            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 88.dp)) {
                if (pantryItems.isEmpty()) {
                    item { Text("Sin productos en la alacena", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(pantryItems.size) { idx ->
                        val pi = pantryItems[idx]
                        val p = productById[pi.productId]
                        PantryRow(
                            name = p?.name ?: "Producto",
                            unit = p?.unit?.ifBlank { "u" } ?: "u",
                            quantity = pi.quantity,
                            onInc = { scope.launch { repo.updatePantryItem(pi.id, pi.quantity + 1) } },
                            onDec = { if (pi.quantity > 1) scope.launch { repo.updatePantryItem(pi.id, pi.quantity - 1) } },
                            onDelete = { scope.launch { repo.deletePantryItem(pi.id) } }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAdd) {
        val categoryById: Map<Long, Category> = remember(categories) { categories.associateBy { it.id } }
        AddPantryItemDialog(
            products = products,
            categories = categories.map { it.name },
            categoryNameFor = { pid ->
                val prod = products.firstOrNull { it.id == pid }
                prod?.categoryId?.let { cid -> categoryById[cid]?.name }
            },
            onDismiss = { showAdd = false },
            onAdd = { productId, name, price, unit, categoryName ->
                scope.launch {
                    try {
                        val finalProductId = if (productId != null) {
                            val existing = products.firstOrNull { it.id == productId }
                            if (!name.isNullOrBlank() || price != null || !unit.isNullOrBlank() || !categoryName.isNullOrBlank()) {
                                val catId = categoryName?.takeIf { it.isNotBlank() }?.let { repo.createOrFindCategoryByName(it) } ?: existing?.categoryId
                                repo.updateProductRemote(productId, name ?: existing?.name ?: "", price ?: existing?.price ?: 0.0, unit ?: existing?.unit ?: "u", catId)
                            }
                            productId
                        } else {
                            val catId = categoryName?.takeIf { it.isNotBlank() }?.let { repo.createOrFindCategoryByName(it) }
                            repo.createProductRemote(name!!.trim(), price ?: 0.0, unit ?: "u", catId)
                        }
                        repo.addOrIncrementPantryItem(finalProductId, addQuantity = 1, unit = unit ?: "u")
                        runCatching { repo.syncPantry() }
                        showAdd = false
                    } catch (t: Throwable) {
                        snack.showSnackbar(t.message ?: "No se pudo agregar el producto a la alacena")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPantryItemDialog(
    products: List<Product>,
    categories: List<String>,
    categoryNameFor: (Long) -> String?,
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
        category = categoryNameFor(product.id) ?: ""
    }

    val suggestions = remember(name, products) {
        val q = name.trim()
        if (q.isBlank()) emptyList() else products.filter { it.name.contains(q, ignoreCase = true) }.take(8)
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                busy = true
                val price = priceText.toDoubleOrNull()
                onAdd(selectedId, if (selectedId == null) name else name.takeIf { it.isNotBlank() }, price, unit, category.ifBlank { null })
                busy = false
            }, enabled = !busy && (selectedId != null || name.isNotBlank())) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = { if (!busy) onDismiss() }) { Text("Cancelar") } },
        title = { Text("Agregar a la alacena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { v ->
                        name = v
                        val match = products.firstOrNull { it.name.equals(v, ignoreCase = true) }
                        selectedId = match?.id
                        if (match != null) prefill(match)
                    },
                    label = { Text("Producto") },
                    singleLine = true
                )
                if (suggestions.isNotEmpty()) {
                    suggestions.forEach { s ->
                        TextButton(onClick = { selectedId = s.id; prefill(s); name = s.name }) { Text(s.name) }
                    }
                }
                OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Precio (opcional)") }, singleLine = true)
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unidad") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría (opcional)") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun PantryRow(
    name: String,
    unit: String,
    quantity: Int,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text("$unit x $quantity") },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDec) { Text("-") }
                Text(quantity.toString())
                TextButton(onClick = onInc) { Text("+") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        }
    )
}
