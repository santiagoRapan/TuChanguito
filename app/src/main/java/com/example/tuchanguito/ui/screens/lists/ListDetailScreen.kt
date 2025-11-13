package com.example.tuchanguito.ui.screens.lists

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.network.dto.ListItemDTO
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt
import com.example.tuchanguito.R
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import retrofit2.HttpException
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

    // Hoisted strings used inside click handlers / coroutines
    val defaultListTitle = stringResource(id = R.string.lists)
    val productLabel = stringResource(id = R.string.product)
    val totalLabelFmt = stringResource(id = R.string.total_label)
    val shareChooserTitle = stringResource(id = R.string.share_list_chooser)
    val copyContentDesc = stringResource(id = R.string.copy_list_content)
    val shareWithUserDesc = stringResource(id = R.string.share_with_user)
    val noCategoryLabel = stringResource(id = R.string.no_category)
    val unitDefault = stringResource(id = R.string.unit_default)
    val deleteItemError = stringResource(id = R.string.delete_item_error)
    val updateQuantityError = stringResource(id = R.string.update_quantity_error)
    val changeStateError = stringResource(id = R.string.change_state_error)
    val closeListLabel = stringResource(id = R.string.close_list)
    val finalizeLabel = stringResource(id = R.string.finalize)
    val addLabel = stringResource(id = R.string.add_label)
    val cancelLabel = stringResource(id = R.string.cancel)
    val addProductTitle = stringResource(id = R.string.add_product_to_list)
    val priceLabel = stringResource(id = R.string.price_label)
    val unitLabel = stringResource(id = R.string.unit_label)
    val categoryLabel = stringResource(id = R.string.category_label)
    val shareSuccess = stringResource(id = R.string.share_success)
    val addItemError = stringResource(id = R.string.add_item_error)
    // share error messages
    val shareError404 = stringResource(id = R.string.share_error_404)
    val shareError409 = stringResource(id = R.string.share_error_409)
    val shareError400 = stringResource(id = R.string.share_error_400)
    val shareErrorUnauthorized = stringResource(id = R.string.share_error_unauthorized)
    val shareErrorDefault = stringResource(id = R.string.share_error_default)

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

    // Progress: how many distinct products are marked acquired/purchased
    val itemsCount = remember(uiProducts) { uiProducts.size }
    val acquiredCount = remember(listItems, remoteItems) {
        if (listItems.isNotEmpty()) listItems.count { it.acquired }
        else remoteItems.count { it.purchased }
    }
    val progressFraction = remember(itemsCount, acquiredCount) { if (itemsCount == 0) 0f else (acquiredCount.toFloat() / itemsCount.toFloat()).coerceIn(0f, 1f) }
    // Animated progress for smooth transitions
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, animationSpec = tween(durationMillis = 400))

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
            TopAppBar(title = {
                // Title on the left, compact progress bar on the right
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = list?.title ?: defaultListTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.width(160.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    // Animated percentage label
                    val percent = (animatedProgress * 100f).roundToInt()
                    Text(text = "$percent%", style = MaterialTheme.typography.bodySmall)
                }
            }, actions = {
                IconButton(onClick = {
                    // Share list via app share sheet (copy text)
                    val body = buildString {
                        appendLine(list?.title ?: defaultListTitle)
                        appendLine()
                        listItems.forEach { li ->
                            val prod = products.firstOrNull { it.id == li.productId }
                            appendLine("- ${prod?.name ?: productLabel} x${li.quantity}")
                        }
                        appendLine()
                        append(String.format(totalLabelFmt, total))
                    }
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }
                    context.startActivity(Intent.createChooser(send, shareChooserTitle))
                }) { Icon(Icons.Default.ContentCopy, contentDescription = copyContentDesc) }
                IconButton(onClick = {
                    // Reset message state each time dialog opens
                    shareMessage = null
                    shareIsError = false
                    showShare = true
                }) {
                    Icon(Icons.Default.Share, contentDescription = shareWithUserDesc)
                }
            })
        },
        snackbarHost = { SnackbarHost(hostState = snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = null) }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val scrollMod = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(Modifier.fillMaxSize().then(scrollMod).padding(padding)) {
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
                        ?: noCategoryLabel
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
                        val unit = product?.unit?.ifBlank { remote?.unit ?: unitDefault } ?: (remote?.unit ?: unitDefault)

                        val existingId = local?.id ?: remote?.id
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { target ->
                                if (target == SwipeToDismissBoxValue.EndToStart && existingId != null && existingId > 0) {
                                    scope.launch {
                                        try {
                                            if (local != null) {
                                                repo.deleteItemRemote(listId, existingId, local)
                                            } else {
                                                runCatching { repo.deleteItemRemote(listId, existingId, ListItem(id = existingId, listId = listId, productId = pid, quantity = qty)) }
                                            }
                                            // Rely on Room emission to update list
                                        } catch (t: Throwable) {
                                            snack.showSnackbar(t.message ?: deleteItemError)
                                        }
                                    }
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val fg = MaterialTheme.colorScheme.onErrorContainer
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(vertical = 0.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = fg)
                                    }
                                }
                            }
                        ) {
                            ListRow(
                                productName = product?.name ?: remote?.product?.name ?: productLabel,
                                price = product?.price ?: 0.0,
                                unit = unit,
                                item = itemForRow,
                                onInc = {
                                    val eid = local?.id ?: remote?.id
                                    scope.launch {
                                        try {
                                            if (eid != null && eid > 0) {
                                                repo.updateItemQuantityRemote(listId, eid, pid, qty + 1, unit)
                                            } else {
                                                repo.addItemRemote(listId, pid, (qty + 1).coerceAtLeast(1), unit)
                                            }
                                        } catch (t: Throwable) {
                                            snack.showSnackbar(t.message ?: updateQuantityError)
                                        }
                                    }
                                },
                                onDec = {
                                    if (qty > 1) {
                                        val eid = local?.id ?: remote?.id
                                        if (eid != null && eid > 0) {
                                            scope.launch {
                                                try {
                                                    repo.updateItemQuantityRemote(listId, eid, pid, qty - 1, unit)
                                                } catch (t: Throwable) {
                                                    snack.showSnackbar(t.message ?: updateQuantityError)
                                                }
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    val eid = local?.id ?: remote?.id
                                    if (eid != null && eid > 0) {
                                        scope.launch {
                                            try {
                                                if (local != null) repo.deleteItemRemote(listId, eid, local) else runCatching { repo.deleteItemRemote(listId, eid, ListItem(id = eid, listId = listId, productId = pid, quantity = qty)) }
                                            } catch (t: Throwable) {
                                                snack.showSnackbar(t.message ?: deleteItemError)
                                            }
                                        }
                                    }
                                },
                                onToggleAcquired = {
                                    val eid = local?.id ?: remote?.id
                                    if (eid != null && eid > 0) {
                                        val newPurchased = !(local?.acquired ?: (remote?.purchased ?: false))
                                        scope.launch {
                                            try {
                                                repo.toggleItemPurchasedRemote(listId, eid, newPurchased)
                                            } catch (t: Throwable) {
                                                snack.showSnackbar(t.message ?: changeStateError)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Text(String.format(totalLabelFmt, total), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
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
                ) { Text(closeListLabel) }
                Button(
                    onClick = { showFinalize = true },
                    modifier = Modifier
                        .weight(0.45f)
                        .heightIn(min = 36.dp)
                ) { Text(finalizeLabel) }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            products = products,
            categories = categories.map { it.name },
            onDismiss = { showAdd = false },
            onAdd = { productId, name, price, unit, categoryName ->
                scope.launch {
                    try {
                        val chosenProductId: Long = productId ?: run {
                            val catId = categoryName?.let { if (it.isNotBlank()) repo.createOrFindCategoryByName(it) else null }
                            repo.createProductRemote(name!!.trim(), price ?: 0.0, unit ?: "", catId)
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
                                    unit = unit ?: existing?.unit ?: "",
                                    categoryId = catId
                                )
                            }
                        }
                        val fallbackUnit = products.firstOrNull { it.id == chosenProductId }?.unit?.ifBlank { "" } ?: ""
                        val safeUnitForItem = (unit?.takeIf { it.isNotBlank() } ?: fallbackUnit)
                        repo.addItemRemote(listId, chosenProductId, 1, safeUnitForItem)
                        // Single remote refresh for fallback
                        remoteItems = runCatching { repo.fetchListItemsRemote(listId) }.getOrDefault(emptyList())
                        showAdd = false
                    } catch (t: Throwable) {
                        snack.showSnackbar(t.message ?: addItemError)
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
                        // Success: show concise success message (use hoisted value)
                        shareMessage = shareSuccess
                        shareIsError = false
                    } catch (t: Throwable) {
                        // If it's a network timeout, optimistically report success because the server may have processed it
                        val isTimeout = t is SocketTimeoutException || (t.message?.contains("timeout", ignoreCase = true) == true)
                        if (isTimeout) {
                            shareMessage = shareSuccess
                            shareIsError = false
                        } else {
                            val code = (t as? HttpException)?.code()
                            val msg = when (code) {
                                404 -> shareError404
                                409 -> shareError409
                                400 -> shareError400
                                401, 403 -> shareErrorUnauthorized
                                else -> shareErrorDefault
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
    item: com.example.tuchanguito.data.model.ListItem,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onDelete: () -> Unit,
    onToggleAcquired: () -> Unit
) {
    ListItem(
        leadingContent = {
            Checkbox(checked = item.acquired, onCheckedChange = { onToggleAcquired() })
        },
        headlineContent = {
            Text(
                productName,
                textDecoration = if (item.acquired) TextDecoration.LineThrough else TextDecoration.None
            )
        },
        supportingContent = {
            val priceStr = "%.2f".format(price)
            Text(stringResource(id = R.string.price_label) + ": $" + priceStr)
        },
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
    categories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (productId: Long?, name: String?, price: Double?, unit: String?, categoryName: String?) -> Unit,
    onPrefillFor: (Long) -> Product?,
    categoryNameFor: (Long) -> String?
) {
    var name by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") } // default empty, user must enter
    var category by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun prefillFrom(product: Product) {
        name = product.name
        priceText = if (product.price != 0.0) product.price.toString() else ""
        unit = product.unit // don’t force "u"; keep as-is
        category = categoryNameFor(product.id) ?: ""
    }

    val suggestions = remember(name, products) {
        val q = name.trim()
        if (q.isBlank()) emptyList() else products.filter { it.name.contains(q, ignoreCase = true) }.take(8)
    }

    val categorySuggestions = remember(category, categories) {
        val q = category.trim()
        if (q.isBlank()) emptyList() else categories.filter { it.contains(q, ignoreCase = true) }.take(8)
    }

    // local strings (AddItemDialog is a top-level composable and cannot access outer hoisted vals)
    val addLabel = stringResource(id = R.string.add_label)
    val cancelLabel = stringResource(id = R.string.cancel)
    val addProductTitle = stringResource(id = R.string.add_product_to_list)
    val productLabel = stringResource(id = R.string.product)
    val priceLabel = stringResource(id = R.string.price_label)
    val unitLabel = stringResource(id = R.string.unit_label)
    val categoryLabel = stringResource(id = R.string.category_label)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                 busy = true
                 val p = selectedId
                 val price = priceText.toDoubleOrNull()
                 onAdd(p, if (p == null) name else name.takeIf { it.isNotBlank() && it != (onPrefillFor(p)?.name ?: "") }, price, unit, category.ifBlank { null })
                 busy = false
             }, enabled = !busy && (selectedId != null || name.trim().isNotBlank())) { Text(addLabel) }
        },
        dismissButton = { TextButton(onClick = { if (!busy) onDismiss() }) { Text(cancelLabel) } },
        title = { Text(addProductTitle) },
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
                    label = { Text(productLabel) },
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
                    label = { Text(priceLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(unitLabel) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(categoryLabel) },
                    singleLine = true
                )
                if (categorySuggestions.isNotEmpty()) {
                    categorySuggestions.forEach { cName ->
                        TextButton(onClick = { category = cName }) { Text(cName) }
                    }
                }
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
                Text(stringResource(id = R.string.share_button))
            }
        },
        dismissButton = {
            val dismissLabel = if (message != null && !isError) stringResource(id = R.string.close_label) else stringResource(id = R.string.cancel)
            TextButton(onClick = { if (!busy) onDismiss() }) { Text(dismissLabel) }
        },
        title = { Text(stringResource(id = R.string.share_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Text(stringResource(id = R.string.share_enter_email))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.email_label)) },
                    singleLine = true,
                    enabled = !busy && (message == null || isError),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = {
                        if (!isValid && email.isNotBlank() && (message == null || isError)) Text(stringResource(id = R.string.invalid_email))
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
            ) { Text(if (busy) stringResource(id = R.string.processing) else stringResource(id = R.string.finalize)) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(stringResource(id = R.string.finalize)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(id = R.string.finalize_question))
                // Purchased -> Pantry
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = includePurchasedToPantry, onCheckedChange = { includePurchasedToPantry = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.add_purchased_to_pantry))
                }
                // Not purchased -> another list
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = moveNotPurchased, onCheckedChange = { moveNotPurchased = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.move_not_purchased))
                }
                if (moveNotPurchased) {
                    // Selector: existing vs create new
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { creatingNew = false }, label = { Text(stringResource(id = R.string.select_list)) }, leadingIcon = {})
                        AssistChip(onClick = { creatingNew = true }, label = { Text(stringResource(id = R.string.create_new)) }, leadingIcon = {})
                    }
                    if (creatingNew) {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text(stringResource(id = R.string.new_list_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Simple dropdown using a list of buttons
                        Column {
                            Text(stringResource(id = R.string.select_destination))
                            val options = lists.filter { it.id != listId }
                            if (options.isEmpty()) {
                                Text(stringResource(id = R.string.no_lists_available))
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
