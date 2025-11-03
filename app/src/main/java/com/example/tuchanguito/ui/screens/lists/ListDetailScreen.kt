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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.network.dto.ListItemDTO
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(listId: Long, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val list by repo.listById(listId).collectAsState(initial = null)
    val items by repo.itemsForList(listId).collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())
    val categories by repo.categories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Initial sync from backend so detail shows server data
    LaunchedEffect(listId) {
        repo.loadListIntoLocal(listId)
        runCatching { repo.syncCatalog() }
        runCatching { repo.syncListItems(listId) }
    }

    // Remote-backed fallback for items
    var remoteItems by remember { mutableStateOf<List<ListItemDTO>>(emptyList()) }

    LaunchedEffect(listId) {
        repo.loadListIntoLocal(listId)
        runCatching { repo.syncCatalog() }
        runCatching { repo.syncListItems(listId) }
        // Also keep a remote snapshot to display immediately
        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
    }

    // Keep remote snapshot fresh when local items change (e.g., after add)
    LaunchedEffect(items.size) {
        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
    }

    // Helper maps
    val productById = remember(products) { products.associateBy { it.id } }
    val remoteByProductId = remember(remoteItems) { remoteItems.associateBy { it.product.id ?: -1L } }
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    // Choose source for UI
    val uiProducts: List<Long> = remember(items, remoteItems) {
        if (items.isNotEmpty()) items.map { it.productId } else remoteItems.map { it.product.id ?: -1L }
    }

    val total = remember(uiProducts, products, items, remoteItems) {
        uiProducts.sumOf { pid ->
            val qty = items.firstOrNull { it.productId == pid }?.quantity
                ?: remoteByProductId[pid]?.quantity?.toInt()
                ?: 0
            val price = productById[pid]?.price ?: 0.0
            (price * qty).toInt()
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var showFinalize by remember { mutableStateOf(false) }

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
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val grouped = remember(uiProducts, products) {
                uiProducts.groupBy { pid -> productById[pid]?.categoryId }
            }
            LazyColumn(Modifier.weight(1f)) {
                grouped.forEach { (catId, pids) ->
                    val catName = categories.firstOrNull { it.id == catId }?.name ?: "Sin categoría"
                    item(key = "header-$catId") { Text(catName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    items(pids, key = { it }) { pid ->
                        val product = productById[pid]
                        val local = items.firstOrNull { it.productId == pid }
                        val remote = remoteByProductId[pid]
                        val qty = local?.quantity ?: remote?.quantity?.toInt() ?: 0
                        val itemForRow = local ?: ListItem(
                            id = remote?.id ?: 0L,
                            listId = listId,
                            productId = pid,
                            quantity = qty,
                            acquired = false
                        )
                        val unit = product?.unit?.ifBlank { remote?.unit ?: "u" } ?: (remote?.unit ?: "u")
                        ListRow(
                            productName = product?.name ?: remote?.product?.name ?: "Producto",
                            price = product?.price ?: 0.0,
                            unit = unit,
                            item = itemForRow,
                            onInc = {
                                val existingId = local?.id ?: remote?.id
                                if (existingId != null && existingId > 0) {
                                    scope.launch { repo.updateItemQuantityRemote(listId, existingId, pid, qty + 1, unit) }
                                } else {
                                    scope.launch {
                                        repo.addItemRemote(listId, pid, qty + 1, unit)
                                        repo.syncListItems(listId)
                                        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                    }
                                }
                            },
                            onDec = {
                                if (qty > 1) {
                                    val existingId = local?.id ?: remote?.id
                                    if (existingId != null && existingId > 0) {
                                        scope.launch { repo.updateItemQuantityRemote(listId, existingId, pid, qty - 1, unit) }
                                    }
                                }
                            },
                            onDelete = {
                                val existingId = local?.id ?: remote?.id
                                if (existingId != null && existingId > 0) {
                                    scope.launch {
                                        if (local != null) repo.deleteItemRemote(listId, existingId, local) else runCatching { repo.deleteItemRemote(listId, existingId, ListItem(id = existingId, listId = listId, productId = pid, quantity = qty)) }
                                        runCatching { repo.deleteItemByIdLocal(existingId) }
                                        runCatching { repo.syncListItems(listId) }
                                        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                    }
                                }
                            },
                            onToggleAcquired = {
                                val existingId = local?.id ?: remote?.id
                                if (existingId != null && existingId > 0) {
                                    val newPurchased = !(local?.acquired ?: (remote?.purchased ?: false))
                                    scope.launch {
                                        runCatching { repo.toggleItemPurchasedRemote(listId, existingId, newPurchased) }
                                        runCatching { repo.syncListItems(listId) }
                                        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Text("Costo total: $${total}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            // Reserve space at end for the floating action button (approx 72-88dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 88.dp, bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(0.45f)
                        .heightIn(min = 36.dp)
                ) { Text("Cerrar lista") }
                Button(
                    onClick = { showFinalize = true },
                    modifier = Modifier
                        .weight(0.45f)
                        .heightIn(min = 36.dp)
                ) { Text("Finalizar") }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            products = products,
            onDismiss = { showAdd = false },
            onAdd = { productId, name, price, unit, categoryName ->
                scope.launch {
                    val chosenProductId = if (productId != null) {
                        // If selecting existing, pre-fill was shown; optionally update product only if we actually add
                        if (!name.isNullOrBlank() || price != null || !unit.isNullOrBlank() || !categoryName.isNullOrBlank()) {
                            // If user edited fields, propagate to backend to keep catalog consistente
                            val existing = products.firstOrNull { it.id == productId }
                            val catId = categoryName?.takeIf { it.isNotBlank() }?.let { repo.createOrFindCategoryByName(it) } ?: existing?.categoryId
                            repo.updateProductRemote(
                                id = productId,
                                name = name ?: existing?.name ?: "",
                                price = price ?: existing?.price ?: 0.0,
                                unit = unit ?: existing?.unit ?: "u",
                                categoryId = catId
                            )
                        }
                        productId
                    } else {
                        val catId = categoryName?.takeIf { it.isNotBlank() }?.let { repo.createOrFindCategoryByName(it) }
                        repo.createProductRemote(name!!.trim(), price ?: 0.0, unit ?: "u", catId)
                    }
                    val safeUnitForItem = unit?.ifBlank { products.firstOrNull { it.id == chosenProductId }?.unit ?: "u" } ?: (products.firstOrNull { it.id == chosenProductId }?.unit ?: "u")
                    runCatching { repo.addItemRemote(listId, chosenProductId, 1, safeUnitForItem) }
                    runCatching { repo.syncListItems(listId) }
                    // Refresh remote snapshot for immediate UI
                    remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                    showAdd = false
                }
            },
            onPrefillFor = { pid -> products.firstOrNull { it.id == pid } },
            categoryNameFor = { pid ->
                val prod = products.firstOrNull { it.id == pid }
                prod?.categoryId?.let { cid -> categoryById[cid]?.name }
            }
        )
    }

    // --- Finalize dialog state and UI ---
    var finalizing by remember { mutableStateOf(false) }

    // Replace Finalizar button onClick to open dialog
    LaunchedEffect(Unit) {
        // no-op placeholder to satisfy tool insertion constraints
    }

    if (showFinalize) {
        FinalizeDialog(
            listId = listId,
            itemsProvider = {
                // Always fetch a fresh snapshot from server to ensure accurate purchased flags
                runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(remoteItems)
            },
            repo = repo,
            onDismiss = { if (!finalizing) showFinalize = false },
            onDone = { showFinalize = false; onClose() }
        )
    }
}

@Composable
private fun ListRow(
    productName: String,
    price: Double,
    unit: String,
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
        supportingContent = { Text("$${price} · $unit x ${item.quantity}") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onAdd: (productId: Long?, name: String?, price: Double?, unit: String?, categoryName: String?) -> Unit,
    onPrefillFor: (Long) -> Product?,
    categoryNameFor: (Long) -> String?
) {
    var name by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("u") }
    var category by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun prefillFrom(product: Product) {
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
                val p = selectedId
                val price = priceText.toDoubleOrNull()
                onAdd(p, if (p == null) name else name.takeIf { it.isNotBlank() && it != (onPrefillFor(p)?.name ?: "") }, price, unit, category.ifBlank { null })
                busy = false
            }, enabled = !busy && (selectedId != null || name.trim().isNotBlank())) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = { if (!busy) onDismiss() }) { Text("Cancelar") } },
        title = { Text("Agregar producto a la lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { v ->
                        name = v
                        val matched = products.firstOrNull { it.name.equals(v, ignoreCase = true) }
                        selectedId = matched?.id
                        if (matched != null) prefillFrom(matched)
                    },
                    label = { Text("Producto") },
                    singleLine = true
                )
                if (suggestions.isNotEmpty()) {
                    suggestions.forEach { s ->
                        TextButton(onClick = {
                            selectedId = s.id
                            prefillFrom(s)
                            name = s.name
                        }) { Text(s.name) }
                    }
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Precio (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unidad") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría (opcional)") },
                    singleLine = true
                )
            }
        }
    )
}

// --- New: Finalize dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalizeDialog(
    listId: Long,
    itemsProvider: suspend () -> List<ListItemDTO>,
    repo: AppRepository,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var includePurchasedToPantry by remember { mutableStateOf(true) }
    var moveNotPurchased by remember { mutableStateOf(true) }

    // Load lists to select or allow creation
    val lists by repo.activeLists().collectAsState(initial = emptyList())
    var creatingNew by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var selectedListId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { repo.refreshLists() }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val items = itemsProvider()
                        val purchased = items.filter { it.purchased }
                        val notPurchased = items.filterNot { it.purchased }

                        // 1) Add purchased to pantry
                        if (includePurchasedToPantry && purchased.isNotEmpty()) {
                            for (it in purchased) {
                                runCatching {
                                    repo.addOrIncrementPantryItem(
                                        productId = it.product.id ?: return@runCatching,
                                        addQuantity = it.quantity.toInt(),
                                        unit = it.unit
                                    )
                                }
                            }
                            // Sync pantry in background (optional)
                            runCatching { repo.syncPantry() }
                        }

                        // 2) Move not purchased to another list
                        if (moveNotPurchased && notPurchased.isNotEmpty()) {
                            val targetId = if (creatingNew) {
                                val name = newListName.trim()
                                if (name.isEmpty()) null else runCatching { repo.createList(name) }.getOrNull()
                            } else selectedListId

                            if (targetId != null) {
                                for (it in notPurchased) {
                                    runCatching {
                                        repo.addItemRemote(
                                            listId = targetId,
                                            productId = it.product.id ?: return@runCatching,
                                            quantity = it.quantity.toInt(),
                                            unit = it.unit
                                        )
                                    }
                                }
                                // Refresh destination list items (optional)
                                runCatching { repo.syncListItems(targetId) }
                            }
                        }

                        // 3) Delete this list so it no longer appears in the lists screen
                        runCatching { repo.deleteListRemote(listId) }
                        runCatching { repo.refreshLists() }

                        busy = false
                        onDone()
                    }
                }
            ) { Text(if (busy) "Procesando..." else "Finalizar") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Finalizar lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Qué deseas hacer al finalizar?")
                // Purchased -> Pantry
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = includePurchasedToPantry, onCheckedChange = { includePurchasedToPantry = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar productos comprados a la alacena")
                }
                // Not purchased -> another list
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = moveNotPurchased, onCheckedChange = { moveNotPurchased = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Mover productos no comprados a otra lista")
                }
                if (moveNotPurchased) {
                    // Selector: existing vs create new
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { creatingNew = false }, label = { Text("Seleccionar lista") }, leadingIcon = {})
                        AssistChip(onClick = { creatingNew = true }, label = { Text("Crear nueva") }, leadingIcon = {})
                    }
                    if (creatingNew) {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text("Nombre de la nueva lista") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Simple dropdown using a list of buttons
                        Column {
                            Text("Selecciona una lista destino:")
                            val options = lists.filter { it.id != listId }
                            if (options.isEmpty()) {
                                Text("No hay listas disponibles")
                            } else {
                                options.forEach { l ->
                                    val selected = selectedListId == l.id
                                    androidx.compose.material3.ListItem(
                                        headlineContent = { Text(l.title) },
                                        trailingContent = {
                                            RadioButton(selected = selected, onClick = { selectedListId = l.id })
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
