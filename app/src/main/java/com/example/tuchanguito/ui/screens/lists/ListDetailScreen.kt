package com.example.tuchanguito.ui.screens.lists

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.data.model.Category
import com.example.tuchanguito.data.model.Product
import com.example.tuchanguito.data.network.model.ListItemDto
import com.example.tuchanguito.data.network.model.ProductDto
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.ui.screens.lists.ListDetailEvent.ItemAdded
import com.example.tuchanguito.ui.screens.lists.ListDetailEvent.ListFinalized
import com.example.tuchanguito.ui.screens.lists.ListDetailEvent.ShowSnackbar
import com.example.tuchanguito.ui.screens.lists.ListDetailViewModel
import com.example.tuchanguito.ui.screens.lists.ListDetailViewModelFactory
import com.example.tuchanguito.ui.screens.lists.ListFinalizeOptions
import com.example.tuchanguito.ui.screens.lists.ShareUiState
import com.example.tuchanguito.ui.theme.ColorAccent
import com.example.tuchanguito.ui.theme.ColorPrimary
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(listId: Long, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val viewModel: ListDetailViewModel = viewModel(
        factory = ListDetailViewModelFactory(
            listId = listId,
            shoppingListsRepository = app.shoppingListsRepository,
            productRepository = app.productRepository,
            categoryRepository = app.categoryRepository,
            pantryRepository = app.pantryRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val shareState by viewModel.shareState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Determinar si la lista pertenece al usuario actual (owner != null)
    val isOwner = uiState.list?.owner != null

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var addDialogBusy by remember { mutableStateOf(false) }
    var itemToDeleteId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ShowSnackbar -> {
                    if (showAddProductDialog) addDialogBusy = false
                    snackbarHost.showSnackbar(event.message)
                }
                ItemAdded -> {
                    addDialogBusy = false
                    showAddProductDialog = false
                }
                ListFinalized -> {
                    showFinalizeDialog = false
                    onClose()
                }
            }
        }
    }

    val groupedItems = remember(uiState.items) {
        uiState.items.groupBy { it.product.category?.name ?: "Sin categoria" }
    }
    val sortedCategories = remember(groupedItems) { groupedItems.keys.sortedBy { it.lowercase() } }
    val totalCost by remember(uiState.items) {
        derivedStateOf { uiState.items.sumOf { it.quantity * it.product.priceFromMetadata() } }
    }
    val categoryLookup = remember(uiState.products, uiState.categories) {
        uiState.products.associate { product ->
            product.id to uiState.categories.firstOrNull { it.id == product.categoryId }?.name
        }
    }

    val purchasedCount by remember(uiState.items) { derivedStateOf { uiState.items.count { it.purchased } } }
    val totalCount by remember(uiState.items) { derivedStateOf { uiState.items.size } }
    val progress by remember(purchasedCount, totalCount) {
        derivedStateOf { if (totalCount == 0) 0f else purchasedCount.toFloat() / totalCount.toFloat() }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "purchaseProgress"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        uiState.list?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_label))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (totalCount > 0) {
                // Contenedor con esquinas redondeadas y leve elevación
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Progreso de la compra", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            val percent = (progress * 100).toInt()
                            Text(
                                text = "$purchasedCount de $totalCount comprados ($percent%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (uiState.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .border(androidx.compose.foundation.BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(8.dp))
                                .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.list_empty_message),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            CombinedCategoriesCard(
                                sortedCategories = sortedCategories,
                                groupedItems = groupedItems,
                                onQuantityChange = { item, newQuantity ->
                                    viewModel.updateItemQuantity(
                                        item.id,
                                        item.product.id,
                                        newQuantity,
                                        item.unit ?: item.product.unitFromMetadata()
                                    )
                                },
                                onRequestDelete = { item -> itemToDeleteId = item.id },
                                onToggleAcquired = { item, purchased -> viewModel.toggleItem(item.id, purchased) }
                            )
                        }
                    }
                }
            }

            Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.total_cost), style = MaterialTheme.typography.bodySmall)
                        Text("$%.2f".format(totalCost), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (isOwner) {
                                showFinalizeDialog = true
                            } else {
                                // Mostrar mensaje claro cuando intenta finalizar una lista compartida
                                coroutineScope.launch {
                                    snackbarHost.showSnackbar(
                                        context.getString(R.string.error_shared_list_finalize_not_allowed)
                                    )
                                }
                            }
                        },
                        enabled = !uiState.isProcessing
                    ) {
                        Text(stringResource(id = R.string.finalize))
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { showAddProductDialog = true },
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
            }
        }
    }

    if (showAddProductDialog) {
        AddItemDialog(
            products = uiState.products,
            categories = uiState.categories,
            isSubmitting = addDialogBusy,
            onDismiss = { if (!addDialogBusy) showAddProductDialog = false },
            onAdd = { productId, name, price, unit, categoryName ->
                addDialogBusy = true
                viewModel.addItem(productId, name, price, unit, categoryName)
            },
            categoryNameFor = { productId -> categoryLookup[productId] }
        )
    }

    if (showShareDialog) {
        ShareListDialog(
            state = shareState,
            onDismiss = {
                showShareDialog = false
                viewModel.resetShareState()
            },
            onShare = { email -> viewModel.shareList(email) }
        )
    }

    if (showFinalizeDialog) {
        FinalizeDialog(
            listId = listId,
            isProcessing = uiState.isProcessing,
            availableLists = uiState.availableLists,
            onDismiss = { if (!uiState.isProcessing) showFinalizeDialog = false },
            onRequestLists = { viewModel.loadAvailableLists() },
            onFinalize = { options -> viewModel.finalizeList(options) }
        )
    }

    if (itemToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { itemToDeleteId = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_item_message)) },
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
private fun AddItemDialog(
    products: List<Product>,
    categories: List<Category>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onAdd: (productId: Long?, name: String, price: Double?, unit: String?, categoryName: String) -> Unit,
    categoryNameFor: (Long) -> String?
) {
    var name by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }

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
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onAdd(selectedId, name, priceText.toDoubleOrNull(), unit, categoryText) },
                enabled = !isSubmitting && name.trim().isNotBlank() && categoryText.trim().isNotBlank()
            ) { Text(stringResource(id = R.string.add)) }
        },
        dismissButton = { TextButton(onClick = { if (!isSubmitting) onDismiss() }) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(stringResource(id = R.string.add_product_to_list)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            productExpanded = it.isNotBlank()
                            selectedId = null
                        },
                        label = { Text("Producto") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { productExpanded = !productExpanded }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    )
                    val filteredProducts = products.filter { product ->
                        product.name.contains(name, ignoreCase = true) && name.isNotBlank()
                    }
                    DropdownMenu(
                        expanded = productExpanded && filteredProducts.isNotEmpty(),
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { value -> priceText = value.filter { it.isDigit() || it == '.' } },
                        label = { Text(stringResource(id = R.string.price_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(id = R.string.unit_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    )
                }

                Box {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = {
                            categoryText = it
                            categoryExpanded = it.isNotBlank()
                        },
                        label = { Text(stringResource(id = R.string.category_label)) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { categoryExpanded = !categoryExpanded }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    )
                    val filteredCategories = categories.filter { option ->
                        option.name.contains(categoryText, ignoreCase = true) && categoryText.isNotBlank()
                    }
                    DropdownMenu(
                        expanded = categoryExpanded && filteredCategories.isNotEmpty(),
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        filteredCategories.forEach { category ->
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareListDialog(
    state: ShareUiState,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isValid = remember(email) { email.contains("@") && email.length > 5 }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = { Text(stringResource(id = R.string.share_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(id = R.string.share_enter_email))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.email_label)) },
                    singleLine = true,
                    enabled = !state.isBusy,
                    isError = !isValid && email.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                state.message?.let {
                    Text(
                        it,
                        color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { focusManager.clearFocus(); onShare(email.trim()) }, enabled = !state.isBusy && isValid) {
                Text(stringResource(id = R.string.share_button))
            }
        },
        dismissButton = { TextButton(onClick = { if (!state.isBusy) onDismiss() }) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalizeDialog(
    listId: Long,
    isProcessing: Boolean,
    availableLists: List<ShoppingListDto>,
    onDismiss: () -> Unit,
    onRequestLists: () -> Unit,
    onFinalize: (ListFinalizeOptions) -> Unit
) {
    var includePurchasedToPantry by remember { mutableStateOf(true) }
    var moveNotPurchased by remember { mutableStateOf(true) }
    var creatingNew by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var selectedListId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { onRequestLists() }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        confirmButton = {
            TextButton(
                enabled = !isProcessing,
                onClick = {
                    onFinalize(
                        ListFinalizeOptions(
                            includePurchasedToPantry = includePurchasedToPantry,
                            moveNotPurchased = moveNotPurchased,
                            targetListId = selectedListId,
                            newListName = newListName.takeIf { creatingNew }
                        )
                    )
                }
            ) {
                Text(if (isProcessing) stringResource(id = R.string.processing) else stringResource(id = R.string.finalize))
            }
        },
        dismissButton = { TextButton(enabled = !isProcessing, onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
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
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        )
                    } else {
                        Column {
                            Text(stringResource(id = R.string.select_destination))
                            val options = availableLists.filter { it.id != listId }
                            if (options.isEmpty()) {
                                Text(stringResource(id = R.string.no_lists_available))
                            } else {
                                options.forEach { list ->
                                    val selected = selectedListId == list.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedListId = list.id },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selected, onClick = { selectedListId = list.id })
                                        Text(list.name)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombinedCategoriesCard(
    sortedCategories: List<String>,
    groupedItems: Map<String, List<ListItemDto>>,
    onQuantityChange: (ListItemDto, Double) -> Unit,
    onRequestDelete: (ListItemDto) -> Unit,
    onToggleAcquired: (ListItemDto, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            sortedCategories.forEachIndexed { cIndex, categoryName ->
                val items = groupedItems[categoryName].orEmpty()
                if (items.isEmpty()) return@forEachIndexed
                // Título de categoría dentro de la tarjeta
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                // Filas de productos
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(checked = item.purchased, onCheckedChange = { onToggleAcquired(item, it) })
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.name,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = "$%.2f".format(item.product.priceFromMetadata()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (item.quantity > 1) onQuantityChange(item, item.quantity - 1) else onRequestDelete(item) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrementar")
                            }
                            val formattedQuantity = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.2f".format(item.quantity)
                            Text(formattedQuantity, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { onQuantityChange(item, item.quantity + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "Incrementar")
                            }
                            IconButton(onClick = { onRequestDelete(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (index < items.lastIndex) {
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                // Separador entre categorías dentro de la misma tarjeta
                if (cIndex < sortedCategories.lastIndex) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun ProductDto.priceFromMetadata(): Double {
    val map = metadata as? Map<*, *> ?: return 0.0
    val priceAny = map["price"] ?: return 0.0
    return (priceAny as? Number)?.toDouble() ?: priceAny.toString().toDoubleOrNull() ?: 0.0
}

private fun ProductDto.unitFromMetadata(): String {
    val map = metadata as? Map<*, *> ?: return "u"
    val unitAny = map["unit"] ?: return "u"
    return unitAny.toString().ifBlank { "u" }
}
