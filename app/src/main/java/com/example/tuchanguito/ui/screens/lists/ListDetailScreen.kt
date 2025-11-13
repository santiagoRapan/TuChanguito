package com.example.tuchanguito.ui.screens.lists

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.network.dto.ListItemDTO
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(listId: Long, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    val list by repo.listById(listId).collectAsState(initial = null)
    val items by repo.itemsForList(listId).collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())
    val categories by repo.categories().collectAsState(initial = emptyList())

    // State for dialogs
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // Data maps for efficient lookup
    val productMap = remember(products) { products.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val itemsGroupedByCategory by remember(items, productMap, categoryMap) {
        derivedStateOf {
            items.mapNotNull { item -> productMap[item.productId]?.let { product -> Pair(item, product) } }
                .groupBy { (_, product) -> categoryMap[product.categoryId] ?: Category(id = -1, name = "Sin Categoría") }
                .toSortedMap(compareBy { it.name })
        }
    }

    // Initial data sync
    LaunchedEffect(listId) {
        busy = true
        repo.loadListIntoLocal(listId)
        repo.syncCatalog()
        repo.syncListItems(listId)
        busy = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(list?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back)) } },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_label))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (busy && items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), // SCROLL FIX
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsGroupedByCategory.forEach { (category, itemsWithProducts) ->
                        item(key = category.id) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                            )
                        }
                        items(itemsWithProducts, key = { it.first.id }) { (item, product) ->
                            ListItemCard(
                                item = item,
                                product = product,
                                onQuantityChange = { newQuantity ->
                                    scope.launch {
                                        try {
                                            repo.updateItemQuantityRemote(listId, item.id, product.id, newQuantity, product.unit)
                                        } catch (t: Throwable) { snackbarHost.showSnackbar("Error actualizando cantidad") }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            repo.deleteItemRemote(listId, item.id)
                                        } catch (t: Throwable) { snackbarHost.showSnackbar("Error eliminando producto") }
                                    }
                                },
                                onToggleAcquired = { purchased ->
                                    scope.launch {
                                        try {
                                            repo.toggleItemPurchasedRemote(listId, item.id, purchased)
                                        } catch (t: Throwable) { snackbarHost.showSnackbar("Error actualizando estado") }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            val totalCost = items.sumOf { (productMap[it.productId]?.price ?: 0.0) * it.quantity }
            Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Costo total:", style = MaterialTheme.typography.bodySmall)
                        Text("$%.2f".format(totalCost), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Button(onClick = { showFinalizeDialog = true }) {
                        Text(stringResource(id = R.string.finalize))
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { showAddProductDialog = true },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_product_to_list))
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddItemDialog(
            products = products,
            categories = categories,
            onDismiss = { showAddProductDialog = false },
            onAdd = { productId, name, price, unit, categoryName ->
                scope.launch {
                    try {
                        // Crash fix: ensure categoryName is not blank
                        if (categoryName.isNullOrBlank()) {
                            snackbarHost.showSnackbar("La categoría es obligatoria")
                            return@launch
                        }
                        val chosenProductId: Long = productId ?: run {
                            val catId = repo.createOrFindCategoryByName(categoryName)
                            repo.createProductRemote(name!!.trim(), price ?: 0.0, unit ?: "", catId)
                        }
                        repo.addItemRemote(listId, chosenProductId, 1, unit ?: "u")
                        repo.syncListItems(listId)
                        showAddProductDialog = false
                    } catch (t: Throwable) {
                        snackbarHost.showSnackbar(t.message ?: "Error al añadir producto")
                    }
                }
            },
            categoryNameFor = { pid -> productMap[pid]?.categoryId?.let { categoryMap[it]?.name } }
        )
    }
    
    var shareBusy by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var shareIsError by remember { mutableStateOf(false) }

    if (showShareDialog) {
        ShareListDialog(
            busy = shareBusy,
            message = shareMessage,
            isError = shareIsError,
            onDismiss = { showShareDialog = false; shareMessage = null; shareIsError = false },
            onShare = { email ->
                shareBusy = true
                shareMessage = null
                shareIsError = false
                scope.launch {
                    try {
                        repo.shareListRemote(listId, email)
                        shareMessage = "Lista compartida con éxito"
                        shareIsError = false
                    } catch (t: Throwable) {
                        shareMessage = t.message ?: "Error al compartir"
                        shareIsError = true
                    } finally {
                        shareBusy = false
                    }
                }
            }
        )
    }

    if (showFinalizeDialog) {
        FinalizeDialog(
            listId = listId,
            itemsProvider = { repo.fetchListItemsRemote(listId) },
            repo = repo,
            onDismiss = { showFinalizeDialog = false },
            onDone = { showFinalizeDialog = false; onClose() }
        )
    }
}

@Composable
fun ListItemCard(
    item: ListItem, product: Product, onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit, onToggleAcquired: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Checkbox(checked = item.acquired, onCheckedChange = onToggleAcquired)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.acquired) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "$%.2f c/u".format(product.price),
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) else onDelete() }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrementar")
                }
                Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = { onQuantityChange(item.quantity + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Incrementar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    products: List<Product>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (productId: Long?, name: String?, price: Double?, unit: String?, categoryName: String?) -> Unit,
    categoryNameFor: (Long) -> String?
) {
    var name by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    var productExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    fun prefillFrom(product: Product) {
        name = product.name
        priceText = if (product.price > 0) product.price.toString() else ""
        unit = product.unit
        categoryText = categoryNameFor(product.id) ?: ""
        selectedId = product.id
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    busy = true
                    onAdd(selectedId, name, priceText.toDoubleOrNull(), unit, categoryText)
                },
                enabled = !busy && name.trim().isNotBlank() && categoryText.trim().isNotBlank()
            ) { Text(stringResource(id = R.string.add)) }
        },
        dismissButton = { TextButton(onClick = { if (!busy) onDismiss() }) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(stringResource(id = R.string.add_product_to_list)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = !productExpanded }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            productExpanded = it.isNotBlank() // UX Fix
                            selectedId = null
                        },
                        label = { Text(stringResource(id = R.string.product)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    val filteredProducts = products.filter { it.name.contains(name, ignoreCase = true) && name.isNotBlank() }
                    if (filteredProducts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            filteredProducts.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(product.name) },
                                    onClick = {
                                        prefillFrom(product)
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(id = R.string.price_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(id = R.string.unit_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { 
                            categoryText = it
                            categoryExpanded = it.isNotBlank() // UX Fix
                        },
                        label = { Text(stringResource(id = R.string.category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    val filteredOptions = categories.filter { it.name.contains(categoryText, ignoreCase = true) && categoryText.isNotBlank() }
                    if (filteredOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            filteredOptions.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categoryText = category.name
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
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
    val isValid = remember(email) { email.contains("@") && email.length > 5 }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(id = R.string.share_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(id = R.string.share_enter_email))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.email_label)) },
                    singleLine = true, enabled = !busy,
                    isError = !isValid && email.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                if (message != null) {
                    Text(message, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { focusManager.clearFocus(); onShare(email.trim()) }, enabled = !busy && isValid) {
                Text(stringResource(id = R.string.share_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onDismiss() }) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

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

                        if (includePurchasedToPantry && purchased.isNotEmpty()) {
                            purchased.forEach { repo.addOrIncrementPantryItem(it.product.id ?: 0, it.quantity.toInt(), it.unit) }
                            repo.syncPantry()
                        }

                        if (moveNotPurchased && notPurchased.isNotEmpty()) {
                            val targetId = if (creatingNew) {
                                newListName.trim().takeIf { it.isNotEmpty() }?.let { repo.createList(it) }
                            } else selectedListId

                            if (targetId != null) {
                                notPurchased.forEach { repo.addItemRemote(targetId, it.product.id ?: 0, it.quantity.toInt(), it.unit) }
                                repo.syncListItems(targetId)
                            }
                        }

                        repo.deleteListRemote(listId)
                        repo.refreshLists()

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includePurchasedToPantry, onCheckedChange = { includePurchasedToPantry = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.add_purchased_to_pantry))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = moveNotPurchased, onCheckedChange = { moveNotPurchased = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.move_not_purchased))
                }
                if (moveNotPurchased) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { creatingNew = false }, label = { Text(stringResource(id = R.string.select_list)) })
                        AssistChip(onClick = { creatingNew = true }, label = { Text(stringResource(id = R.string.create_new)) })
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
                        Column {
                            Text(stringResource(id = R.string.select_destination))
                            val options = lists.filter { it.id != listId }
                            if (options.isEmpty()) {
                                Text(stringResource(id = R.string.no_lists_available))
                            } else {
                                options.forEach { l ->
                                    val selected = selectedListId == l.id
                                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedListId = l.id }, verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selected, onClick = { selectedListId = l.id })
                                        Text(l.title)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
