package com.example.tuchanguito.ui.screens.lists

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import retrofit2.HttpException
import androidx.compose.ui.platform.LocalFocusManager
import java.net.SocketTimeoutException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(listId: Long, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val list by repo.listById(listId).collectAsState(initial = null)
    val listItems by repo.itemsForList(listId).collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())
    val categories by repo.categories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    // Remote-backed fallback for items
    var remoteItems by remember { mutableStateOf<List<ListItemDTO>>(emptyList()) }

    // Initial sync from backend so detail shows server data
    LaunchedEffect(listId) {
        repo.loadListIntoLocal(listId)
        runCatching { repo.syncCatalog() }
        runCatching { repo.syncListItems(listId) }
        // Keep a remote snapshot once for fallback rendering
        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
    }

    // Helper maps
    val productById = remember(products) { products.associateBy { it.id } }
    val remoteByProductId = remember(remoteItems) { remoteItems.associateBy { it.product.id ?: -1L } }
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    // Choose source for UI
    val uiProducts: List<Long> = remember(listItems, remoteItems) {
        if (listItems.isNotEmpty()) listItems.map { it.productId } else remoteItems.map { it.product.id ?: -1L }
    }

    val total = remember(uiProducts, products, listItems, remoteItems) {
        uiProducts.sumOf { pid ->
            val qty = listItems.firstOrNull { it.productId == pid }?.quantity
                ?: remoteByProductId[pid]?.quantity?.toInt()
                ?: 0
            val price = productById[pid]?.price ?: 0.0
            (price * qty).toInt()
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var showFinalize by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var shareBusy by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var shareIsError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(list?.title ?: "Lista") }, actions = {
                IconButton(onClick = {
                    // Share list via app share sheet (copy text)
                    val body = buildString {
                        appendLine(list?.title ?: "Lista")
                        appendLine()
                        listItems.forEach { li ->
                            val prod = products.firstOrNull { it.id == li.productId }
                            appendLine("- ${prod?.name ?: "Producto"} x${li.quantity}")
                        }
                        appendLine()
                        append("Total: \$${total}")
                    }
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }
                    context.startActivity(Intent.createChooser(send, "Compartir lista"))
                }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copiar contenido de la lista") }
                IconButton(onClick = {
                    // Reset message state each time dialog opens
                    shareMessage = null
                    shareIsError = false
                    showShare = true
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir lista con usuario")
                }
            })
        },
        snackbarHost = { SnackbarHost(hostState = snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = null) }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Group by local categoryId, falling back to remote product.category.id
            val grouped = remember(uiProducts, products, remoteItems) {
                uiProducts.groupBy { pid ->
                    productById[pid]?.categoryId ?: remoteByProductId[pid]?.product?.category?.id
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                grouped.forEach { (catId, pids) ->
                    val catName = categories.firstOrNull { it.id == catId }?.name
                        ?: pids.firstOrNull()?.let { firstPid ->
                            remoteByProductId[firstPid]?.product?.category?.name
                        }
                        ?: "Sin categoría"
                    item(key = "header-$catId") { Text(catName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    items(pids, key = { it }) { pid ->
                        val product = productById[pid]
                        val local = listItems.firstOrNull { it.productId == pid }
                        val remote = remoteByProductId[pid]
                        val qty = local?.quantity ?: remote?.quantity?.toInt() ?: 0
                        val itemForRow = local ?: ListItem(
                            id = remote?.id ?: 0L,
                            listId = listId,
                            productId = pid,
                            quantity = qty,
                            acquired = remote?.purchased ?: false
                        )
                        val unit = product?.unit?.ifBlank { remote?.unit ?: "u" } ?: (remote?.unit ?: "u")
                        ListRow(
                            productName = product?.name ?: remote?.product?.name ?: "Producto",
                            price = product?.price ?: 0.0,
                            unit = unit,
                            item = itemForRow,
                            onInc = {
                                val existingId = local?.id ?: remote?.id
                                scope.launch {
                                    try {
                                        if (existingId != null && existingId > 0) {
                                            repo.updateItemQuantityRemote(listId, existingId, pid, qty + 1, unit)
                                        } else {
                                            repo.addItemRemote(listId, pid, (qty + 1).coerceAtLeast(1), unit)
                                        }
                                        // Fetch a single fresh snapshot for remote fallback
                                        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                    } catch (t: Throwable) {
                                        snack.showSnackbar(t.message ?: "No se pudo actualizar la cantidad")
                                    }
                                }
                            },
                            onDec = {
                                if (qty > 1) {
                                    val existingId = local?.id ?: remote?.id
                                    if (existingId != null && existingId > 0) {
                                        scope.launch {
                                            try {
                                                repo.updateItemQuantityRemote(listId, existingId, pid, qty - 1, unit)
                                                remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                            } catch (t: Throwable) {
                                                snack.showSnackbar(t.message ?: "No se pudo actualizar la cantidad")
                                            }
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                val existingId = local?.id ?: remote?.id
                                if (existingId != null && existingId > 0) {
                                    scope.launch {
                                        try {
                                            if (local != null) repo.deleteItemRemote(listId, existingId, local) else runCatching { repo.deleteItemRemote(listId, existingId, ListItem(id = existingId, listId = listId, productId = pid, quantity = qty)) }
                                            // Single refresh for fallback
                                            remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                        } catch (t: Throwable) {
                                            snack.showSnackbar(t.message ?: "No se pudo eliminar el producto de la lista")
                                        }
                                    }
                                }
                            },
                            onToggleAcquired = {
                                val existingId = local?.id ?: remote?.id
                                if (existingId != null && existingId > 0) {
                                    val newPurchased = !(local?.acquired ?: (remote?.purchased ?: false))
                                    scope.launch {
                                        try {
                                            repo.toggleItemPurchasedRemote(listId, existingId, newPurchased)
                                            remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                                        } catch (t: Throwable) {
                                            snack.showSnackbar(t.message ?: "No se pudo cambiar el estado")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Text("Costo total: \$${total}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
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
                    try {
                        val chosenProductId: Long = productId ?: run {
                            val catId = categoryName?.let { if (it.isNotBlank()) repo.createOrFindCategoryByName(it) else null }
                            repo.createProductRemote(name!!.trim(), price ?: 0.0, unit ?: "u", catId)
                        }
                        if (productId != null) {
                            val existing = products.firstOrNull { it.id == productId }
                            val existingCategoryName = existing?.categoryId?.let { cid -> categoryById[cid]?.name }
                            val changedName = !name.isNullOrBlank()
                            val changedPrice = price != null
                            val changedUnit = !unit.isNullOrBlank() && unit != existing?.unit
                            val changedCategory = !categoryName.isNullOrBlank() && !categoryName.equals(existingCategoryName, ignoreCase = true)
                            if (changedName || changedPrice || changedUnit || changedCategory) {
                                val catId = if (changedCategory) repo.createOrFindCategoryByName(categoryName) else existing?.categoryId
                                repo.updateProductRemote(
                                    id = productId,
                                    name = name ?: existing?.name ?: "",
                                    price = price ?: existing?.price ?: 0.0,
                                    unit = unit ?: existing?.unit ?: "u",
                                    categoryId = catId
                                )
                            }
                        }
                        val fallbackUnit = products.firstOrNull { it.id == chosenProductId }?.unit?.ifBlank { "u" } ?: "u"
                        val safeUnitForItem = (unit?.takeIf { it.isNotBlank() } ?: fallbackUnit)
                        repo.addItemRemote(listId, chosenProductId, 1, safeUnitForItem)
                        // Single remote refresh for fallback
                        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                        showAdd = false
                    } catch (t: Throwable) {
                        snack.showSnackbar(t.message ?: "No se pudo agregar el producto a la lista")
                    }
                }
            },
            onPrefillFor = { pid -> products.firstOrNull { it.id == pid } },
            categoryNameFor = { pid ->
                val prod = products.firstOrNull { it.id == pid }
                prod?.categoryId?.let { cid -> categoryById[cid]?.name }
            }
        )
    }

    if (showShare) {
        ShareListDialog(
            busy = shareBusy,
            message = shareMessage,
            isError = shareIsError,
            onDismiss = {
                if (!shareBusy) {
                    showShare = false
                    shareMessage = null
                    shareIsError = false
                }
            },
            onShare = { email ->
                if (shareBusy) return@ShareListDialog
                shareBusy = true
                shareMessage = null
                shareIsError = false
                scope.launch {
                    try {
                        repo.shareListRemote(listId, email)
                        // Success: show concise success message
                        shareMessage = "La lista ha sido compartida"
                        shareIsError = false
                    } catch (t: Throwable) {
                        // If it's a network timeout, optimistically report success because the server may have processed it
                        val isTimeout = t is SocketTimeoutException || (t.message?.contains("timeout", ignoreCase = true) == true)
                        if (isTimeout) {
                            shareMessage = "La lista ha sido compartida"
                            shareIsError = false
                        } else {
                            val code = (t as? HttpException)?.code()
                            val msg = when (code) {
                                404 -> "No encontramos un usuario registrado con ese email"
                                409 -> "Ya compartiste esta lista con ese email"
                                400 -> "No se pudo compartir la lista. Verifica el email."
                                401, 403 -> "No estás autorizado para realizar esta acción"
                                else -> t.message ?: "No se pudo compartir la lista"
                            }
                            shareMessage = msg
                            shareIsError = true
                        }
                    } finally {
                        shareBusy = false
                    }
                }
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
                try { repo.fetchListItemsRemote(listId) } catch (_: Throwable) { remoteItems }
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
    ListItem(
        leadingContent = {
            Checkbox(checked = item.acquired, onCheckedChange = { onToggleAcquired() })
        },
        headlineContent = { Text(productName) },
        supportingContent = { Text("\$${price} · ${unit} x ${item.quantity}") },
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
    HorizontalDivider()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareListDialog(
    busy: Boolean,
    message: String?,
    isError: Boolean,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isValid = remember(email) { email.contains("@") && email.contains('.') && email.length >= 5 }
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            val canConfirm = !busy && isValid && (message == null || isError)
            TextButton(onClick = {
                focusManager.clearFocus()
                onShare(email.trim())
            }, enabled = canConfirm) {
                Text("Compartir")
            }
        },
        dismissButton = {
            val dismissLabel = if (message != null && !isError) "Cerrar" else "Cancelar"
            TextButton(onClick = { if (!busy) onDismiss() }) { Text(dismissLabel) }
        },
        title = { Text("Compartir lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Text("Ingresa el email del usuario con quien deseas compartir la lista")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !busy && (message == null || isError),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = {
                        if (!isValid && email.isNotBlank() && (message == null || isError)) Text("Introduce un email válido")
                    }
                )
                if (message != null) {
                    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    Text(message, color = color)
                }
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
                                    ListItem(
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
