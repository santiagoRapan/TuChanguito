package com.example.tuchanguito.ui.screens.products

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.collectAsState
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
import com.example.tuchanguito.ui.theme.ColorPrimary
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = newProductDesc)
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
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
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = fgColor
                                        )
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
                                    scope.launch {
                                        val id = p.id ?: return@launch
                                        val res = runCatching { catalogRepository.deleteProduct(id) }
                                        if (res.isFailure) {
                                            snack.showSnackbar(res.exceptionOrNull()?.message ?: deleteErrorLabel)
                                        } else {
                                            remoteProducts = remoteProducts.filterNot { it.id == id }
                                        }
                                    }
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

@Composable
private fun ProductCard(
    product: ProductDto,
    editLabel: String,
    deleteLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priceVal = product.metadata.doubleValue("price")
    val priceStr = "%.2f".format(priceVal)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = "${"$"}${priceStr} c/u",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
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

private fun JsonElement?.doubleValue(key: String, default: Double = 0.0): Double =
    this?.jsonObject?.get(key)?.jsonPrimitive?.doubleOrNull ?: default

private fun JsonElement?.stringValue(key: String): String =
    this?.jsonObject?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
