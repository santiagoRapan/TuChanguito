package com.example.tuchanguito.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.data.network.model.CategoryDto
import com.example.tuchanguito.data.network.model.ProductDto
import com.example.tuchanguito.ui.theme.ColorAccent
import com.example.tuchanguito.ui.theme.ColorPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen() {
    val context = LocalContext.current
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

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    var showCreate by rememberSaveable { mutableStateOf(false) }
    var editingProductId by rememberSaveable { mutableStateOf<Long?>(null) }

    var remoteCategories by remember { mutableStateOf(listOf<CategoryDto>()) }
    var remoteProducts by remember { mutableStateOf(listOf<ProductDto>()) }
    // Pending delete state used to show confirmation dialog before deleting a product
    var pendingDeleteProductId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteProductName by remember { mutableStateOf<String?>(null) }

    // Helper to delete a product and clean all references in pantry and lists
    fun removeProductEverywhere(productId: Long, productName: String) {
        scope.launch {
            suspend fun cleanupRefs() {
                // Pantry cleanup (best-effort) BEFORE product deletion using search by product name to avoid orphans crash
                runCatching { app.pantryRepository.getItems(search = productName) }
                    .onSuccess { items ->
                        items.filter { it.product.id == productId }.forEach { pi ->
                            runCatching { app.pantryRepository.deleteItem(pi.id) }
                        }
                    }
                // Shopping lists cleanup (best-effort) BEFORE product deletion
                runCatching { app.shoppingListsRepository.getLists(perPage = 200) }
                    .onSuccess { page ->
                        page.data.forEach { list ->
                            runCatching { app.shoppingListsRepository.getItems(list.id) }
                                .onSuccess { listItems ->
                                    listItems.filter { it.product.id == productId }.forEach { li ->
                                        runCatching { app.shoppingListsRepository.deleteItem(list.id, li.id) }
                                    }
                                }
                        }
                    }
            }

            // Always cleanup references first to keep server consistent
            cleanupRefs()

            // Then attempt to delete the product
            val deleteAttempt = runCatching { catalogRepository.deleteProduct(productId) }
            if (deleteAttempt.isSuccess) {
                remoteProducts = remoteProducts.filterNot { it.id == productId }
            } else {
                // If deletion still fails, make one more cleanup pass and retry once
                cleanupRefs()
                val retry = runCatching { catalogRepository.deleteProduct(productId) }
                if (retry.isSuccess) {
                    remoteProducts = remoteProducts.filterNot { it.id == productId }
                } else {
                    snack.showSnackbar(retry.exceptionOrNull()?.message ?: deleteErrorLabel)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val res = catalogRepository.syncCatalog()
        if (res.isFailure) snack.showSnackbar(res.exceptionOrNull()?.message ?: createErrorLabel)
    }

    LaunchedEffect(query) {
        runCatching { catalogRepository.categoriesForQuery(query) }
            .onSuccess { cats ->
                remoteCategories = cats
                if (selectedCategoryId != null && cats.none { it.id == selectedCategoryId }) selectedCategoryId = null
            }
            .onFailure { snack.showSnackbar(it.message ?: "Error cargando categorías") }
    }

    LaunchedEffect(query, selectedCategoryId) {
        runCatching { catalogRepository.searchProducts(name = query, categoryId = selectedCategoryId) }
            .onSuccess { list -> remoteProducts = list }
            .onFailure { snack.showSnackbar(it.message ?: "Error cargando productos") }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(productsTitle, color = Color.White) },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            Button(
                onClick = { showCreate = true },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = newProductDesc)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(searchLabel) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text(allLabel) },
                        colors = filterChipColors
                    )
                }
                items(remoteCategories.size) { index ->
                    val cat = remoteCategories[index]
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) },
                        colors = filterChipColors
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (remoteProducts.isEmpty()) {
                    item { Text(noProductsLabel, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(remoteProducts.size, key = { idx -> remoteProducts[idx].id }) { i ->
                        val p = remoteProducts[i]
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { target ->
                                if (target == SwipeToDismissBoxValue.EndToStart) {
                                    // Show confirmation dialog instead of deleting immediately
                                    pendingDeleteProductId = p.id
                                    pendingDeleteProductName = p.name
                                    // Do not auto-dismiss the item; wait for user confirmation
                                    false
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
                                                Icons.Filled.Delete,
                                                contentDescription = null,
                                                tint = fgColor
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            ProductCard(
                                product = p,
                                editLabel = editLabel,
                                deleteLabel = deleteLabel,
                                onEdit = { editingProductId = p.id },
                                onDelete = {
                                    // Don't delete immediately: ask for confirmation like in lists
                                    pendingDeleteProductId = p.id
                                    pendingDeleteProductName = p.name
                                }
                            )
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
                            remoteProducts = catalogRepository.searchProducts(query, selectedCategoryId)
                            remoteCategories = catalogRepository.categoriesForQuery(query)
                        } catch (t: Throwable) {
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
                            remoteProducts = catalogRepository.searchProducts(query, selectedCategoryId)
                            remoteCategories = catalogRepository.categoriesForQuery(query)
                        } catch (t: Throwable) {
                            snack.showSnackbar(t.message ?: createErrorLabel)
                        } finally { busy = false }
                    }
                }, enabled = valid && !busy) { Text(stringResource(id = R.string.save_changes)) }
            },
            dismissButton = { TextButton(onClick = { if (!busy) editingProductId = null }) { Text(stringResource(id = R.string.cancel)) } },
            title = { Text(stringResource(id = R.string.edit_product)) },
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

    // Confirmation dialog for product deletion (used by both swipe and delete button)
    if (pendingDeleteProductId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteProductId = null; pendingDeleteProductName = null },
            title = { Text(stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(stringResource(id = R.string.confirm_delete_product_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = pendingDeleteProductId ?: return@TextButton
                    val name = pendingDeleteProductName ?: ""
                    pendingDeleteProductId = null
                    pendingDeleteProductName = null
                    removeProductEverywhere(id, name)
                }) { Text(stringResource(id = R.string.delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProductId = null; pendingDeleteProductName = null }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductDto,
    editLabel: String,
    deleteLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Card with white background (barras blancas) and no price line under the name
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
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
                    text = product.name,
                    fontWeight = FontWeight.Bold
                )
                // Price line intentionally removed per request
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = editLabel,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = deleteLabel,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Metadata helpers use Any? -> Map<*,*> to avoid serialization dependency
private fun Any?.doubleValue(key: String, default: Double = 0.0): Double {
    val map = this as? Map<*, *> ?: return default
    val v = map[key] ?: return default
    return (v as? Number)?.toDouble() ?: v.toString().toDoubleOrNull() ?: default
}

private fun Any?.stringValue(key: String): String {
    val map = this as? Map<*, *> ?: return ""
    val v = map[key] ?: return ""
    return v.toString().ifBlank { "" }
}
